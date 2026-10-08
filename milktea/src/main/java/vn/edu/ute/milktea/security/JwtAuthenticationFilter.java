package vn.edu.ute.milktea.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.repository.account.AccountRepository;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ACCESS_TOKEN_COOKIE = "milktea_access_token";
    public static final String CURRENT_ACTOR_ATTR = "CURRENT_ACTOR";

    private final JwtService jwtService;
    private final AccountRepository accountRepository;
    private final GuestAccessService guestAccessService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractJwtToken(request);
        CurrentActor actor = null;

        if (token != null) {
            Map<String, Object> claims = jwtService.validateAndExtractClaims(token);
            if (claims != null) {
                Long accountId = Long.parseLong((String) claims.get("sub"));
                Long claimVersion = ((Number) claims.get("tokenVersion")).longValue();

                Optional<Account> accountOpt = accountRepository.findById(accountId);
                if (accountOpt.isPresent()) {
                    Account account = accountOpt.get();
                    if (Boolean.TRUE.equals(account.getActive()) && account.getTokenVersion().equals(claimVersion)) {
                        Role role = account.getRole();
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.name());

                        actor = CurrentActor.builder()
                                .actorType(CurrentActor.ActorType.AUTHENTICATED)
                                .accountId(account.getId())
                                .email(account.getEmail())
                                .role(role)
                                .build();

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                actor, null, Collections.singletonList(authority));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
        }

        // If not authenticated account, check guest headers
        if (actor == null) {
            String tableToken = request.getHeader("X-Table-Session-Token");
            if (tableToken == null || tableToken.isBlank()) {
                tableToken = request.getHeader("X-Table-Token");
            }

            String counterToken = request.getHeader("X-Counter-Order-Token");
            if (counterToken == null || counterToken.isBlank()) {
                counterToken = request.getHeader("X-Counter-Token");
            }

            if (tableToken != null && !tableToken.isBlank()) {
                var accessOpt = guestAccessService.validateTableToken(tableToken);
                if (accessOpt.isPresent()) {
                    var access = accessOpt.get();
                    actor = CurrentActor.builder()
                            .actorType(CurrentActor.ActorType.TABLE_GUEST)
                            .sessionId(access.getSession().getId())
                            .tableId(access.getSession().getTable().getId())
                            .build();

                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_GUEST");
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            actor, null, Collections.singletonList(authority));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } else if (counterToken != null && !counterToken.isBlank()) {
                var accessOpt = guestAccessService.validateCounterToken(counterToken);
                if (accessOpt.isPresent()) {
                    var access = accessOpt.get();
                    actor = CurrentActor.builder()
                            .actorType(CurrentActor.ActorType.COUNTER_GUEST)
                            .orderId(access.getOrder().getId())
                            .build();

                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_GUEST");
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            actor, null, Collections.singletonList(authority));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        if (actor != null) {
            request.setAttribute(CURRENT_ACTOR_ATTR, actor);
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
