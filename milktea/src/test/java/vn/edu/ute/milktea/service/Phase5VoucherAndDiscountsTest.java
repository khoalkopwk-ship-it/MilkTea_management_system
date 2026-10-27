package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.VoucherDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.order.Invoice;
import vn.edu.ute.milktea.entity.order.InvoiceStatus;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.voucher.*;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.branch.BranchRepository;
import vn.edu.ute.milktea.repository.order.InvoiceRepository;
import vn.edu.ute.milktea.repository.order.OrderRepository;
import vn.edu.ute.milktea.repository.voucher.VoucherBranchRepository;
import vn.edu.ute.milktea.repository.voucher.VoucherRepository;
import vn.edu.ute.milktea.repository.voucher.VoucherUsageHistoryRepository;
import vn.edu.ute.milktea.service.voucher.VoucherService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Bộ kiểm thử toàn diện Giai đoạn 5: Hệ thống Voucher & Khuyến mãi (Voucher & Discounts)
 * - BR-06: Quy tắc Voucher (thời hạn, phạm vi chi nhánh, giá trị đơn tối thiểu, giới hạn lượt dùng)
 * - AT-05: Concurrency & Race condition handling (Atomic update tranh chấp lượt voucher cuối)
 * - Hoàn trả lượt voucher khi đơn hàng bị hủy / hoàn tiền
 * - Kiểm thử Admin CRUD voucher
 */
@ExtendWith(MockitoExtension.class)
public class Phase5VoucherAndDiscountsTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private VoucherBranchRepository voucherBranchRepository;

    @Mock
    private VoucherUsageHistoryRepository voucherUsageHistoryRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    private VoucherService voucherService;

    private Branch branchCN01;
    private Branch branchCN02;
    private Account testCustomer;
    private Voucher testVoucherPercent;
    private Voucher testVoucherFixed;

    @BeforeEach
    void setUp() {
        voucherService = new VoucherService(
                voucherRepository,
                voucherBranchRepository,
                voucherUsageHistoryRepository,
                branchRepository,
                accountRepository,
                orderRepository,
                invoiceRepository
        );

        branchCN01 = Branch.builder().id(1L).code("CN01").name("Chi nhánh Quận 1").build();
        branchCN02 = Branch.builder().id(2L).code("CN02").name("Chi nhánh Thủ Đức").build();

        testCustomer = Account.builder()
                .id(10L)
                .email("customer@test.com")
                .fullName("Nguyễn Văn Khách")
                .role(Role.CUSTOMER)
                .build();

        testVoucherPercent = Voucher.builder()
                .id(100L)
                .code("MILKTEA10")
                .name("Giảm 10% tối đa 30k")
                .discountType(DiscountType.PERCENT)
                .discountValue(BigDecimal.valueOf(10))
                .maxDiscountAmount(BigDecimal.valueOf(30000))
                .minOrderValue(BigDecimal.valueOf(60000))
                .totalUsageLimit(100)
                .usedCount(5)
                .usageLimitPerCustomer(2)
                .applyAllBranches(true)
                .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .endDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .isActive(true)
                .build();

        testVoucherFixed = Voucher.builder()
                .id(200L)
                .code("CHAOMUNG")
                .name("Giảm 20k đơn từ 50k")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(20000))
                .minOrderValue(BigDecimal.valueOf(50000))
                .totalUsageLimit(50)
                .usedCount(10)
                .usageLimitPerCustomer(1)
                .applyAllBranches(true)
                .startDate(Instant.now().minus(2, ChronoUnit.DAYS))
                .endDate(Instant.now().plus(5, ChronoUnit.DAYS))
                .isActive(true)
                .build();
    }

    // =========================================================================
    // 1. KIỂM THỬ XÁC THỰC VOUCHER (VALIDATION)
    // =========================================================================

    @Test
    @DisplayName("Validate Voucher: Ném VOUCHER_NOT_FOUND khi mã voucher không tồn tại")
    void testValidateVoucher_NotFound() {
        when(voucherRepository.findByCodeIgnoreCase("NONEXISTENT")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                voucherService.validateVoucher("NONEXISTENT", 1L, BigDecimal.valueOf(100000), 10L));

        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("Validate Voucher: Ném VOUCHER_INVALID khi voucher bị vô hiệu hóa")
    void testValidateVoucher_Inactive() {
        testVoucherFixed.setIsActive(false);
        when(voucherRepository.findByCodeIgnoreCase("CHAOMUNG")).thenReturn(Optional.of(testVoucherFixed));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                voucherService.validateVoucher("CHAOMUNG", 1L, BigDecimal.valueOf(100000), 10L));

        assertEquals(ErrorCode.VOUCHER_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("Validate Voucher: Ném VOUCHER_EXPIRED khi voucher đã quá hạn kết thúc")
    void testValidateVoucher_Expired() {
        testVoucherFixed.setEndDate(Instant.now().minus(1, ChronoUnit.HOURS));
        when(voucherRepository.findByCodeIgnoreCase("CHAOMUNG")).thenReturn(Optional.of(testVoucherFixed));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                voucherService.validateVoucher("CHAOMUNG", 1L, BigDecimal.valueOf(100000), 10L));

        assertEquals(ErrorCode.VOUCHER_EXPIRED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Validate Voucher: Ném VOUCHER_EXPIRED khi chưa đến ngày bắt đầu voucher")
    void testValidateVoucher_NotStarted() {
        testVoucherFixed.setStartDate(Instant.now().plus(1, ChronoUnit.DAYS));
        when(voucherRepository.findByCodeIgnoreCase("CHAOMUNG")).thenReturn(Optional.of(testVoucherFixed));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                voucherService.validateVoucher("CHAOMUNG", 1L, BigDecimal.valueOf(100000), 10L));

        assertEquals(ErrorCode.VOUCHER_EXPIRED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Validate Voucher: Ném VOUCHER_EXHAUSTED khi voucher đã chạm giới hạn tổng số lượt dùng")
    void testValidateVoucher_Exhausted() {
        testVoucherFixed.setUsedCount(50);
        testVoucherFixed.setTotalUsageLimit(50);
        when(voucherRepository.findByCodeIgnoreCase("CHAOMUNG")).thenReturn(Optional.of(testVoucherFixed));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                voucherService.validateVoucher("CHAOMUNG", 1L, BigDecimal.valueOf(100000), 10L));

        assertEquals(ErrorCode.VOUCHER_EXHAUSTED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Validate Voucher: Ném VOUCHER_MIN_ORDER_NOT_MET khi giá trị đơn chưa đạt minOrderValue")
    void testValidateVoucher_MinOrderNotMet() {
        when(voucherRepository.findByCodeIgnoreCase("CHAOMUNG")).thenReturn(Optional.of(testVoucherFixed));

        // minOrderValue = 50,000đ, subtotal = 40,000đ
        BusinessException ex = assertThrows(BusinessException.class, () ->
                voucherService.validateVoucher("CHAOMUNG", 1L, BigDecimal.valueOf(40000), 10L));

        assertEquals(ErrorCode.VOUCHER_MIN_ORDER_NOT_MET, ex.getErrorCode());
    }

    @Test
    @DisplayName("Validate Voucher: Ném VOUCHER_NOT_APPLICABLE_BRANCH khi chi nhánh không nằm trong danh sách áp dụng")
    void testValidateVoucher_BranchNotApplicable() {
        Voucher branchScoped = Voucher.builder()
                .id(300L)
                .code("CN01ONLY")
                .name("Chỉ áp dụng CN01")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(15000))
                .minOrderValue(BigDecimal.valueOf(30000))
                .totalUsageLimit(100)
                .usedCount(0)
                .usageLimitPerCustomer(1)
                .applyAllBranches(false)
                .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .endDate(Instant.now().plus(5, ChronoUnit.DAYS))
                .isActive(true)
                .build();

        when(voucherRepository.findByCodeIgnoreCase("CN01ONLY")).thenReturn(Optional.of(branchScoped));
        when(voucherBranchRepository.existsByIdVoucherIdAndIdBranchId(300L, 2L)).thenReturn(false);

        // Đặt tại CN02 (branchId = 2)
        BusinessException ex = assertThrows(BusinessException.class, () ->
                voucherService.validateVoucher("CN01ONLY", 2L, BigDecimal.valueOf(50000), 10L));

        assertEquals(ErrorCode.VOUCHER_NOT_APPLICABLE_BRANCH, ex.getErrorCode());
    }

    @Test
    @DisplayName("Validate Voucher: Ném VOUCHER_USER_LIMIT_EXCEEDED khi khách hàng đã dùng hết số lượt cho phép")
    void testValidateVoucher_UserLimitExceeded() {
        when(voucherRepository.findByCodeIgnoreCase("CHAOMUNG")).thenReturn(Optional.of(testVoucherFixed));
        // testVoucherFixed cho phép tối đa 1 lần/khách. Mock đã dùng 1 lần DA_DUNG
        when(voucherUsageHistoryRepository.countByVoucherIdAndAccountIdAndStatus(200L, 10L, VoucherUsageStatus.DA_DUNG))
                .thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                voucherService.validateVoucher("CHAOMUNG", 1L, BigDecimal.valueOf(100000), 10L));

        assertEquals(ErrorCode.VOUCHER_USER_LIMIT_EXCEEDED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Validate Voucher: Thành công với FIXED_AMOUNT (Giảm 20.000đ)")
    void testValidateVoucher_FixedAmount_Success() {
        when(voucherRepository.findByCodeIgnoreCase("CHAOMUNG")).thenReturn(Optional.of(testVoucherFixed));
        when(voucherUsageHistoryRepository.countByVoucherIdAndAccountIdAndStatus(200L, 10L, VoucherUsageStatus.DA_DUNG))
                .thenReturn(0L);

        var res = voucherService.validateVoucher("CHAOMUNG", 1L, BigDecimal.valueOf(80000), 10L);

        assertTrue(res.isValid());
        assertEquals("CHAOMUNG", res.getVoucherCode());
        assertEquals(BigDecimal.valueOf(20000), res.getDiscountAmount());
        assertEquals(BigDecimal.valueOf(80000), res.getOriginalSubtotal());
        assertEquals(BigDecimal.valueOf(60000), res.getFinalSubtotal());
    }

    @Test
    @DisplayName("Validate Voucher: Thành công với PERCENT và áp dụng mức chặn maxDiscountAmount")
    void testValidateVoucher_PercentWithCap_Success() {
        when(voucherRepository.findByCodeIgnoreCase("MILKTEA10")).thenReturn(Optional.of(testVoucherPercent));
        when(voucherUsageHistoryRepository.countByVoucherIdAndAccountIdAndStatus(100L, 10L, VoucherUsageStatus.DA_DUNG))
                .thenReturn(0L);

        // Đơn 500,000đ. 10% = 50,000đ. Do maxDiscountAmount = 30,000đ nên giảm đúng 30,000đ
        var res = voucherService.validateVoucher("MILKTEA10", 1L, BigDecimal.valueOf(500000), 10L);

        assertTrue(res.isValid());
        assertEquals(BigDecimal.valueOf(30000), res.getDiscountAmount());
        assertEquals(BigDecimal.valueOf(470000), res.getFinalSubtotal());
    }

    // =========================================================================
    // 2. KIỂM THỬ ĐỒNG THỜI & TRANH CHẤP AT-05 (ATOMIC CONCURRENCY & RACE CONDITION)
    // =========================================================================

    @Test
    @DisplayName("AT-05: Áp dụng voucher thành công khi atomic update incrementUsageCount trả về 1")
    void testApplyVoucherToOrder_AtomicIncrementSuccess() {
        Order order = Order.builder()
                .id(1001L)
                .branch(branchCN01)
                .account(testCustomer)
                .build();

        Invoice invoice = Invoice.builder()
                .id(2001L)
                .order(order)
                .subtotal(BigDecimal.valueOf(100000))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(100000))
                .build();

        when(voucherRepository.findByCodeIgnoreCase("CHAOMUNG")).thenReturn(Optional.of(testVoucherFixed));
        when(voucherUsageHistoryRepository.countByVoucherIdAndAccountIdAndStatus(200L, 10L, VoucherUsageStatus.DA_DUNG)).thenReturn(0L);
        // Giả lập atomic update thành công
        when(voucherRepository.incrementUsageCount(200L)).thenReturn(1);

        voucherService.applyVoucherToOrder(order, invoice, "CHAOMUNG", 10L);

        // Xác minh voucher và snapshot được ghi vào Order và Invoice
        assertEquals(testVoucherFixed, order.getVoucher());
        assertEquals("CHAOMUNG", order.getVoucherCode());
        assertEquals("CHAOMUNG", invoice.getVoucherCode());
        assertEquals(BigDecimal.valueOf(20000), invoice.getVoucherDiscountAmount());
        assertEquals(BigDecimal.valueOf(80000), invoice.getTotalAmount());

        // Xác minh lưu lịch sử sử dụng voucher
        ArgumentCaptor<VoucherUsageHistory> historyCaptor = ArgumentCaptor.forClass(VoucherUsageHistory.class);
        verify(voucherUsageHistoryRepository).save(historyCaptor.capture());
        VoucherUsageHistory history = historyCaptor.getValue();
        assertEquals(testVoucherFixed, history.getVoucher());
        assertEquals(order, history.getOrder());
        assertEquals(BigDecimal.valueOf(20000), history.getDiscountAmount());
        assertEquals(VoucherUsageStatus.DA_DUNG, history.getStatus());
    }

    @Test
    @DisplayName("AT-05: Tranh chấp slot cuối cùng (Race condition) bị chặn và ném VOUCHER_EXHAUSTED khi incrementUsageCount trả về 0")
    void testApplyVoucherToOrder_RaceConditionSlotExhausted() {
        Order order = Order.builder()
                .id(1002L)
                .branch(branchCN01)
                .account(testCustomer)
                .build();

        Invoice invoice = Invoice.builder()
                .id(2002L)
                .order(order)
                .subtotal(BigDecimal.valueOf(100000))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(100000))
                .build();

        when(voucherRepository.findByCodeIgnoreCase("CHAOMUNG")).thenReturn(Optional.of(testVoucherFixed));
        when(voucherUsageHistoryRepository.countByVoucherIdAndAccountIdAndStatus(200L, 10L, VoucherUsageStatus.DA_DUNG)).thenReturn(0L);

        // Giả lập atomic update thất bại (trả về 0 do một transaction khác đã lấy slot cuối trước đó)
        when(voucherRepository.incrementUsageCount(200L)).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                voucherService.applyVoucherToOrder(order, invoice, "CHAOMUNG", 10L));

        assertEquals(ErrorCode.VOUCHER_EXHAUSTED, ex.getErrorCode());
        // Không lưu history khi thất bại
        verify(voucherUsageHistoryRepository, never()).save(any());
    }

    // =========================================================================
    // 3. HOÀN TRẢ VOUCHER KHI HỦY ĐƠN HÀNG (RELEASE & RESTORE USAGE COUNT)
    // =========================================================================

    @Test
    @DisplayName("Release Voucher: Cập nhật lịch sử sang DA_HOAN_TRA và giảm số lượt đã dùng khi hủy đơn")
    void testReleaseVoucherFromOrder_Success() {
        Order order = Order.builder().id(1003L).build();

        VoucherUsageHistory history = VoucherUsageHistory.builder()
                .id(501L)
                .voucher(testVoucherFixed)
                .order(order)
                .status(VoucherUsageStatus.DA_DUNG)
                .build();

        when(voucherUsageHistoryRepository.findByOrderId(1003L)).thenReturn(Optional.of(history));

        voucherService.releaseVoucherFromOrder(order, "Khách đổi ý");

        assertEquals(VoucherUsageStatus.DA_HOAN_TRA, history.getStatus());
        assertEquals("Khách đổi ý", history.getRefundReason());
        assertNotNull(history.getRefundedAt());

        verify(voucherUsageHistoryRepository).save(history);
        verify(voucherRepository).decrementUsageCount(200L);
    }

    @Test
    @DisplayName("Release Voucher: Không giảm count lần 2 nếu đơn đã được hoàn trả voucher trước đó")
    void testReleaseVoucherFromOrder_AlreadyRefunded_NoOp() {
        Order order = Order.builder().id(1004L).build();

        VoucherUsageHistory history = VoucherUsageHistory.builder()
                .id(502L)
                .voucher(testVoucherFixed)
                .order(order)
                .status(VoucherUsageStatus.DA_HOAN_TRA)
                .build();

        when(voucherUsageHistoryRepository.findByOrderId(1004L)).thenReturn(Optional.of(history));

        voucherService.releaseVoucherFromOrder(order, "Hủy lặp lại");

        verify(voucherRepository, never()).decrementUsageCount(anyLong());
    }

    // =========================================================================
    // 4. DANH SÁCH VOUCHER KHẢ DỤNG CHO CHI NHÁNH & KHÁCH HÀNG
    // =========================================================================

    @Test
    @DisplayName("Active Vouchers: Lọc đúng voucher theo chi nhánh và gắn cờ canApply chuẩn xác")
    void testGetActiveVouchersForBranch() {
        Voucher vBranchScoped = Voucher.builder()
                .id(300L)
                .code("CN02ONLY")
                .name("Chỉ CN02")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(10000))
                .minOrderValue(BigDecimal.valueOf(30000))
                .totalUsageLimit(100)
                .usedCount(0)
                .usageLimitPerCustomer(1)
                .applyAllBranches(false)
                .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .endDate(Instant.now().plus(5, ChronoUnit.DAYS))
                .isActive(true)
                .build();

        when(voucherRepository.findActiveVouchers(any())).thenReturn(List.of(testVoucherFixed, vBranchScoped));
        when(voucherBranchRepository.existsByIdVoucherIdAndIdBranchId(300L, 1L)).thenReturn(false);

        // Khách ở CN01, subtotal = 40,000đ (testVoucherFixed minOrder = 50,000đ)
        var result = voucherService.getActiveVouchersForBranch(1L, BigDecimal.valueOf(40000), 10L);

        // vBranchScoped bị loại bỏ vì không thuộc CN01
        assertEquals(1, result.size());
        VoucherDto.VoucherResponse vRes = result.get(0);
        assertEquals("CHAOMUNG", vRes.getCode());
        // Do đơn 40k < 50k nên canApply = false
        assertFalse(vRes.getCanApply());
    }

    // =========================================================================
    // 5. KIỂM THỬ ADMIN CRUD VOUCHER
    // =========================================================================

    @Test
    @DisplayName("Admin: Tạo voucher mới thành công khi code chưa tồn tại")
    void testCreateVoucher_Success() {
        VoucherDto.CreateVoucherRequest req = VoucherDto.CreateVoucherRequest.builder()
                .code("NEWYEAR2026")
                .name("Ưu đãi Tết 2026")
                .discountType(DiscountType.PERCENT)
                .discountValue(BigDecimal.valueOf(15))
                .maxDiscountAmount(BigDecimal.valueOf(40000))
                .minOrderValue(BigDecimal.valueOf(80000))
                .totalUsageLimit(200)
                .usageLimitPerCustomer(2)
                .applyAllBranches(true)
                .startDate(Instant.now())
                .endDate(Instant.now().plus(10, ChronoUnit.DAYS))
                .isActive(true)
                .build();

        when(voucherRepository.findByCodeIgnoreCase("NEWYEAR2026")).thenReturn(Optional.empty());
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> {
            Voucher saved = invocation.getArgument(0);
            saved.setId(999L);
            return saved;
        });

        VoucherDto.VoucherResponse res = voucherService.createVoucher(req);

        assertNotNull(res);
        assertEquals("NEWYEAR2026", res.getCode());
        assertEquals("Ưu đãi Tết 2026", res.getName());
        assertEquals(200, res.getRemainingUses());
    }

    @Test
    @DisplayName("Admin: Ném VALIDATION_FAILED khi tạo voucher với code đã tồn tại")
    void testCreateVoucher_DuplicateCode() {
        VoucherDto.CreateVoucherRequest req = VoucherDto.CreateVoucherRequest.builder()
                .code("CHAOMUNG")
                .name("Trùng code")
                .startDate(Instant.now())
                .endDate(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();

        when(voucherRepository.findByCodeIgnoreCase("CHAOMUNG")).thenReturn(Optional.of(testVoucherFixed));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                voucherService.createVoucher(req));

        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Admin: Bật / tắt trạng thái toggleVoucherStatus thành công")
    void testToggleVoucherStatus_Success() {
        when(voucherRepository.findById(200L)).thenReturn(Optional.of(testVoucherFixed));

        assertTrue(testVoucherFixed.getIsActive());
        voucherService.toggleVoucherStatus(200L);

        assertFalse(testVoucherFixed.getIsActive());
        verify(voucherRepository).save(testVoucherFixed);
    }
}
