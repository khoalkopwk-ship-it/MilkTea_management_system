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
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final AccountRepository accountRepository;
    private final GuestAccessService guestAccessService;
    private final vn.edu.ute.milktea.repository.branch.BranchRepository branchRepository;
    private final vn.edu.ute.milktea.repository.branch.StaffBranchRepository staffBranchRepository;
    private final vn.edu.ute.milktea.repository.table.TableSessionRepository tableSessionRepository;

    public WebSocketConfig(JwtService jwtService,
                           AccountRepository accountRepository,
                           GuestAccessService guestAccessService) {
        this(jwtService, accountRepository, guestAccessService, null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public WebSocketConfig(JwtService jwtService,
                           AccountRepository accountRepository,
                           GuestAccessService guestAccessService,
                           vn.edu.ute.milktea.repository.branch.BranchRepository branchRepository,
                           vn.edu.ute.milktea.repository.branch.StaffBranchRepository staffBranchRepository,
                           vn.edu.ute.milktea.repository.table.TableSessionRepository tableSessionRepository) {
        this.jwtService = jwtService;
        this.accountRepository = accountRepository;
        this.guestAccessService = guestAccessService;
        this.branchRepository = branchRepository;
        this.staffBranchRepository = staffBranchRepository;
        this.tableSessionRepository = tableSessionRepository;
    }

    @org.springframework.beans.factory.annotation.Value("${app.websocket.allowed-origin:*}")
    private String allowedOrigin;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        org.springframework.web.socket.server.HandshakeInterceptor cookieInterceptor = new org.springframework.web.socket.server.HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request,
                                          org.springframework.http.server.ServerHttpResponse response,
                                          org.springframework.web.socket.WebSocketHandler wsHandler,
                                          Map<String, Object> attributes) {
                if (request instanceof org.springframework.http.server.ServletServerHttpRequest servletRequest) {
                    jakarta.servlet.http.HttpServletRequest httpRequest = servletRequest.getServletRequest();
                    jakarta.servlet.http.Cookie[] cookies = httpRequest.getCookies();
                    if (cookies != null) {
                        for (jakarta.servlet.http.Cookie c : cookies) {
                            if (vn.edu.ute.milktea.security.JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE.equals(c.getName())) {
                                attributes.put(vn.edu.ute.milktea.security.JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, c.getValue());
                            }
                        }
                    }
                }
                return true;
            }

            @Override
            public void afterHandshake(org.springframework.http.server.ServerHttpRequest request,
                                       org.springframework.http.server.ServerHttpResponse response,
                                       org.springframework.web.socket.WebSocketHandler wsHandler,
                                       Exception exception) {
            }
        };

        String[] originPatterns;
        if (allowedOrigin == null || allowedOrigin.isBlank() || "*".equals(allowedOrigin.trim())) {
            originPatterns = new String[]{"*"};
        } else {
            originPatterns = java.util.Arrays.stream(allowedOrigin.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
            if (originPatterns.length == 0) {
                originPatterns = new String[]{"*"};
            }
        }

        registry.addEndpoint("/ws")
                .addInterceptors(cookieInterceptor)
                .setAllowedOriginPatterns(originPatterns);
        registry.addEndpoint("/ws")
                .addInterceptors(cookieInterceptor)
                .setAllowedOriginPatterns(originPatterns)
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
        String token = null;
        String bearer = accessor.getFirstNativeHeader("Authorization");
        if (bearer == null) {
            bearer = accessor.getFirstNativeHeader("authorization");
        }
        if (bearer != null && bearer.startsWith("Bearer ")) {
            token = bearer.substring(7).trim();
        }

        // 2. Nếu không có header, lấy từ session attributes (được trích xuất từ cookie ở HTTP handshake)
        if (token == null && accessor.getSessionAttributes() != null) {
            Object cookieToken = accessor.getSessionAttributes().get(vn.edu.ute.milktea.security.JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE);
            if (cookieToken instanceof String ct && !ct.isBlank()) {
                token = ct.trim();
            }
        }

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
                        Long primaryBranchId = null;
                        java.util.Set<Long> assignedBranchIds = new java.util.HashSet<>();
                        if (role == Role.ADMIN) {
                            if (branchRepository != null) {
                                branchRepository.findByActiveTrueOrderByIdAsc().forEach(b -> assignedBranchIds.add(b.getId()));
                            }
                        } else {
                            if (staffBranchRepository != null) {
                                // Match active staff assignments used by JwtAuthenticationFilter.
                                var staffBranches = staffBranchRepository.findByAccountIdAndStatus(account.getId(), "HOAT_DONG");
                                for (var sb : staffBranches) {
                                    if (sb.getBranch() != null) {
                                        assignedBranchIds.add(sb.getBranch().getId());
                                        if (primaryBranchId == null) {
                                            primaryBranchId = sb.getBranch().getId();
                                        }
                                    }
                                }
                            }
                        }

                        // Đọc thêm table token hoặc counter token nếu khách hàng đã đăng nhập nhưng đang ngồi bàn
                        Long sessionId = null;
                        Long tableId = null;
                        String tableToken = accessor.getFirstNativeHeader("X-Table-Session-Token");
                        if (tableToken == null) {
                            tableToken = accessor.getFirstNativeHeader("X-Table-Token");
                        }
                        if (tableToken != null && !tableToken.isBlank()) {
                            var accessOpt = guestAccessService.validateTableToken(tableToken);
                            if (accessOpt.isPresent()) {
                                sessionId = accessOpt.get().getSession().getId();
                                tableId = accessOpt.get().getSession().getTable().getId();
                                if (primaryBranchId == null && accessOpt.get().getSession().getTable().getBranch() != null) {
                                    primaryBranchId = accessOpt.get().getSession().getTable().getBranch().getId();
                                }
                            }
                        }

                        CurrentActor actor = CurrentActor.builder()
                                .actorType(CurrentActor.ActorType.AUTHENTICATED)
                                .accountId(account.getId())
                                .email(account.getEmail())
                                .role(role)
                                .sessionId(sessionId)
                                .tableId(tableId)
                                .branchId(primaryBranchId)
                                .assignedBranchIds(assignedBranchIds)
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
                        .branchId(access.getSession().getTable().getBranch() != null ? access.getSession().getTable().getBranch().getId() : null)
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
                        .branchId(access.getOrder().getBranch() != null ? access.getOrder().getBranch().getId() : null)
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

        // Kiểm tra kênh branch scoped: /topic/branches/{branchId}/{type}
        if (destination.startsWith("/topic/branches/")) {
            String[] parts = destination.split("/");
            if (parts.length >= 5) {
                try {
                    Long branchId = Long.parseLong(parts[3]);
                    String type = parts[4];
                    if ("cashier".equals(type)) {
                        boolean allowed = actor != null && (actor.getRole() == Role.CASHIER || actor.getRole() == Role.ADMIN || actor.getRole() == Role.MANAGER)
                                && actor.canAccessBranch(branchId);
                        if (!allowed) {
                            log.warn("Từ chối SUBSCRIBE {} từ actor={}", destination, actor != null ? actor.getEmail() : "anonymous");
                            throw new IllegalArgumentException("Không có quyền truy cập kênh Thu Ngân chi nhánh " + branchId);
                        }
                        return;
                    } else if ("kitchen".equals(type)) {
                        boolean allowed = actor != null && (actor.getRole() == Role.KITCHEN || actor.getRole() == Role.ADMIN || actor.getRole() == Role.MANAGER)
                                && actor.canAccessBranch(branchId);
                        if (!allowed) {
                            log.warn("Từ chối SUBSCRIBE {} từ actor={}", destination, actor != null ? actor.getEmail() : "anonymous");
                            throw new IllegalArgumentException("Không có quyền truy cập kênh Bếp chi nhánh " + branchId);
                        }
                        return;
                    } else if ("stock".equals(type)) {
                        boolean allowed = actor != null && (actor.getRole() == Role.KITCHEN || actor.getRole() == Role.ADMIN || actor.getRole() == Role.MANAGER)
                                && actor.canAccessBranch(branchId);
                        if (!allowed) {
                            log.warn("Từ chối SUBSCRIBE {} từ actor={}", destination, actor != null ? actor.getEmail() : "anonymous");
                            throw new IllegalArgumentException("Không có quyền truy cập kênh Kho chi nhánh " + branchId);
                        }
                        return;
                    }
                } catch (NumberFormatException ignored) {
                    throw new IllegalArgumentException("Định dạng kênh chi nhánh không hợp lệ");
                }
            }
        }

        // Kiểm tra quyền theo bảng 2 trong realtime_rule.md & F09
        if (destination.startsWith("/topic/cashier")) {
            if (actor == null || actor.getRole() != Role.ADMIN) {
                log.warn("Từ chối SUBSCRIBE /topic/cashier từ principal không phải ADMIN");
                throw new IllegalArgumentException("Chỉ ADMIN mới có quyền truy cập kênh Thu Ngân toàn chuỗi. Vui lòng sử dụng kênh chi nhánh: /topic/branches/{branchId}/cashier");
            }
        } else if (destination.startsWith("/topic/kitchen")) {
            if (actor == null || actor.getRole() != Role.ADMIN) {
                log.warn("Từ chối SUBSCRIBE /topic/kitchen từ principal không phải ADMIN");
                throw new IllegalArgumentException("Chỉ ADMIN mới có quyền truy cập kênh Bếp toàn chuỗi. Vui lòng sử dụng kênh chi nhánh: /topic/branches/{branchId}/kitchen");
            }
        } else if (destination.startsWith("/topic/stock")) {
            if (actor == null || actor.getRole() != Role.ADMIN) {
                log.warn("Từ chối SUBSCRIBE /topic/stock từ principal không phải ADMIN");
                throw new IllegalArgumentException("Chỉ ADMIN mới có quyền truy cập kênh Kho toàn chuỗi. Vui lòng sử dụng kênh chi nhánh: /topic/branches/{branchId}/stock");
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
                boolean allowed = false;
                if (actor != null) {
                    if (actor.getRole() == Role.ADMIN) {
                        allowed = true;
                    } else if (actor.getSessionId() != null && actor.getSessionId().equals(targetSessionId)) {
                        allowed = true;
                    } else if (actor.getRole() == Role.CASHIER || actor.getRole() == Role.MANAGER || actor.getRole() == Role.KITCHEN) {
                        if (tableSessionRepository != null) {
                            var sOpt = tableSessionRepository.findById(targetSessionId);
                            if (sOpt.isPresent() && sOpt.get().getTable() != null && sOpt.get().getTable().getBranch() != null) {
                                allowed = actor.canAccessBranch(sOpt.get().getTable().getBranch().getId());
                            }
                        }
                    } else if (actor.getRole() == Role.CUSTOMER) {
                        if (tableSessionRepository != null) {
                            var sOpt = tableSessionRepository.findById(targetSessionId);
                            if (sOpt.isPresent() && sOpt.get().getOpenedBy() != null && sOpt.get().getOpenedBy().getId().equals(actor.getAccountId())) {
                                allowed = true;
                            }
                        }
                    }
                }
                if (!allowed) {
                    // Fallback kiểm tra header table token
                    String subTableToken = accessor.getFirstNativeHeader("X-Table-Session-Token");
                    if (subTableToken == null) subTableToken = accessor.getFirstNativeHeader("X-Table-Token");
                    if (subTableToken != null && !subTableToken.isBlank()) {
                        var accessOpt = guestAccessService.validateTableToken(subTableToken);
                        if (accessOpt.isPresent() && accessOpt.get().getSession().getId().equals(targetSessionId)) {
                            allowed = true;
                        }
                    }
                }
                if (!allowed) {
                    log.warn("Từ chối SUBSCRIBE {} từ actor={}", destination, actor != null ? actor.getEmail() : "anonymous");
                    throw new IllegalArgumentException("Không có quyền truy cập phiên bàn này");
                }
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException("Mã phiên bàn không hợp lệ");
            }
        } else if (destination.startsWith("/user/queue/") || destination.startsWith("/user/")) {
            if (actor == null) {
                log.warn("Từ chối SUBSCRIBE {} từ principal ẩn danh", destination);
                throw new IllegalArgumentException("Cần đăng nhập hoặc có quyền để truy cập kênh người dùng");
            }
        } else {
            log.warn("Từ chối SUBSCRIBE điểm đến không xác định hoặc không được phép: {}", destination);
            throw new IllegalArgumentException("Kênh không hợp lệ hoặc không được phép");
        }
    }
}
