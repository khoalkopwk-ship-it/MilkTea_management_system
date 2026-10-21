package vn.edu.ute.milktea.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.table.DiningTable;
import vn.edu.ute.milktea.entity.table.TableSession;
import vn.edu.ute.milktea.entity.table.TableSessionAccess;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.security.GuestAccessService;
import vn.edu.ute.milktea.security.JwtService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketSecurityConfigTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private GuestAccessService guestAccessService;

    @Mock
    private MessageChannel messageChannel;

    private WebSocketConfig webSocketConfig;
    private ChannelInterceptor channelInterceptor;

    @BeforeEach
    void setUp() {
        webSocketConfig = new WebSocketConfig(jwtService, accountRepository, guestAccessService);
        ReflectionTestUtils.setField(webSocketConfig, "allowedOrigin", "*");

        ChannelRegistration registration = mock(ChannelRegistration.class);
        ArgumentCaptor<ChannelInterceptor> captor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        doReturn(registration).when(registration).interceptors(captor.capture());

        webSocketConfig.configureClientInboundChannel(registration);
        channelInterceptor = captor.getValue();
    }

    @Test
    @DisplayName("1. Chặn toàn bộ lệnh STOMP SEND từ client")
    void testBlockStompSend() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/orders");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = channelInterceptor.preSend(message, messageChannel);
        assertNull(result, "STOMP SEND từ client phải bị chặn (trả về null)");
    }

    @Test
    @DisplayName("2. Từ chối SUBSCRIBE vào destination lạ hoặc không được phép")
    void testRejectUnknownDestination() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/unknown-secret");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(IllegalArgumentException.class, () -> channelInterceptor.preSend(message, messageChannel));
    }

    @Test
    @DisplayName("3. Từ chối SUBSCRIBE /topic/cashier nếu không phải CASHIER hoặc ADMIN")
    void testRejectCashierChannelForKitchen() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/cashier");
        CurrentActor kitchenActor = CurrentActor.builder().actorType(CurrentActor.ActorType.AUTHENTICATED).role(Role.KITCHEN).build();
        accessor.setUser(new UsernamePasswordAuthenticationToken(kitchenActor, null, List.of(new SimpleGrantedAuthority("ROLE_KITCHEN"))));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(IllegalArgumentException.class, () -> channelInterceptor.preSend(message, messageChannel));
    }

    @Test
    @DisplayName("4. Cho phép SUBSCRIBE /topic/cashier nếu là CASHIER")
    void testAllowCashierChannelForCashier() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/cashier");
        CurrentActor cashierActor = CurrentActor.builder().actorType(CurrentActor.ActorType.AUTHENTICATED).role(Role.CASHIER).build();
        accessor.setUser(new UsernamePasswordAuthenticationToken(cashierActor, null, List.of(new SimpleGrantedAuthority("ROLE_CASHIER"))));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> res = channelInterceptor.preSend(message, messageChannel);
        assertNotNull(res);
    }

    @Test
    @DisplayName("5. Phiên bàn: Khách bàn 10 không được xem phiên bàn 20")
    void testRejectCrossSessionSubscription() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/table-sessions/20");
        CurrentActor guestSession10 = CurrentActor.builder().actorType(CurrentActor.ActorType.TABLE_GUEST).sessionId(10L).build();
        accessor.setUser(new UsernamePasswordAuthenticationToken(guestSession10, null, List.of(new SimpleGrantedAuthority("ROLE_GUEST"))));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(IllegalArgumentException.class, () -> channelInterceptor.preSend(message, messageChannel));
    }

    @Test
    @DisplayName("6. Phiên bàn: Khách bàn 10 xem đúng phiên bàn 10 được cho phép")
    void testAllowMatchingSessionSubscription() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/table-sessions/10");
        CurrentActor guestSession10 = CurrentActor.builder().actorType(CurrentActor.ActorType.TABLE_GUEST).sessionId(10L).build();
        accessor.setUser(new UsernamePasswordAuthenticationToken(guestSession10, null, List.of(new SimpleGrantedAuthority("ROLE_GUEST"))));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> res = channelInterceptor.preSend(message, messageChannel);
        assertNotNull(res);
    }

    @Test
    @DisplayName("7. STOMP CONNECT xác thực thành công qua JWT Bearer")
    void testConnectWithJwtBearer() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("Authorization", "Bearer valid.jwt.token");

        when(jwtService.validateAndExtractClaims("valid.jwt.token")).thenReturn(Map.of("sub", "1", "tokenVersion", 0L));
        Account account = Account.builder().id(1L).email("admin@milktea.vn").role(Role.ADMIN).active(true).tokenVersion(0L).build();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        channelInterceptor.preSend(message, messageChannel);

        assertNotNull(accessor.getUser());
        org.springframework.security.core.Authentication auth = (org.springframework.security.core.Authentication) accessor.getUser();
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("8. STOMP CONNECT xác thực thành công qua X-Table-Session-Token")
    void testConnectWithTableSessionToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("X-Table-Session-Token", "raw-opaque-token");

        TableSession session = TableSession.builder().id(15L).table(DiningTable.builder().id(3L).name("Bàn 3").build()).build();
        TableSessionAccess access = TableSessionAccess.builder().session(session).build();
        when(guestAccessService.validateTableToken("raw-opaque-token")).thenReturn(Optional.of(access));

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        channelInterceptor.preSend(message, messageChannel);

        assertNotNull(accessor.getUser());
        org.springframework.security.core.Authentication auth = (org.springframework.security.core.Authentication) accessor.getUser();
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GUEST")));
    }
}
