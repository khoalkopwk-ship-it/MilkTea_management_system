package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.AdminDto;
import vn.edu.ute.milktea.dto.PaymentDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.order.Invoice;
import vn.edu.ute.milktea.entity.order.InvoiceStatus;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.order.OrderStatus;
import vn.edu.ute.milktea.entity.payment.PaymentMethod;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.branch.BranchProductRepository;
import vn.edu.ute.milktea.repository.branch.BranchRepository;
import vn.edu.ute.milktea.repository.branch.StaffBranchRepository;
import vn.edu.ute.milktea.repository.catalog.CategoryRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.order.InvoiceRepository;
import vn.edu.ute.milktea.repository.order.OrderRepository;
import vn.edu.ute.milktea.repository.payment.PaymentNoticeRepository;
import vn.edu.ute.milktea.repository.payment.PaymentRepository;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.admin.AdminService;
import vn.edu.ute.milktea.service.payment.PaymentService;
import vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher;
import vn.edu.ute.milktea.service.table.TableSessionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BranchAccessControlAuditTest {

    @Mock private AccountRepository accountRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private DiningTableRepository tableRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private BranchProductRepository branchProductRepository;
    @Mock private StaffBranchRepository staffBranchRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private BusinessAuditRepository auditRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private TableSessionService tableSessionService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentNoticeRepository paymentNoticeRepository;
    @Mock private RealtimeEventPublisher realtimeEventPublisher;

    private AdminService adminService;
    private PaymentService paymentService;

    private Branch branch1;
    private Branch branch2;

    @BeforeEach
    void setUp() {
        branch1 = Branch.builder().id(1L).code("CN01").name("Chi nhánh 1").status("HOAT_DONG").build();
        branch2 = Branch.builder().id(2L).code("CN02").name("Chi nhánh 2").status("HOAT_DONG").build();

        adminService = new AdminService(
                accountRepository, categoryRepository, productRepository, null,
                tableRepository, null, null, null, null, null,
                auditRepository, realtimeEventPublisher, passwordEncoder,
                branchRepository, staffBranchRepository, branchProductRepository, null
        );

        paymentService = new PaymentService(
                invoiceRepository, tableRepository, tableSessionService,
                paymentRepository, orderRepository, paymentNoticeRepository,
                accountRepository, auditRepository, realtimeEventPublisher, staffBranchRepository
        );
    }

    @Test
    @DisplayName("F18 & F26: Manager không thể tạo tài khoản nhân viên thiếu targetBranchId")
    void testManagerCannotCreateStaffWithoutBranch() {
        AdminDto.CreateStaffRequest req = AdminDto.CreateStaffRequest.builder()
                .fullName("Thu ngân test")
                .email("cashier_test@milktea.vn")
                .password("Password123")
                .role(Role.CASHIER)
                .branchId(null)
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminService.createStaffAccount(req, null)
        );
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("bắt buộc phải chọn chi nhánh"));
    }

    @Test
    @DisplayName("F06: Thu ngân chi nhánh 1 bị từ chối thanh toán hóa đơn của chi nhánh 2")
    void testCashierCannotRecordReceiptForCrossBranchOrder() {
        Account cashier1 = Account.builder()
                .id(101L)
                .fullName("Thu ngân CN1")
                .role(Role.CASHIER)
                .branch(branch1)
                .build();
        when(accountRepository.findById(101L)).thenReturn(Optional.of(cashier1));

        Order orderCN2 = Order.builder()
                .id(501L)
                .branch(branch2)
                .status(OrderStatus.CHO_THANH_TOAN)
                .build();

        Invoice invoice = Invoice.builder()
                .id(201L)
                .order(orderCN2)
                .totalAmount(BigDecimal.valueOf(50000))
                .status(InvoiceStatus.HIEU_LUC)
                .build();

        when(invoiceRepository.findById(201L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findByIdWithLock(201L)).thenReturn(Optional.of(invoice));
        when(orderRepository.findByIdWithLock(501L)).thenReturn(Optional.of(orderCN2));

        PaymentDto.RecordPaymentRequest req = PaymentDto.RecordPaymentRequest.builder()
                .method(PaymentMethod.CASH)
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentService.recordReceipt(201L, req, 101L, "KEY-PAY-CROSS-01")
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("không thuộc chi nhánh của đơn hàng"));
    }

    @Test
    @DisplayName("F06: Admin toàn chuỗi được phép thu tiền mọi chi nhánh")
    void testAdminCanRecordReceiptAcrossAnyBranch() {
        Account admin = Account.builder()
                .id(999L)
                .fullName("Admin Tổng")
                .role(Role.ADMIN)
                .build();
        when(accountRepository.findById(999L)).thenReturn(Optional.of(admin));

        Order orderCN2 = Order.builder()
                .id(502L)
                .branch(branch2)
                .status(OrderStatus.CHO_THANH_TOAN)
                .build();

        Invoice invoice = Invoice.builder()
                .id(202L)
                .order(orderCN2)
                .totalAmount(BigDecimal.valueOf(50000))
                .status(InvoiceStatus.HIEU_LUC)
                .build();

        when(invoiceRepository.findById(202L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findByIdWithLock(202L)).thenReturn(Optional.of(invoice));
        when(orderRepository.findByIdWithLock(502L)).thenReturn(Optional.of(orderCN2));
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            vn.edu.ute.milktea.entity.payment.Payment p = inv.getArgument(0);
            p.setId(999L);
            return p;
        });
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentDto.RecordPaymentRequest req = PaymentDto.RecordPaymentRequest.builder()
                .method(PaymentMethod.CASH)
                .build();

        assertDoesNotThrow(() ->
                paymentService.recordReceipt(202L, req, 999L, "KEY-PAY-ADMIN-01")
        );
    }
}
