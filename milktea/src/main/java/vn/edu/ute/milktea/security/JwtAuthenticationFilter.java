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
    private final vn.edu.ute.milktea.repository.branch.StaffBranchRepository staffBranchRepository;

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
                    if (Boolean.TRUE.equals(account.getActive()) && Boolean.TRUE.equals(account.getEmailVerified()) && account.getTokenVersion().equals(claimVersion)) {
                        Role role = account.getRole();
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.name());

                        java.util.Set<Long> assignedBranchIds = java.util.Set.of();
                        Long primaryBranchId = null;
                        if (staffBranchRepository != null) {
                            var staffBranches = staffBranchRepository.findByAccountIdAndStatus(account.getId(), "HOAT_DONG");
                            if (!staffBranches.isEmpty()) {
                                assignedBranchIds = staffBranches.stream()
                                        .map(sb -> sb.getBranch().getId())
                                        .collect(java.util.stream.Collectors.toSet());
                                primaryBranchId = staffBranches.get(0).getBranch().getId();
                            }
                        }

                        actor = CurrentActor.builder()
                                .actorType(CurrentActor.ActorType.AUTHENTICATED)
                                .accountId(account.getId())
                                .email(account.getEmail())
                                .role(role)
                                .branchId(primaryBranchId)
                                .assignedBranchIds(assignedBranchIds)
                                .build();

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                actor, null, Collections.singletonList(authority));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
        }

        String tableToken = request.getHeader("X-Table-Session-Token");
        if (tableToken == null || tableToken.isBlank()) {
            tableToken = request.getHeader("X-Table-Token");
        }
        if (tableToken == null || tableToken.isBlank()) {
            tableToken = extractCookie(request, "milktea_table_token");
        }

        String counterToken = request.getHeader("X-Counter-Order-Token");
        if (counterToken == null || counterToken.isBlank()) {
            counterToken = request.getHeader("X-Counter-Token");
        }
        if (counterToken == null || counterToken.isBlank()) {
            counterToken = extractCookie(request, "milktea_counter_token");
        }

        if (tableToken != null && !tableToken.isBlank()) {
            var accessOpt = guestAccessService.validateTableToken(tableToken);
            if (accessOpt.isPresent()) {
                var access = accessOpt.get();
                if (actor != null) {
                    actor.setSessionId(access.getSession().getId());
                    actor.setTableId(access.getSession().getTable().getId());
                    if (actor.getBranchId() == null && access.getSession().getTable().getBranch() != null) {
                        actor.setBranchId(access.getSession().getTable().getBranch().getId());
                    }
                } else {
                    Long tableBranchId = (access.getSession().getTable().getBranch() != null)
                            ? access.getSession().getTable().getBranch().getId()
                            : null;
                    actor = CurrentActor.builder()
                            .actorType(CurrentActor.ActorType.TABLE_GUEST)
                            .sessionId(access.getSession().getId())
                            .tableId(access.getSession().getTable().getId())
                            .branchId(tableBranchId)
                            .build();

                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_GUEST");
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            actor, null, Collections.singletonList(authority));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } else if (counterToken != null && !counterToken.isBlank()) {
            var accessOpt = guestAccessService.validateCounterToken(counterToken);
            if (accessOpt.isPresent()) {
                var access = accessOpt.get();
                if (actor != null) {
                    actor.setOrderId(access.getOrder().getId());
                    if (actor.getBranchId() == null && access.getOrder().getBranch() != null) {
                        actor.setBranchId(access.getOrder().getBranch().getId());
                    }
                } else {
                    Long counterBranchId = (access.getOrder().getBranch() != null)
                            ? access.getOrder().getBranch().getId()
                            : null;
                    actor = CurrentActor.builder()
                            .actorType(CurrentActor.ActorType.COUNTER_GUEST)
                            .orderId(access.getOrder().getId())
                            .branchId(counterBranchId)
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

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && name != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue().trim();
                }
            }
        }
        return null;
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
