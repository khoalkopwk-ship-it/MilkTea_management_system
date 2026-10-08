package vn.edu.ute.milktea.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.security.GuestAccessService;
import vn.edu.ute.milktea.security.JwtService;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final AccountRepository accountRepository;
    private final GuestAccessService guestAccessService;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    authenticateStompConnect(accessor);
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    authorizeStompSubscribe(accessor);
                } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                    // Cấm SEND command nghiệp vụ trực tiếp từ client theo realtime_rule.md
                    log.warn("Client cố gắng gửi STOMP SEND trực tiếp tới: {}", accessor.getDestination());
                    return null;
                }

                return message;
            }
        });
    }

    private void authenticateStompConnect(StompHeaderAccessor accessor) {
        // 1. Kiểm tra header Authorization / Cookie JWT
        String bearer = accessor.getFirstNativeHeader("Authorization");
        if (bearer == null) {
            bearer = accessor.getFirstNativeHeader("authorization");
        }
        if (bearer != null && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7).trim();
            Map<String, Object> claims = jwtService.validateAndExtractClaims(token);
            if (claims != null) {
                Long accountId = Long.parseLong((String) claims.get("sub"));
                Long claimVersion = ((Number) claims.get("tokenVersion")).longValue();

                Optional<Account> accountOpt = accountRepository.findById(accountId);
                if (accountOpt.isPresent()) {
                    Account account = accountOpt.get();
                    if (Boolean.TRUE.equals(account.getActive()) && account.getTokenVersion().equals(claimVersion)) {
                        Role role = account.getRole();
                        CurrentActor actor = CurrentActor.builder()
                                .actorType(CurrentActor.ActorType.AUTHENTICATED)
                                .accountId(account.getId())
                                .email(account.getEmail())
                                .role(role)
                                .build();

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                actor, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name())));
                        accessor.setUser(auth);
                        return;
                    }
                }
            }
        }

        // 2. Kiểm tra Table Session Token cho khách bàn
        String tableToken = accessor.getFirstNativeHeader("X-Table-Session-Token");
        if (tableToken == null) {
            tableToken = accessor.getFirstNativeHeader("X-Table-Token");
        }
        if (tableToken != null && !tableToken.isBlank()) {
            var accessOpt = guestAccessService.validateTableToken(tableToken);
            if (accessOpt.isPresent()) {
                var access = accessOpt.get();
                CurrentActor actor = CurrentActor.builder()
                        .actorType(CurrentActor.ActorType.TABLE_GUEST)
                        .sessionId(access.getSession().getId())
                        .tableId(access.getSession().getTable().getId())
                        .build();

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        actor, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST")));
                accessor.setUser(auth);
                return;
            }
        }

        // 3. Kiểm tra Counter Order Token cho khách quầy
        String counterToken = accessor.getFirstNativeHeader("X-Counter-Order-Token");
        if (counterToken == null) {
            counterToken = accessor.getFirstNativeHeader("X-Counter-Token");
        }
        if (counterToken != null && !counterToken.isBlank()) {
            var accessOpt = guestAccessService.validateCounterToken(counterToken);
            if (accessOpt.isPresent()) {
                var access = accessOpt.get();
                CurrentActor actor = CurrentActor.builder()
                        .actorType(CurrentActor.ActorType.COUNTER_GUEST)
                        .orderId(access.getOrder().getId())
                        .build();

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        actor, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST")));
                accessor.setUser(auth);
                return;
            }
        }

        log.debug("STOMP CONNECT từ anonymous guest hoặc public client");
    }

    private void authorizeStompSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Object principalObj = accessor.getUser();
        CurrentActor actor = null;
        if (principalObj instanceof UsernamePasswordAuthenticationToken token) {
            if (token.getPrincipal() instanceof CurrentActor ca) {
                actor = ca;
            }
        }

        // Kiểm tra quyền theo bảng 2 trong realtime_rule.md
        if (destination.startsWith("/topic/cashier")) {
            if (actor == null || (actor.getRole() != Role.CASHIER && actor.getRole() != Role.ADMIN)) {
                log.warn("Từ chối SUBSCRIBE /topic/cashier từ principal không hợp lệ");
                throw new IllegalArgumentException("Không có quyền truy cập kênh Thu Ngân");
            }
        } else if (destination.startsWith("/topic/kitchen")) {
            if (actor == null || (actor.getRole() != Role.KITCHEN && actor.getRole() != Role.ADMIN)) {
                log.warn("Từ chối SUBSCRIBE /topic/kitchen từ principal không hợp lệ");
                throw new IllegalArgumentException("Không có quyền truy cập kênh Bếp");
            }
        } else if (destination.startsWith("/topic/stock")) {
            if (actor == null || (actor.getRole() != Role.KITCHEN && actor.getRole() != Role.ADMIN)) {
                log.warn("Từ chối SUBSCRIBE /topic/stock từ principal không hợp lệ");
                throw new IllegalArgumentException("Không có quyền truy cập kênh Kho");
            }
        } else if (destination.startsWith("/topic/admin")) {
            if (actor == null || actor.getRole() != Role.ADMIN) {
                log.warn("Từ chối SUBSCRIBE /topic/admin từ principal không hợp lệ");
                throw new IllegalArgumentException("Không có quyền truy cập kênh Quản trị");
            }
        } else if (destination.startsWith("/topic/table-sessions/")) {
            String sessionIdStr = destination.substring("/topic/table-sessions/".length());
            try {
                Long targetSessionId = Long.parseLong(sessionIdStr);
                // Được phép nếu là ADMIN, CASHIER, hoặc khách của đúng sessionId này
                boolean allowed = actor != null && (
                        actor.getRole() == Role.ADMIN ||
                        actor.getRole() == Role.CASHIER ||
                        (actor.getSessionId() != null && actor.getSessionId().equals(targetSessionId))
                );
                if (!allowed) {
                    log.warn("Từ chối SUBSCRIBE {} từ actor={}", destination, actor != null ? actor.getEmail() : "anonymous");
                    throw new IllegalArgumentException("Không có quyền truy cập phiên bàn này");
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
