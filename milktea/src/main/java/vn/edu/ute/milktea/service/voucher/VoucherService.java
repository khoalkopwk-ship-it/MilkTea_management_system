package vn.edu.ute.milktea.service.voucher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.VoucherDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.order.Invoice;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.voucher.*;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.branch.BranchRepository;
import vn.edu.ute.milktea.repository.order.InvoiceRepository;
import vn.edu.ute.milktea.repository.order.OrderRepository;
import vn.edu.ute.milktea.repository.voucher.VoucherBranchRepository;
import vn.edu.ute.milktea.repository.voucher.VoucherRepository;
import vn.edu.ute.milktea.repository.voucher.VoucherUsageHistoryRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherBranchRepository voucherBranchRepository;
    private final VoucherUsageHistoryRepository voucherUsageHistoryRepository;
    private final BranchRepository branchRepository;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public VoucherDto.ValidateVoucherResponse validateVoucher(
            String code, Long branchId, BigDecimal subtotal, Long accountId) {

        if (code == null || code.trim().isEmpty()) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Mã voucher không được để trống");
        }

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VOUCHER_NOT_FOUND, "Không tìm thấy mã voucher: " + code));

        validateVoucherEligibility(voucher, branchId, subtotal, accountId);

        BigDecimal discountAmount = calculateDiscount(voucher, subtotal);
        BigDecimal original = subtotal != null ? subtotal : BigDecimal.ZERO;
        BigDecimal finalSubtotal = original.subtract(discountAmount).max(BigDecimal.ZERO);

        return VoucherDto.ValidateVoucherResponse.builder()
                .valid(true)
                .voucherId(voucher.getId())
                .voucherCode(voucher.getCode())
                .voucherName(voucher.getName())
                .discountAmount(discountAmount)
                .originalSubtotal(original)
                .finalSubtotal(finalSubtotal)
                .message("Áp dụng mã voucher " + voucher.getCode() + " thành công")
                .build();
    }

    public void validateVoucherEligibility(Voucher voucher, Long branchId, BigDecimal subtotal, Long accountId) {
        if (!Boolean.TRUE.equals(voucher.getIsActive())) {
            throw BusinessException.badRequest(ErrorCode.VOUCHER_INVALID, "Voucher hiện đang bị khóa hoặc ngưng áp dụng");
        }

        Instant now = Instant.now();
        if (now.isBefore(voucher.getStartDate())) {
            throw BusinessException.badRequest(ErrorCode.VOUCHER_EXPIRED, "Chương trình khuyến mãi chưa bắt đầu");
        }
        if (now.isAfter(voucher.getEndDate())) {
            throw BusinessException.badRequest(ErrorCode.VOUCHER_EXPIRED, "Voucher đã hết hạn sử dụng");
        }

        if (voucher.getUsedCount() >= voucher.getTotalUsageLimit()) {
            throw BusinessException.badRequest(ErrorCode.VOUCHER_EXHAUSTED, "Voucher đã hết lượt sử dụng");
        }

        if (!Boolean.TRUE.equals(voucher.getApplyAllBranches())) {
            if (branchId == null || !voucherBranchRepository.existsByIdVoucherIdAndIdBranchId(voucher.getId(), branchId)) {
                throw BusinessException.badRequest(ErrorCode.VOUCHER_NOT_APPLICABLE_BRANCH, "Voucher không áp dụng cho chi nhánh hiện tại");
            }
        }

        if (subtotal == null || subtotal.compareTo(voucher.getMinOrderValue()) < 0) {
            DecimalFormat df = new DecimalFormat("#,###");
            String minOrderFormatted = df.format(voucher.getMinOrderValue());
            throw BusinessException.badRequest(ErrorCode.VOUCHER_MIN_ORDER_NOT_MET,
                    "Đơn hàng chưa đạt giá trị tối thiểu " + minOrderFormatted + "đ để áp dụng voucher này");
        }

        if (voucher.getUsageLimitPerCustomer() != null) {
            if (accountId == null) {
                throw BusinessException.badRequest(ErrorCode.VOUCHER_USER_LIMIT_EXCEEDED,
                        "Voucher này yêu cầu đăng nhập tài khoản để áp dụng giới hạn lượt sử dụng theo khách hàng");
            }
            long usedByUser = voucherUsageHistoryRepository.countByVoucherIdAndAccountIdAndStatus(
                    voucher.getId(), accountId, VoucherUsageStatus.DA_DUNG);
            if (usedByUser >= voucher.getUsageLimitPerCustomer()) {
                throw BusinessException.badRequest(ErrorCode.VOUCHER_USER_LIMIT_EXCEEDED,
                        "Bạn đã sử dụng hết số lần quy định cho voucher này (" + voucher.getUsageLimitPerCustomer() + " lần)");
            }
        }
    }

    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal subtotal) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (voucher.getDiscountType() == DiscountType.PERCENT) {
            discount = subtotal.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscountAmount() != null && voucher.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(voucher.getMaxDiscountAmount());
            }
        } else {
            discount = voucher.getDiscountValue();
        }

        return discount.min(subtotal).max(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<VoucherDto.VoucherResponse> getActiveVouchersForBranch(
            Long branchId, BigDecimal subtotal, Long accountId) {

        Instant now = Instant.now();
        List<Voucher> activeVouchers = voucherRepository.findActiveVouchers(now);

        List<VoucherDto.VoucherResponse> result = new ArrayList<>();
        for (Voucher v : activeVouchers) {
            if (!Boolean.TRUE.equals(v.getApplyAllBranches())) {
                if (branchId == null || !voucherBranchRepository.existsByIdVoucherIdAndIdBranchId(v.getId(), branchId)) {
                    continue;
                }
            }

            boolean canApply = true;
            if (subtotal != null && subtotal.compareTo(v.getMinOrderValue()) < 0) {
                canApply = false;
            }
            if (v.getUsageLimitPerCustomer() != null) {
                if (accountId == null) {
                    canApply = false;
                } else {
                    long userUses = voucherUsageHistoryRepository.countByVoucherIdAndAccountIdAndStatus(
                            v.getId(), accountId, VoucherUsageStatus.DA_DUNG);
                    if (userUses >= v.getUsageLimitPerCustomer()) {
                        canApply = false;
                    }
                }
            }

            result.add(mapToVoucherResponse(v, canApply));
        }

        return result;
    }

    @Transactional
    public void applyVoucherToOrder(Order order, Invoice invoice, String voucherCode, Long accountId) {
        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            return;
        }

        String code = voucherCode.trim();
        Long branchId = order.getBranch() != null ? order.getBranch().getId() : null;

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VOUCHER_NOT_FOUND, "Không tìm thấy voucher: " + code));

        // Nếu đơn hàng đã có voucher trước đó và mã mới khác mã cũ, giải phóng voucher cũ
        if (order.getVoucher() != null) {
            if (order.getVoucher().getId().equals(voucher.getId())) {
                log.info("Voucher {} đã được áp dụng cho đơn #{}", code, order.getId());
                return;
            }
            releaseVoucherFromOrder(order, "Đổi voucher mới");
        }

        validateVoucherEligibility(voucher, branchId, invoice.getSubtotal(), accountId);

        // Concurrency control (AT-05): Atomic update prevents race conditions on last remaining slot
        int updatedRows = voucherRepository.incrementUsageCount(voucher.getId());
        if (updatedRows == 0) {
            throw BusinessException.conflict(ErrorCode.VOUCHER_EXHAUSTED, "Voucher đã hết lượt sử dụng do vừa có khách hàng khác sử dụng");
        }

        if (voucher.getUsageLimitPerCustomer() != null && accountId != null) {
            long usedByUser = voucherUsageHistoryRepository.countByVoucherIdAndAccountIdAndStatus(
                    voucher.getId(), accountId, VoucherUsageStatus.DA_DUNG);
            if (usedByUser >= voucher.getUsageLimitPerCustomer()) {
                voucherRepository.decrementUsageCount(voucher.getId());
                throw BusinessException.badRequest(ErrorCode.VOUCHER_USER_LIMIT_EXCEEDED,
                        "Bạn đã sử dụng hết số lần quy định cho voucher này (" + voucher.getUsageLimitPerCustomer() + " lần)");
            }
        }

        BigDecimal discountAmount = calculateDiscount(voucher, invoice.getSubtotal());

        // Respect existing store discount
        BigDecimal storeDiscount = invoice.getDiscountAmount() != null ? invoice.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal remainingAfterStore = invoice.getSubtotal().subtract(storeDiscount).max(BigDecimal.ZERO);
        BigDecimal actualVoucherDiscount = discountAmount.min(remainingAfterStore);

        invoice.setVoucher(voucher);
        invoice.setVoucherCode(voucher.getCode());
        invoice.setVoucherDiscountAmount(actualVoucherDiscount);
        invoice.setTotalAmount(remainingAfterStore.subtract(actualVoucherDiscount).max(BigDecimal.ZERO));
        invoiceRepository.save(invoice);

        order.setVoucher(voucher);
        order.setVoucherCode(voucher.getCode());
        orderRepository.save(order);

        Account customerAccount = order.getAccount();
        if (customerAccount == null && accountId != null) {
            customerAccount = accountRepository.findById(accountId).orElse(null);
        }

        VoucherUsageHistory history = VoucherUsageHistory.builder()
                .voucher(voucher)
                .order(order)
                .invoice(invoice)
                .account(customerAccount)
                .branch(order.getBranch())
                .discountAmount(actualVoucherDiscount)
                .status(VoucherUsageStatus.DA_DUNG)
                .usedAt(Instant.now())
                .build();
        voucherUsageHistoryRepository.save(history);

        log.info("Applied voucher {} to order #{}, discount: {}đ", voucher.getCode(), order.getId(), actualVoucherDiscount);
    }

    @Transactional
    public void releaseVoucherFromOrder(Order order, String reason) {
        if (order == null || order.getId() == null) {
            return;
        }

        var historyOpt = voucherUsageHistoryRepository.findByOrderId(order.getId());
        if (historyOpt.isEmpty()) {
            return;
        }

        VoucherUsageHistory history = historyOpt.get();
        if (history.getStatus() != VoucherUsageStatus.DA_DUNG) {
            return;
        }

        history.setStatus(VoucherUsageStatus.DA_HOAN_TRA);
        history.setRefundedAt(Instant.now());
        history.setRefundReason(reason != null ? reason : "Đơn hàng bị hủy");
        voucherUsageHistoryRepository.save(history);

        voucherRepository.decrementUsageCount(history.getVoucher().getId());
        log.info("Released voucher {} from cancelled order #{}", history.getVoucher().getCode(), order.getId());
    }

    @Transactional(readOnly = true)
    public Page<VoucherDto.VoucherResponse> searchVouchers(String keyword, Boolean isActive, Pageable pageable) {
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return voucherRepository.searchVouchers(kw, isActive, pageable)
                .map(v -> mapToVoucherResponse(v, null));
    }

    @Transactional(readOnly = true)
    public VoucherDto.VoucherResponse getVoucherById(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VOUCHER_NOT_FOUND, "Không tìm thấy voucher #" + id));
        return mapToVoucherResponse(voucher, null);
    }

    @Transactional
    public VoucherDto.VoucherResponse createVoucher(VoucherDto.CreateVoucherRequest request) {
        if (voucherRepository.findByCodeIgnoreCase(request.getCode().trim()).isPresent()) {
            throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED, "Mã voucher " + request.getCode() + " đã tồn tại trong hệ thống");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Ngày kết thúc phải sau ngày bắt đầu");
        }

        Voucher voucher = Voucher.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO)
                .totalUsageLimit(request.getTotalUsageLimit())
                .usedCount(0)
                .usageLimitPerCustomer(request.getUsageLimitPerCustomer() != null ? request.getUsageLimitPerCustomer() : 1)
                .applyAllBranches(request.getApplyAllBranches() != null ? request.getApplyAllBranches() : true)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        voucher = voucherRepository.save(voucher);

        if (!Boolean.TRUE.equals(voucher.getApplyAllBranches()) && request.getBranchIds() != null) {
            for (Long bId : request.getBranchIds()) {
                if (branchRepository.existsById(bId)) {
                    voucherBranchRepository.save(VoucherBranch.builder()
                            .id(new VoucherBranchId(voucher.getId(), bId))
                            .voucher(voucher)
                            .branch(branchRepository.getReferenceById(bId))
                            .build());
                }
            }
        }

        return mapToVoucherResponse(voucher, null);
    }

    @Transactional
    public VoucherDto.VoucherResponse updateVoucher(Long id, VoucherDto.UpdateVoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VOUCHER_NOT_FOUND, "Không tìm thấy voucher #" + id));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Ngày kết thúc phải sau ngày bắt đầu");
        }

        voucher.setName(request.getName().trim());
        voucher.setDescription(request.getDescription());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        voucher.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO);
        voucher.setTotalUsageLimit(request.getTotalUsageLimit());
        voucher.setUsageLimitPerCustomer(request.getUsageLimitPerCustomer() != null ? request.getUsageLimitPerCustomer() : 1);
        voucher.setApplyAllBranches(request.getApplyAllBranches() != null ? request.getApplyAllBranches() : true);
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        voucher.setUpdatedAt(Instant.now());

        voucher = voucherRepository.save(voucher);

        voucherBranchRepository.deleteByVoucherId(voucher.getId());
        if (!Boolean.TRUE.equals(voucher.getApplyAllBranches()) && request.getBranchIds() != null) {
            for (Long bId : request.getBranchIds()) {
                if (branchRepository.existsById(bId)) {
                    voucherBranchRepository.save(VoucherBranch.builder()
                            .id(new VoucherBranchId(voucher.getId(), bId))
                            .voucher(voucher)
                            .branch(branchRepository.getReferenceById(bId))
                            .build());
                }
            }
        }

        return mapToVoucherResponse(voucher, null);
    }

    @Transactional
    public void toggleVoucherStatus(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VOUCHER_NOT_FOUND, "Không tìm thấy voucher #" + id));
        voucher.setIsActive(!Boolean.TRUE.equals(voucher.getIsActive()));
        voucher.setUpdatedAt(Instant.now());
        voucherRepository.save(voucher);
    }

    @Transactional(readOnly = true)
    public Page<VoucherDto.VoucherUsageHistoryResponse> getVoucherUsageHistory(Long voucherId, Pageable pageable) {
        return voucherUsageHistoryRepository.findByVoucherIdOrderByUsedAtDesc(voucherId, pageable)
                .map(this::mapToUsageResponse);
    }

    private VoucherDto.VoucherResponse mapToVoucherResponse(Voucher v, Boolean canApply) {
        List<Long> branchIds = new ArrayList<>();
        if (!Boolean.TRUE.equals(v.getApplyAllBranches())) {
            branchIds = voucherBranchRepository.findByIdVoucherId(v.getId()).stream()
                    .map(vb -> vb.getId().getBranchId())
                    .toList();
        }

        DecimalFormat df = new DecimalFormat("#,###");
        String formattedDiscountText;
        if (v.getDiscountType() == DiscountType.PERCENT) {
            formattedDiscountText = "Giảm " + df.format(v.getDiscountValue()) + "%";
            if (v.getMaxDiscountAmount() != null && v.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                formattedDiscountText += " (tối đa " + df.format(v.getMaxDiscountAmount()) + "đ)";
            }
        } else {
            formattedDiscountText = "Giảm " + df.format(v.getDiscountValue()) + "đ";
        }

        return VoucherDto.VoucherResponse.builder()
                .id(v.getId())
                .code(v.getCode())
                .name(v.getName())
                .description(v.getDescription())
                .discountType(v.getDiscountType())
                .discountValue(v.getDiscountValue())
                .maxDiscountAmount(v.getMaxDiscountAmount())
                .minOrderValue(v.getMinOrderValue())
                .totalUsageLimit(v.getTotalUsageLimit())
                .usedCount(v.getUsedCount())
                .remainingUses(Math.max(0, v.getTotalUsageLimit() - v.getUsedCount()))
                .usageLimitPerCustomer(v.getUsageLimitPerCustomer())
                .applyAllBranches(v.getApplyAllBranches())
                .applicableBranchIds(branchIds)
                .startDate(v.getStartDate())
                .endDate(v.getEndDate())
                .isActive(v.getIsActive())
                .isExpired(v.isExpired())
                .canApply(canApply)
                .formattedDiscountText(formattedDiscountText)
                .build();
    }

    private VoucherDto.VoucherUsageHistoryResponse mapToUsageResponse(VoucherUsageHistory h) {
        return VoucherDto.VoucherUsageHistoryResponse.builder()
                .id(h.getId())
                .voucherId(h.getVoucher() != null ? h.getVoucher().getId() : null)
                .voucherCode(h.getVoucher() != null ? h.getVoucher().getCode() : null)
                .voucherName(h.getVoucher() != null ? h.getVoucher().getName() : null)
                .orderId(h.getOrder() != null ? h.getOrder().getId() : null)
                .invoiceId(h.getInvoice() != null ? h.getInvoice().getId() : null)
                .accountId(h.getAccount() != null ? h.getAccount().getId() : null)
                .customerName(h.getAccount() != null ? h.getAccount().getFullName() : null)
                .customerEmail(h.getAccount() != null ? h.getAccount().getEmail() : null)
                .branchId(h.getBranch() != null ? h.getBranch().getId() : null)
                .branchName(h.getBranch() != null ? h.getBranch().getName() : null)
                .discountAmount(h.getDiscountAmount())
                .status(h.getStatus())
                .usedAt(h.getUsedAt())
                .refundedAt(h.getRefundedAt())
                .refundReason(h.getRefundReason())
                .build();
    }
}
