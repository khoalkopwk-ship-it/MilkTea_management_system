package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.CancellationDto;
import vn.edu.ute.milktea.dto.PaymentDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.cancellation.CancellationRequest;
import vn.edu.ute.milktea.entity.cancellation.CancellationStatus;
import vn.edu.ute.milktea.entity.cancellation.RefundRequest;
import vn.edu.ute.milktea.entity.cancellation.RefundStatus;
import vn.edu.ute.milktea.entity.order.Invoice;
import vn.edu.ute.milktea.entity.order.InvoiceStatus;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.order.OrderStatus;
import vn.edu.ute.milktea.entity.payment.Payment;
import vn.edu.ute.milktea.entity.payment.PaymentMethod;
import vn.edu.ute.milktea.entity.voucher.DiscountType;
import vn.edu.ute.milktea.entity.voucher.Voucher;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.cancellation.CancellationRequestRepository;
import vn.edu.ute.milktea.repository.cancellation.RefundRequestRepository;
import vn.edu.ute.milktea.repository.order.InvoiceRepository;
import vn.edu.ute.milktea.repository.order.OrderRepository;
import vn.edu.ute.milktea.repository.payment.PaymentNoticeRepository;
import vn.edu.ute.milktea.repository.payment.PaymentRepository;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;
import vn.edu.ute.milktea.repository.voucher.VoucherRepository;
import vn.edu.ute.milktea.repository.voucher.VoucherUsageHistoryRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.cancellation.CancellationService;
import vn.edu.ute.milktea.service.cancellation.RefundService;
import vn.edu.ute.milktea.service.payment.PaymentService;
import vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher;
import vn.edu.ute.milktea.service.table.TableSessionService;
import vn.edu.ute.milktea.service.voucher.VoucherService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefundAndVoucherAuditTest {

    @Mock private OrderRepository orderRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CancellationRequestRepository cancellationRepository;
    @Mock private RefundRequestRepository refundRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private BusinessAuditRepository auditRepository;
    @Mock private RealtimeEventPublisher realtimeEventPublisher;
    @Mock private DiningTableRepository tableRepository;
    @Mock private TableSessionService tableSessionService;
    @Mock private PaymentNoticeRepository paymentNoticeRepository;
    @Mock private VoucherRepository voucherRepository;
    @Mock private VoucherUsageHistoryRepository voucherHistoryRepository;

    private CancellationService cancellationService;
    private RefundService refundService;
    private PaymentService paymentService;
    private VoucherService voucherService;

    private Branch branch;

    @BeforeEach
    void setUp() {
        branch = Branch.builder().id(1L).code("CN01").name("Chi nhánh 1").build();

        voucherService = new VoucherService(
                voucherRepository, null, voucherHistoryRepository,
                null, null, orderRepository, invoiceRepository
        );

        cancellationService = new CancellationService(
                orderRepository, invoiceRepository, paymentRepository,
                cancellationRepository, refundRepository, accountRepository,
                auditRepository, null, realtimeEventPublisher, voucherService
        );

        refundService = new RefundService(
                refundRepository, accountRepository, auditRepository,
                orderRepository, invoiceRepository, paymentRepository,
                cancellationRepository, realtimeEventPublisher
        );

        paymentService = new PaymentService(
                invoiceRepository, tableRepository, tableSessionService,
                paymentRepository, orderRepository, paymentNoticeRepository,
                accountRepository, auditRepository, realtimeEventPublisher
        );
    }

    @Test
    @DisplayName("F15: Khi hủy đơn đã thanh toán ở bước CHO_CHE_BIEN, hệ thống tự động sinh RefundRequest")
    void testCancelPaidOrderAutoCreatesRefundRequest() {
        Order order = Order.builder()
                .id(801L)
                .branch(branch)
                .status(OrderStatus.CHO_CHE_BIEN)
                .build();

        Invoice invoice = Invoice.builder()
                .id(901L)
                .order(order)
                .totalAmount(BigDecimal.valueOf(70000))
                .status(InvoiceStatus.HIEU_LUC)
                .build();

        Payment payment = Payment.builder()
                .id(951L)
                .invoice(invoice)
                .method(PaymentMethod.BANK_TRANSFER)
                .amount(BigDecimal.valueOf(70000))
                .build();

        CancellationRequest cancelReq = CancellationRequest.builder()
                .id(601L)
                .order(order)
                .status(CancellationStatus.CHO)
                .build();

        when(cancellationRepository.findById(601L)).thenReturn(Optional.of(cancelReq));
        when(orderRepository.findByIdWithLock(801L)).thenReturn(Optional.of(order));
        when(invoiceRepository.findByOrderId(801L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.existsByInvoiceId(901L)).thenReturn(true);
        when(paymentRepository.findByInvoiceId(901L)).thenReturn(Optional.of(payment));
        when(refundRepository.findByOrderId(801L)).thenReturn(Optional.empty());
        when(refundRepository.save(any())).thenAnswer(inv -> {
            RefundRequest r = inv.getArgument(0);
            r.setId(777L);
            return r;
        });
        when(cancellationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CurrentActor cashierActor = CurrentActor.authenticated(20L, Role.CASHIER, 1L, List.of(1L));
        CancellationDto.DecideCancellationRequest decideReq = CancellationDto.DecideCancellationRequest.builder()
                .approved(true)
                .reason("Khách đổi ý, đồng ý hủy")
                .build();

        cancellationService.decideCancellation(601L, decideReq, cashierActor);

        assertEquals(OrderStatus.DA_HUY, order.getStatus());
        assertEquals(CancellationStatus.CHAP_THUAN, cancelReq.getStatus());
        verify(refundRepository, times(1)).save(argThat(r ->
                r.getOrder().getId().equals(801L) &&
                r.getAmount().compareTo(BigDecimal.valueOf(70000)) == 0 &&
                r.getStatus() == RefundStatus.CHO_DUYET
        ));
    }

    @Test
    @DisplayName("F16: Khi yêu cầu hoàn tiền bị TU_CHOI, thu ngân gửi lại cập nhật bản ghi cũ không lỗi duplicate")
    void testResubmitRefundOnRejectedUpdatesExistingWithoutConflict() {
        Order order = Order.builder().id(802L).branch(branch).status(OrderStatus.DA_HUY).build();
        Invoice invoice = Invoice.builder().id(902L).order(order).totalAmount(BigDecimal.valueOf(50000)).build();
        Payment payment = Payment.builder().id(952L).method(PaymentMethod.BANK_TRANSFER).amount(BigDecimal.valueOf(50000)).build();

        RefundRequest existingRejected = RefundRequest.builder()
                .id(401L)
                .order(order)
                .payment(payment)
                .amount(BigDecimal.valueOf(50000))
                .status(RefundStatus.TU_CHOI)
                .build();

        when(orderRepository.findByIdWithLock(802L)).thenReturn(Optional.of(order));
        when(refundRepository.findByOrderId(802L)).thenReturn(Optional.of(existingRejected));
        when(refundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CurrentActor cashierActor = CurrentActor.authenticated(20L, Role.CASHIER, 1L, List.of(1L));
        CancellationDto.RefundResponse response = refundService.requestBankRefund(802L, cashierActor);

        assertNotNull(response);
        assertEquals(RefundStatus.CHO_DUYET, response.getStatus());
        assertEquals(401L, response.getId());
        verify(refundRepository, times(1)).save(existingRejected);
    }

    @Test
    @DisplayName("F17: Cho phép thanh toán hóa đơn 0 VND khi voucher giảm 100%")
    void testZeroDongPaymentAllowedWhenFullyDiscounted() {
        Order order = Order.builder().id(803L).branch(branch).status(OrderStatus.CHO_THANH_TOAN).build();
        Account cashier = Account.builder().id(20L).fullName("Thu ngân 1").role(Role.CASHIER).branch(branch).build();

        Invoice invoice = Invoice.builder()
                .id(903L)
                .order(order)
                .totalAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.HIEU_LUC)
                .build();

        when(accountRepository.findById(20L)).thenReturn(Optional.of(cashier));
        when(invoiceRepository.findById(903L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findByIdWithLock(903L)).thenReturn(Optional.of(invoice));
        when(orderRepository.findByIdWithLock(803L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(999L);
            return p;
        });
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentDto.RecordPaymentRequest req = PaymentDto.RecordPaymentRequest.builder()
                .method(PaymentMethod.CASH)
                .build();

        PaymentDto.PaymentResponse resp = paymentService.recordReceipt(903L, req, 20L, "KEY-ZERO-01");
        assertNotNull(resp);
        assertEquals(0, BigDecimal.ZERO.compareTo(resp.getAmount()));
    }

    @Test
    @DisplayName("F17: Khách vãng lai bị từ chối áp dụng voucher có giới hạn lượt theo khách hàng")
    void testGuestRejectedForCustomerLimitedVoucher() {
        Voucher voucher = Voucher.builder()
                .id(1L)
                .code("QUOTA_VIP")
                .discountType(DiscountType.PERCENT)
                .discountValue(BigDecimal.valueOf(20))
                .isActive(true)
                .startDate(java.time.Instant.now().minusSeconds(3600))
                .endDate(java.time.Instant.now().plusSeconds(3600))
                .totalUsageLimit(100)
                .usedCount(0)
                .applyAllBranches(true)
                .usageLimitPerCustomer(1)
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                voucherService.validateVoucherEligibility(voucher, 1L, BigDecimal.valueOf(100000), null)
        );

        assertEquals(ErrorCode.VOUCHER_USER_LIMIT_EXCEEDED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("yêu cầu đăng nhập tài khoản"));
    }
}
