package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.dto.CancellationDto;
import vn.edu.ute.milktea.dto.CartDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.audit.BusinessAudit;
import vn.edu.ute.milktea.entity.cancellation.*;
import vn.edu.ute.milktea.entity.catalog.Category;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.order.*;
import vn.edu.ute.milktea.entity.payment.Payment;
import vn.edu.ute.milktea.entity.payment.PaymentMethod;
import vn.edu.ute.milktea.entity.table.SessionCart;
import vn.edu.ute.milktea.entity.table.TableSession;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.cancellation.CancellationRequestRepository;
import vn.edu.ute.milktea.repository.cancellation.RefundRequestRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.order.InvoiceRepository;
import vn.edu.ute.milktea.repository.order.OrderItemRepository;
import vn.edu.ute.milktea.repository.order.OrderItemToppingRepository;
import vn.edu.ute.milktea.repository.order.OrderRepository;
import vn.edu.ute.milktea.repository.payment.PaymentRepository;
import vn.edu.ute.milktea.repository.table.SessionCartItemRepository;
import vn.edu.ute.milktea.repository.table.SessionCartRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.cancellation.CancellationService;
import vn.edu.ute.milktea.service.cancellation.RefundService;
import vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher;
import vn.edu.ute.milktea.service.table.SessionCartService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NewRulesTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class CategoryToppingValidationTest {
        @Mock SessionCartRepository cartRepository;
        @Mock SessionCartItemRepository cartItemRepository;
        @Mock ProductRepository productRepository;
        @Mock RealtimeEventPublisher publisher;
        @InjectMocks SessionCartService sessionCartService;

        @Test
        void throwsExceptionWhenAddingToppingToCategoryWithAllowToppingFalse() {
            Category catNoTopping = Category.builder().id(1L).name("Cà phê").allowTopping(false).build();
            Product drink = Product.builder().id(10L).name("Cà phê đen").category(catNoTopping).active(true).build();

            SessionCart cart = SessionCart.builder().id(50L).version(1L).build();
            when(cartRepository.findBySessionIdWithLock(5L)).thenReturn(Optional.of(cart));
            when(productRepository.findById(10L)).thenReturn(Optional.of(drink));

            CartDto.ReplaceCartRequest req = CartDto.ReplaceCartRequest.builder()
                    .items(List.of(CartDto.CartItemRequest.builder()
                            .productId(10L)
                            .quantity(1)
                            .toppingIds(List.of(99L))
                            .build()))
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    sessionCartService.replaceCart(5L, req, null)
            );

            assertTrue(ex.getMessage().contains("không cho phép chọn topping đi kèm"));
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class CancellationRulesTest {
        @Mock OrderRepository orderRepository;
        @Mock InvoiceRepository invoiceRepository;
        @Mock PaymentRepository paymentRepository;
        @Mock CancellationRequestRepository cancellationRepository;
        @Mock RefundRequestRepository refundRepository;
        @Mock AccountRepository accountRepository;
        @Mock BusinessAuditRepository auditRepository;
        @Mock RealtimeEventPublisher publisher;
        @InjectMocks CancellationService cancellationService;

        @Test
        void unpaidOrderIsCancelledDirectlyWithoutPendingApproval() {
            TableSession session = TableSession.builder().id(1L).build();
            Order order = Order.builder()
                    .id(101L)
                    .session(session)
                    .status(OrderStatus.CHO_XAC_NHAN)
                    .build();
            Invoice invoice = Invoice.builder().id(201L).order(order).status(InvoiceStatus.HIEU_LUC).build();

            when(orderRepository.findByIdWithLock(101L)).thenReturn(Optional.of(order));
            when(invoiceRepository.findByOrderId(101L)).thenReturn(Optional.of(invoice));
            when(paymentRepository.existsByInvoiceId(201L)).thenReturn(false);
            when(cancellationRepository.save(any(CancellationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CurrentActor actor = CurrentActor.tableGuest(1L, 1L);
            CancellationDto.CreateCancellationRequest req = CancellationDto.CreateCancellationRequest.builder()
                    .reason("Khách đổi ý")
                    .build();

            var res = cancellationService.requestCancellation(101L, req, actor);

            assertEquals(CancellationStatus.CHAP_THUAN, res.getStatus());
            assertEquals(OrderStatus.DA_HUY, order.getStatus());
            assertEquals(InvoiceStatus.DA_HUY, invoice.getStatus());
        }

        @Test
        void paidOrderCookingCreatesPendingCancellationRequest() {
            TableSession session = TableSession.builder().id(1L).build();
            Order order = Order.builder()
                    .id(102L)
                    .session(session)
                    .status(OrderStatus.CHO_CHE_BIEN)
                    .build();
            Invoice invoice = Invoice.builder().id(202L).order(order).status(InvoiceStatus.HIEU_LUC).build();

            when(orderRepository.findByIdWithLock(102L)).thenReturn(Optional.of(order));
            when(invoiceRepository.findByOrderId(102L)).thenReturn(Optional.of(invoice));
            when(paymentRepository.existsByInvoiceId(202L)).thenReturn(true);
            when(cancellationRepository.findFirstByOrderIdAndStatus(102L, CancellationStatus.CHO)).thenReturn(Optional.empty());
            when(cancellationRepository.save(any(CancellationRequest.class))).thenAnswer(invocation -> {
                CancellationRequest cr = invocation.getArgument(0);
                cr.setId(888L);
                return cr;
            });

            CurrentActor actor = CurrentActor.tableGuest(1L, 1L);
            CancellationDto.CreateCancellationRequest req = CancellationDto.CreateCancellationRequest.builder()
                    .reason("Khách bận về gấp")
                    .build();

            var res = cancellationService.requestCancellation(102L, req, actor);

            assertEquals(CancellationStatus.CHO, res.getStatus());
            assertEquals(OrderStatus.CHO_CHE_BIEN, order.getStatus());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class RefundFlowTest {
        @Mock RefundRequestRepository refundRepository;
        @Mock OrderRepository orderRepository;
        @Mock InvoiceRepository invoiceRepository;
        @Mock PaymentRepository paymentRepository;
        @Mock CancellationRequestRepository cancellationRepository;
        @Mock AccountRepository accountRepository;
        @Mock BusinessAuditRepository auditRepository;
        @Mock RealtimeEventPublisher publisher;
        @InjectMocks RefundService refundService;

        @Test
        void requestBankRefundCreatesChoDuyetForAdmin() {
            Order order = Order.builder().id(301L).status(OrderStatus.DA_HUY).build();
            Invoice invoice = Invoice.builder().id(401L).totalAmount(new BigDecimal("50000")).build();
            Payment payment = Payment.builder().id(501L).method(PaymentMethod.BANK_TRANSFER).amount(new BigDecimal("50000")).build();

            when(orderRepository.findByIdWithLock(301L)).thenReturn(Optional.of(order));
            when(refundRepository.findByOrderId(301L)).thenReturn(Optional.empty());
            when(invoiceRepository.findByOrderId(301L)).thenReturn(Optional.of(invoice));
            when(paymentRepository.findByInvoiceId(401L)).thenReturn(Optional.of(payment));
            when(refundRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> {
                RefundRequest rr = invocation.getArgument(0);
                rr.setId(901L);
                return rr;
            });

            var res = refundService.requestBankRefund(301L, 9L);

            assertNotNull(res);
            assertEquals(RefundStatus.CHO_DUYET, res.getStatus());
            assertEquals(new BigDecimal("50000"), res.getAmount());
            verify(publisher).publishAfterCommit(eq("/topic/admin"), eq("REFUND_CHANGED"), any(), any(), any(), any());
        }

        @Test
        void refundCashCompletesDirectlyWithoutAdminApproval() {
            Order order = Order.builder().id(302L).status(OrderStatus.DA_HUY).build();
            Invoice invoice = Invoice.builder().id(402L).totalAmount(new BigDecimal("60000")).build();
            Payment payment = Payment.builder().id(502L).method(PaymentMethod.CASH).amount(new BigDecimal("60000")).build();
            Account cashier = Account.builder().id(9L).fullName("Thu ngân A").build();

            when(orderRepository.findByIdWithLock(302L)).thenReturn(Optional.of(order));
            when(refundRepository.findByOrderId(302L)).thenReturn(Optional.empty());
            when(invoiceRepository.findByOrderId(302L)).thenReturn(Optional.of(invoice));
            when(paymentRepository.findByInvoiceId(402L)).thenReturn(Optional.of(payment));
            when(accountRepository.findById(9L)).thenReturn(Optional.of(cashier));
            when(refundRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> {
                RefundRequest rr = invocation.getArgument(0);
                rr.setId(902L);
                return rr;
            });

            var res = refundService.refundCash(302L, 9L);

            assertNotNull(res);
            assertEquals(RefundStatus.DA_HOAN, res.getStatus());
            assertEquals(new BigDecimal("60000"), res.getAmount());
            verify(publisher).publishAfterCommit(eq("/topic/cashier"), eq("REFUND_CHANGED"), any(), any(), any(), any());
        }
    }
}
