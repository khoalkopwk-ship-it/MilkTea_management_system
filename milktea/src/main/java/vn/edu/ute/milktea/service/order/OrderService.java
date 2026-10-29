package vn.edu.ute.milktea.service.order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.CartDto;
import vn.edu.ute.milktea.dto.OrderDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.audit.BusinessAudit;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.cancellation.CancellationStatus;
import vn.edu.ute.milktea.entity.order.*;
import vn.edu.ute.milktea.entity.inventory.Material;
import vn.edu.ute.milktea.entity.payment.PaymentNotice;
import vn.edu.ute.milktea.entity.payment.PaymentMethod;
import vn.edu.ute.milktea.entity.recipe.ProductRecipe;
import vn.edu.ute.milktea.entity.settings.GlobalSettings;
import vn.edu.ute.milktea.entity.table.DiningTable;
import vn.edu.ute.milktea.entity.table.TableSession;
import vn.edu.ute.milktea.entity.table.TableSessionStatus;
import vn.edu.ute.milktea.entity.table.TableStatus;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.cancellation.CancellationRequestRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.inventory.MaterialRepository;
import vn.edu.ute.milktea.repository.order.InvoiceRepository;
import vn.edu.ute.milktea.repository.order.OrderIngredientSnapshotRepository;
import vn.edu.ute.milktea.repository.order.OrderItemRepository;
import vn.edu.ute.milktea.repository.order.OrderRepository;
import vn.edu.ute.milktea.repository.payment.PaymentRepository;
import vn.edu.ute.milktea.repository.recipe.ProductRecipeRepository;
import vn.edu.ute.milktea.repository.settings.GlobalSettingsRepository;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;
import vn.edu.ute.milktea.repository.table.TableSessionRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.security.GuestAccessService;
import vn.edu.ute.milktea.service.inventory.InventoryService;
import vn.edu.ute.milktea.entity.table.SessionCart;
import vn.edu.ute.milktea.repository.table.SessionCartRepository;
import vn.edu.ute.milktea.service.table.SessionCartService;
import vn.edu.ute.milktea.service.table.TableSessionService;
import vn.edu.ute.milktea.service.recipe.FixedRecipeService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
@Service
@RequiredArgsConstructor
public class OrderService {
    private final DiningTableRepository tableRepository;
    private final TableSessionRepository sessionRepository;
    private final TableSessionService tableSessionService;
    private final SessionCartService sessionCartService;
    private final SessionCartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductRecipeRepository recipeRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final vn.edu.ute.milktea.repository.order.OrderItemToppingRepository orderItemToppingRepository;
    private final OrderIngredientSnapshotRepository snapshotRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final vn.edu.ute.milktea.repository.payment.PaymentNoticeRepository paymentNoticeRepository;
    private final vn.edu.ute.milktea.repository.cancellation.RefundRequestRepository refundRepository;
    private final CancellationRequestRepository cancellationRepository;
    private final GlobalSettingsRepository settingsRepository;
    private final AccountRepository accountRepository;
    private final BusinessAuditRepository auditRepository;
    private final GuestAccessService guestAccessService;
    private final InventoryService inventoryService;
    private final FixedRecipeService fixedRecipeService;
    private final MaterialRepository materialRepository;
    private final vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher realtimeEventPublisher;
    private final vn.edu.ute.milktea.repository.branch.BranchRepository branchRepository;
    private final vn.edu.ute.milktea.repository.branch.BranchProductRepository branchProductRepository;
    private final vn.edu.ute.milktea.service.common.IdempotencyService idempotencyService;
    private final vn.edu.ute.milktea.service.voucher.VoucherService voucherService;

    private final vn.edu.ute.milktea.repository.branch.StaffBranchRepository staffBranchRepository;

    @Transactional
    public OrderDto.OrderResponse createTableOrder(OrderDto.CreateTableOrderRequest request, CurrentActor actor, String idempotencyKey) {
        DiningTable table = tableRepository.findByIdWithLock(request.getTableId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_NOT_FOUND, "Không tìm thấy bàn"));
        if (!Boolean.TRUE.equals(table.getActive())) {
            throw BusinessException.badRequest(ErrorCode.TABLE_NOT_FOUND, "Bàn đang ngừng hoạt động");
        }
        if (table.getBranch() != null && "KHOTONG".equalsIgnoreCase(table.getBranch().getCode())) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Kho tổng không bán lẻ trực tiếp");
        }

        boolean isStaffOfBranch = actor != null && (actor.hasRole(Role.CASHIER) || actor.hasRole(Role.ADMIN) || actor.hasRole(Role.MANAGER))
                && actor.canAccessBranch(table.getBranch() != null ? table.getBranch().getId() : null);

        // V4-01 & Yêu cầu 5: Kiểm tra quyền bắt buộc phải có QR capability token, staff hoặc session token hợp lệ
        boolean hasValidQr = request.getTableToken() != null && guestAccessService.validateTableQrCapabilityToken(request.getTableToken(), table.getId());
        boolean hasValidSession = false;
        if (table.getActiveSessionId() != null) {
            TableSession activeSession = sessionRepository.findById(table.getActiveSessionId()).orElse(null);
            if (activeSession != null && activeSession.getStatus() == TableSessionStatus.OPEN) {
                boolean hasSessionToken = request.getTableToken() != null && guestAccessService.validateTableToken(request.getTableToken())
                        .map(access -> access.getSession() != null && activeSession.getId().equals(access.getSession().getId()))
                        .orElse(false);
                boolean isSessionGuest = actor != null && actor.getSessionId() != null && actor.getSessionId().equals(activeSession.getId());
                hasValidSession = hasSessionToken || isSessionGuest;
            }
        }

        if (!isStaffOfBranch && !hasValidQr && !hasValidSession) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Cần quét mã QR hợp lệ của bàn để đặt món");
        }

        // V4-12 & Mục 4.2: Kiểm tra Idempotency replay TRƯỚC KHI kiểm tra phiên bị đổi (khi client retry cùng request)
        String callerScope = resolveCallerScope(actor, table.getId());
        String payloadJson = buildCanonicalTablePayload(request);
        String fingerprint = idempotencyService.computeFingerprint(payloadJson, callerScope);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existingOrderId = idempotencyService.checkExisting("CREATE_ORDER", idempotencyKey, fingerprint);
            if (existingOrderId.isPresent()) {
                Order order = orderRepository.findById(existingOrderId.get()).orElse(null);
                if (order != null) {
                    // Kiểm tra thẩm quyền truy cập đối với tài nguyên cũ (Mục 4.2)
                    boolean isStaffAuthorized = isStaffOfBranch;
                    boolean isUserAuthorized = actor != null && actor.isAuthenticated() && order.getAccount() != null
                            && order.getAccount().getId().equals(actor.getAccountId());
                    boolean isGuestSessionAuthorized = order.getSession() != null && actor != null && actor.getSessionId() != null
                            && actor.getSessionId().equals(order.getSession().getId());
                    boolean isGuestTableSessionAuthorized = order.getSession() != null && table.getActiveSessionId() != null
                            && order.getSession().getId().equals(table.getActiveSessionId())
                            && order.getSession().getTable() != null
                            && order.getSession().getTable().getId().equals(table.getId());

                    if (!isStaffAuthorized && !isUserAuthorized && !isGuestSessionAuthorized && !isGuestTableSessionAuthorized) {
                        throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Không có quyền truy cập đơn hàng của phiên này");
                    }

                    Invoice invoice = invoiceRepository.findByOrderId(order.getId()).orElse(null);
                    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                    return mapToOrderResponse(order, invoice, items, null, null);
                }
            }
        }

        // Nếu không phải idempotency replay, kiểm tra logic mở phiên hoặc đặt thêm món vào phiên
        if (table.getActiveSessionId() == null) {
            if (!isStaffOfBranch && !hasValidQr) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Cần quét mã QR hợp lệ của bàn để đặt món");
            }
        } else {
            // Đã có phiên: kiểm tra expectedSessionId và quyền đặt thêm món
            if (request.getExpectedSessionId() == null) {
                throw BusinessException.conflict(ErrorCode.TABLE_SESSION_CHANGED, "Bàn đã có khách đang phục vụ. Vui lòng tải lại.");
            }
            if (!table.getActiveSessionId().equals(request.getExpectedSessionId())) {
                throw BusinessException.conflict(ErrorCode.TABLE_SESSION_CHANGED, "Phiên bàn đã thay đổi");
            }
            if (!isStaffOfBranch && !hasValidSession) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Bạn không có quyền đặt thêm món vào phiên bàn này");
            }
        }

        // Mở phiên mới hoặc lấy phiên hiện có
        TableSession session;
        String rawGuestTableToken = null;
        if (table.getActiveSessionId() == null) {
            if (request.getExpectedSessionId() != null) {
                throw BusinessException.conflict(ErrorCode.TABLE_SESSION_CHANGED, "Bàn chưa có phiên hoặc phiên đã kết thúc");
            }
            Account opener = (actor != null && actor.isAuthenticated()) ?
                    accountRepository.findById(actor.getAccountId()).orElse(null) : null;
            session = TableSession.builder()
                    .table(table)
                    .status(TableSessionStatus.OPEN)
                    .openedAt(Instant.now())
                    .openedBy(opener)
                    .version(0L)
                    .build();
            session = sessionRepository.save(session);
            table.setStatus(TableStatus.CO_KHACH);
            table.setActiveSessionId(session.getId());
            tableRepository.save(table);
            SessionCart cart = SessionCart.builder()
                    .session(session)
                    .version(1L)
                    .modifiedAt(Instant.now())
                    .build();
            cartRepository.save(cart);
            var issued = guestAccessService.issueTableSessionToken(session, 12);
            rawGuestTableToken = issued.rawToken();
        } else {
            session = sessionRepository.findById(table.getActiveSessionId())
                    .orElseThrow(() -> BusinessException.conflict(ErrorCode.TABLE_SESSION_CHANGED, "Không tìm thấy phiên"));
        }
        // Tạo đơn hàng và tính giá snapshot
        Account customerAccount = (actor != null && actor.isAuthenticated()) ?
                accountRepository.findById(actor.getAccountId()).orElse(null) : null;
        Order order = Order.builder()
                .branch(table.getBranch())
                .session(session)
                .account(customerAccount)
                .source(OrderSource.TABLE)
                .status(actor != null && (actor.hasRole(Role.CASHIER) || actor.hasRole(Role.ADMIN) || actor.hasRole(Role.MANAGER))
                        ? OrderStatus.CHO_THANH_TOAN : OrderStatus.CHO_XAC_NHAN)
                .note(request.getNote())
                .idempotencyKey(idempotencyKey)
                .createdAt(Instant.now())
                .version(0L)
                .build();
        order = orderRepository.save(order);
        List<OrderItem> savedItems = createOrderItemsAndSnapshots(order, request.getItems());
        // Tính toán tổng tiền và giảm giá từ GlobalSettings
        BigDecimal subtotal = calculateOrderSubtotal(savedItems);
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        CalculatedDiscount discount = computeDiscount(subtotal, settings);
        Invoice invoice = Invoice.builder()
                .order(order)
                .subtotal(subtotal)
                .discountAmount(discount.discountAmount)
                .totalAmount(discount.totalAmount)
                .snapshotDiscountPercent(discount.snapshotPercent)
                .snapshotDiscountType(discount.discountType)
                .snapshotDiscountValue(discount.configValue)
                .status(InvoiceStatus.HIEU_LUC)
                .createdAt(Instant.now())
                .build();
        invoice = invoiceRepository.save(invoice);
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            voucherService.applyVoucherToOrder(order, invoice, request.getVoucherCode(), customerAccount != null ? customerAccount.getId() : null);
        }
        // Cập nhật phần còn lại của giỏ hàng trên server
        sessionCartService.replaceCart(session.getId(),
                new CartDto.ReplaceCartRequest(request.getRemainingCartItems() != null ? request.getRemainingCartItems() : Collections.emptyList()),
                null);
        auditRepository.save(BusinessAudit.builder()
                .sessionId(session.getId())
                .orderId(order.getId())
                .accountId(customerAccount != null ? customerAccount.getId() : null)
                .action("ORDER_CREATED")
                .afterState(order.getStatus().name())
                .createdAt(Instant.now())
                .build());
        if (table.getBranch() == null) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Bàn chưa được liên kết với chi nhánh nào");
        }
        Long branchId = table.getBranch().getId();
        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "ORDER_CREATED", order.getId().toString(), session.getId().toString(), "1", Map.of("orderId", order.getId(), "status", order.getStatus().name(), "branchId", branchId));
        realtimeEventPublisher.publishAfterCommit("/topic/branches/" + branchId + "/cashier", "ORDER_CREATED", order.getId().toString(), session.getId().toString(), "1", Map.of("orderId", order.getId(), "status", order.getStatus().name(), "branchId", branchId));
        realtimeEventPublisher.publishAfterCommit("/topic/table-sessions/" + session.getId(), "ORDER_CREATED", order.getId().toString(), session.getId().toString(), "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.record("CREATE_ORDER", idempotencyKey, fingerprint, "ORDER", order.getId(), 24);
        }
        return mapToOrderResponse(order, invoice, savedItems, rawGuestTableToken, null);
    }

    @Transactional
    public OrderDto.OrderResponse createCounterOrder(OrderDto.CreateCounterOrderRequest request, CurrentActor actor, String idempotencyKey) {
        if (actor == null || (!actor.hasRole(Role.CASHIER) && !actor.hasRole(Role.ADMIN) && !actor.hasRole(Role.MANAGER))) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Chỉ nhân viên thu ngân/quản lý mới được tạo đơn tại quầy");
        }
        Long targetBranchId;
        if (actor.hasRole(Role.ADMIN)) {
            targetBranchId = request.getBranchId() != null ? request.getBranchId() : actor.getBranchId();
        } else {
            // Đối với Thu ngân / Quản lý: lấy chi nhánh từ phân công nhân viên, không tin tưởng tham số client
            targetBranchId = actor.getBranchId();
        }
        if (targetBranchId == null) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Nhân viên chưa được phân công chi nhánh hoạt động");
        }
        if (!actor.canAccessBranch(targetBranchId)) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Bạn không có quyền tạo đơn tại chi nhánh này");
        }

        String callerScope = "USER:" + (actor.getAccountId() != null ? actor.getAccountId() : 0L);
        String payloadJson = buildCanonicalCounterPayload(request, targetBranchId);
        String fingerprint = idempotencyService.computeFingerprint(payloadJson, callerScope);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existingOrderId = idempotencyService.checkExisting("CREATE_ORDER", idempotencyKey, fingerprint);
            if (existingOrderId.isPresent()) {
                Order order = orderRepository.findById(existingOrderId.get()).orElse(null);
                if (order != null) {
                    if (!actor.canAccessBranch(order.getBranch().getId())) {
                        throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Không có quyền truy cập đơn hàng của chi nhánh khác");
                    }
                    Invoice invoice = invoiceRepository.findByOrderId(order.getId()).orElse(null);
                    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                    return mapToOrderResponse(order, invoice, items, null, null);
                }
            }
        }

        var branch = branchRepository.findById(targetBranchId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy chi nhánh"));
        if (!branch.isActive()) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Chi nhánh đang tạm ngưng hoạt động");
        }
        if ("KHOTONG".equalsIgnoreCase(branch.getCode())) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Kho tổng không bán lẻ trực tiếp");
        }

        Account creator = (actor.isAuthenticated()) ?
                accountRepository.findById(actor.getAccountId()).orElse(null) : null;
        Order order = Order.builder()
                .branch(branch)
                .session(null)
                .account(creator)
                .source(OrderSource.COUNTER)
                .status(OrderStatus.CHO_THANH_TOAN)
                .note(request.getNote())
                .idempotencyKey(idempotencyKey)
                .createdAt(Instant.now())
                .version(0L)
                .build();
        order = orderRepository.save(order);
        List<OrderItem> savedItems = createOrderItemsAndSnapshots(order, request.getItems());
        BigDecimal subtotal = calculateOrderSubtotal(savedItems);
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        CalculatedDiscount discount = computeDiscount(subtotal, settings);
        Invoice invoice = Invoice.builder()
                .order(order)
                .subtotal(subtotal)
                .discountAmount(discount.discountAmount)
                .totalAmount(discount.totalAmount)
                .snapshotDiscountPercent(discount.snapshotPercent)
                .snapshotDiscountType(discount.discountType)
                .snapshotDiscountValue(discount.configValue)
                .status(InvoiceStatus.HIEU_LUC)
                .createdAt(Instant.now())
                .build();
        invoice = invoiceRepository.save(invoice);
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            voucherService.applyVoucherToOrder(order, invoice, request.getVoucherCode(), creator != null ? creator.getId() : null);
        }
        var issued = guestAccessService.issueCounterOrderToken(order, 24);
        auditRepository.save(BusinessAudit.builder()
                .orderId(order.getId())
                .accountId(creator != null ? creator.getId() : null)
                .action("COUNTER_ORDER_CREATED")
                .afterState(order.getStatus().name())
                .createdAt(Instant.now())
                .build());
        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "ORDER_CREATED", order.getId().toString(), null, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name(), "branchId", branch.getId()));
        realtimeEventPublisher.publishAfterCommit("/topic/branches/" + branch.getId() + "/cashier", "ORDER_CREATED", order.getId().toString(), null, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name(), "branchId", branch.getId()));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.record("CREATE_ORDER", idempotencyKey, fingerprint, "ORDER", order.getId(), 24);
        }
        return mapToOrderResponse(order, invoice, savedItems, null, issued.rawToken());
    }

    @Transactional
    public void assignCounterOrderToTable(Long orderId, Long tableId, Long cashierId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));
        if (order.getSource() != OrderSource.COUNTER) {
            throw BusinessException.badRequest(ErrorCode.ORDER_STATE_CONFLICT, "Chỉ được gắn đơn hàng tại quầy vào bàn");
        }
        if (order.getSession() != null) {
            throw BusinessException.badRequest(ErrorCode.ORDER_STATE_CONFLICT, "Đơn hàng đã được gắn vào phiên bàn khác");
        }
        DiningTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_NOT_FOUND, "Không tìm thấy bàn"));
        if (order.getBranch() != null && table.getBranch() != null && !order.getBranch().getId().equals(table.getBranch().getId())) {
            throw BusinessException.badRequest(ErrorCode.ORDER_STATE_CONFLICT, "Đơn hàng và bàn không cùng một chi nhánh");
        }
        // R06: Kiểm tra quyền chi nhánh của nhân viên thu ngân gọi gán đơn
        if (cashierId != null) {
            Account cashier = accountRepository.findById(cashierId)
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy nhân viên thu ngân"));
            Long targetBranchId = order.getBranch() != null ? order.getBranch().getId() : (table.getBranch() != null ? table.getBranch().getId() : null);
            if (cashier.getRole() != Role.ADMIN && targetBranchId != null) {
                boolean hasActive = staffBranchRepository != null && staffBranchRepository
                        .findByAccountIdAndBranchIdAndStatus(cashier.getId(), targetBranchId, "HOAT_DONG")
                        .isPresent();
                if (!hasActive && cashier.getBranch() != null && cashier.getBranch().getId().equals(targetBranchId)) {
                    hasActive = true;
                }
                if (!hasActive) {
                    throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Thu ngân không thuộc chi nhánh của đơn hàng và bàn");
                }
            }
        }

        // Mở phiên nếu bàn chưa có phiên, hoặc lấy phiên hiện tại
        TableSession session = tableSessionService.openByCashier(tableId, cashierId);
        order.setSession(session);
        orderRepository.save(order);
        auditRepository.save(BusinessAudit.builder()
                .sessionId(session.getId())
                .orderId(order.getId())
                .accountId(cashierId)
                .action("ORDER_ASSIGNED_TO_TABLE")
                .reason("Gắn đơn quầy vào bàn #" + tableId)
                .createdAt(Instant.now())
                .build());
        Long branchId = order.getBranch() != null ? order.getBranch().getId() : (table.getBranch() != null ? table.getBranch().getId() : null);
        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "ORDER_ASSIGNED_TO_TABLE", order.getId().toString(), session.getId().toString(), "1", Map.of("orderId", order.getId()));
        if (branchId != null) {
            realtimeEventPublisher.publishAfterCommit("/topic/branches/" + branchId + "/cashier", "ORDER_ASSIGNED_TO_TABLE", order.getId().toString(), session.getId().toString(), "1", Map.of("orderId", order.getId()));
        }
        realtimeEventPublisher.publishAfterCommit("/topic/table-sessions/" + session.getId(), "ORDER_ASSIGNED_TO_TABLE", order.getId().toString(), session.getId().toString(), "1", Map.of("orderId", order.getId()));
    }

    @Transactional
    public void confirm(Long orderId, CurrentActor actor) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));
        if (actor != null) {
            if (!actor.hasRole(Role.CASHIER) && !actor.hasRole(Role.ADMIN) && !actor.hasRole(Role.MANAGER)) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Không có quyền xác nhận đơn hàng");
            }
            if (order.getBranch() != null && !actor.canAccessBranch(order.getBranch().getId())) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Không thể xác nhận đơn hàng của chi nhánh khác");
            }
        }
        Long cashierId = actor != null ? actor.getAccountId() : null;
        confirmInternal(order, cashierId);
    }

    @Transactional
    public void confirm(Long orderId, Long cashierId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));
        confirmInternal(order, cashierId);
    }

    private void confirmInternal(Order order, Long cashierId) {
        if (order.getStatus() == OrderStatus.CHO_CHE_BIEN) {
            return;
        }
        if (order.getStatus() != OrderStatus.CHO_XAC_NHAN && order.getStatus() != OrderStatus.CHO_THANH_TOAN) {
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT,
                    "Không thể xác nhận đơn ở trạng thái: " + order.getStatus());
        }
        String oldState = order.getStatus().name();
        order.setStatus(OrderStatus.CHO_CHE_BIEN);
        Invoice invoice = invoiceRepository.findByOrderId(order.getId())
                .orElseThrow(() -> BusinessException.notFound(
                        ErrorCode.ORDER_STATE_CONFLICT,
                        "Không tìm thấy hóa đơn của đơn hàng"));
        // Không áp dụng paid gate: Hỗ trợ tiền mặt hoặc chuyển khoản trước/trong/sau pha chế theo GEMINI.md
        orderRepository.save(order);
        auditRepository.save(BusinessAudit.builder()
                .orderId(order.getId())
                .accountId(cashierId)
                .action("ORDER_CONFIRMED")
                .beforeState(oldState)
                .afterState("CHO_CHE_BIEN")
                .createdAt(Instant.now())
                .build());
        String sId = order.getSession() != null ? order.getSession().getId().toString() : null;
        Long bId = order.getBranch() != null ? order.getBranch().getId() : null;
        realtimeEventPublisher.publishAfterCommit("/topic/kitchen", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        if (bId != null) {
            realtimeEventPublisher.publishAfterCommit("/topic/branches/" + bId + "/kitchen", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name(), "branchId", bId));
            realtimeEventPublisher.publishAfterCommit("/topic/branches/" + bId + "/cashier", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name(), "branchId", bId));
        }
        if (order.getSession() != null) {
            realtimeEventPublisher.publishAfterCommit("/topic/table-sessions/" + order.getSession().getId(), "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        }
    }

    @Transactional
    public void start(Long orderId, CurrentActor actor) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));
        if (actor != null) {
            if (!actor.hasRole(Role.KITCHEN) && !actor.hasRole(Role.ADMIN) && !actor.hasRole(Role.MANAGER)) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Không có quyền bắt đầu chế biến đơn hàng");
            }
            if (order.getBranch() != null && !actor.canAccessBranch(order.getBranch().getId())) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Không thể chế biến đơn hàng của chi nhánh khác");
            }
        }
        Long kitchenId = actor != null ? actor.getAccountId() : null;
        startInternal(order, kitchenId);
    }

    @Transactional
    public void start(Long orderId, Long kitchenId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));
        startInternal(order, kitchenId);
    }

    private void startInternal(Order order, Long kitchenId) {
        if (order.getStatus() == OrderStatus.DANG_CHE_BIEN) {
            return;
        }
        if (order.getStatus() != OrderStatus.CHO_CHE_BIEN) {
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT,
                    "Không thể bắt đầu chế biến đơn ở trạng thái: " + order.getStatus());
        }
        Invoice invoice = invoiceRepository.findByOrderId(order.getId())
                .orElseThrow(() -> BusinessException.notFound(
                        ErrorCode.ORDER_STATE_CONFLICT,
                        "Không tìm thấy hóa đơn của đơn hàng"));
        // Không áp dụng paid gate: Bếp có thể chế biến theo lệnh điều phối của quán
        // Kiểm tra không có yêu cầu hủy đang chờ xử lý
        var pendingCancel = cancellationRepository.findFirstByOrderIdAndStatus(order.getId(),
                vn.edu.ute.milktea.entity.cancellation.CancellationStatus.CHO);
        if (pendingCancel.isPresent()) {
            throw BusinessException.conflict(ErrorCode.CANCELLATION_PENDING,
                    "Đơn hàng đang có yêu cầu hủy chờ xử lý từ khách hàng. Không thể bắt đầu.");
        }
        order.setStatus(OrderStatus.DANG_CHE_BIEN);
        order.setStartedAt(Instant.now());
        orderRepository.save(order);
        auditRepository.save(BusinessAudit.builder()
                .orderId(order.getId())
                .accountId(kitchenId)
                .action("ORDER_STARTED")
                .beforeState("CHO_CHE_BIEN")
                .afterState("DANG_CHE_BIEN")
                .createdAt(Instant.now())
                .build());
        String sId = order.getSession() != null ? order.getSession().getId().toString() : null;
        Long bId = order.getBranch() != null ? order.getBranch().getId() : null;
        realtimeEventPublisher.publishAfterCommit("/topic/kitchen", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        if (bId != null) {
            realtimeEventPublisher.publishAfterCommit("/topic/branches/" + bId + "/kitchen", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name(), "branchId", bId));
            realtimeEventPublisher.publishAfterCommit("/topic/branches/" + bId + "/cashier", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name(), "branchId", bId));
        }
        if (order.getSession() != null) {
            realtimeEventPublisher.publishAfterCommit("/topic/table-sessions/" + order.getSession().getId(), "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        }
    }

    @Transactional
    public void complete(Long orderId, CurrentActor actor) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));
        if (actor != null) {
            if (!actor.hasRole(Role.KITCHEN) && !actor.hasRole(Role.ADMIN) && !actor.hasRole(Role.MANAGER)) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Không có quyền hoàn thành chế biến đơn hàng");
            }
            if (order.getBranch() != null && !actor.canAccessBranch(order.getBranch().getId())) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Không thể hoàn thành đơn hàng của chi nhánh khác");
            }
        }
        Long kitchenId = actor != null ? actor.getAccountId() : null;
        completeInternal(order, kitchenId);
    }

    @Transactional
    public void complete(Long orderId, Long kitchenId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));
        completeInternal(order, kitchenId);
    }

    private void completeInternal(Order order, Long kitchenId) {
        if (order.getStatus() == OrderStatus.HOAN_THANH) {
            return;
        }
        if (order.getStatus() != OrderStatus.DANG_CHE_BIEN) {
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT,
                    "Chỉ có thể hoàn thành đơn đang ở trạng thái DANG_CHE_BIEN. Trạng thái hiện tại: " + order.getStatus());
        }
        Account kitchen = kitchenId != null ? accountRepository.findById(kitchenId).orElse(null) : null;
        // Tiêu hao kho BEP theo công thức đang lưu, nhân số ly và lưu snapshot thực tế.
        // Nếu không đủ tồn kho, ném BusinessException.conflict và không đổi trạng thái đơn
        inventoryService.completeOrderConsumption(order, kitchen);
        // Chuyển trạng thái đơn sang HOAN_THANH khi tiêu hao kho thành công
        order.setStatus(OrderStatus.HOAN_THANH);
        order.setCompletedAt(Instant.now());
        orderRepository.save(order);
        auditRepository.save(BusinessAudit.builder()
                .orderId(order.getId())
                .accountId(kitchenId)
                .action("ORDER_COMPLETED")
                .beforeState("DANG_CHE_BIEN")
                .afterState("HOAN_THANH")
                .createdAt(Instant.now())
                .build());
        String sId = order.getSession() != null ? order.getSession().getId().toString() : null;
        Long bId = order.getBranch() != null ? order.getBranch().getId() : null;
        realtimeEventPublisher.publishAfterCommit("/topic/kitchen", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        if (bId != null) {
            realtimeEventPublisher.publishAfterCommit("/topic/branches/" + bId + "/kitchen", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name(), "branchId", bId));
            realtimeEventPublisher.publishAfterCommit("/topic/branches/" + bId + "/cashier", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name(), "branchId", bId));
            realtimeEventPublisher.publishAfterCommit("/topic/branches/" + bId + "/stock", "STOCK_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "branchId", bId));
        }
        if (order.getSession() != null) {
            realtimeEventPublisher.publishAfterCommit("/topic/table-sessions/" + order.getSession().getId(), "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        }
        realtimeEventPublisher.publishAfterCommit("/topic/stock", "STOCK_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId()));
    }

    @Transactional(readOnly = true)
    public OrderDto.OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));
        Invoice invoice = invoiceRepository.findByOrderId(order.getId()).orElse(null);
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        return mapToOrderResponse(order, invoice, items, null, null);
    }

    private List<OrderItem> createOrderItemsAndSnapshots(Order order, List<CartDto.CartItemRequest> itemRequests) {
        List<OrderItem> orderItems = new ArrayList<>();
        Map<Long, BigDecimal> aggregatedIngredients = new HashMap<>();
        Map<Long, Material> materialSampleMap = new HashMap<>();
        List<OrderItemTopping> allToppings = new ArrayList<>();

        for (var req : itemRequests) {
            Product product = productRepository.findById(req.getProductId())
                    .orElseThrow(() -> BusinessException.badRequest(ErrorCode.PRODUCT_UNAVAILABLE, "Món không tồn tại: ID " + req.getProductId()));
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw BusinessException.badRequest(ErrorCode.PRODUCT_UNAVAILABLE, "Món đã ngừng bán: " + product.getName());
            }
            if (order.getBranch() != null) {
                var bp = branchProductRepository.findByBranchIdAndProductId(order.getBranch().getId(), product.getId());
                if (bp.isEmpty() || Boolean.FALSE.equals(bp.get().getActive()) || Boolean.FALSE.equals(bp.get().getAvailable())) {
                    throw BusinessException.badRequest(ErrorCode.PRODUCT_UNAVAILABLE, "Món \"" + product.getName() + "\" hiện không phục vụ tại chi nhánh này");
                }
            }
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .snapshotName(product.getName())
                    .snapshotSize(product.getSize())
                    .snapshotUnitPrice(product.getPrice())
                    .quantity(req.getQuantity())
                    .note(req.getNote())
                    .toppings(new ArrayList<>())
                    .build();

            if (req.getToppingIds() != null && !req.getToppingIds().isEmpty()) {
                if (product.getCategory() != null && !Boolean.TRUE.equals(product.getCategory().getAllowTopping())) {
                    throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED,
                            "Cấu hình món/danh mục đã thay đổi: Danh mục \"" + product.getCategory().getName() + "\" không còn cho phép chọn topping đi kèm. Vui lòng cập nhật lại giỏ hàng.");
                }
                for (Long tId : req.getToppingIds()) {
                    Product toppingProd = productRepository.findById(tId)
                            .orElseThrow(() -> BusinessException.badRequest(ErrorCode.PRODUCT_UNAVAILABLE, "Topping không tồn tại: ID " + tId));
                    if (!Boolean.TRUE.equals(toppingProd.getActive())) {
                        throw BusinessException.badRequest(ErrorCode.PRODUCT_UNAVAILABLE, "Topping đã ngừng bán: " + toppingProd.getName());
                    }
                    if (toppingProd.getCategory() == null || (!toppingProd.getCategory().getName().toLowerCase().contains("topping") && Boolean.TRUE.equals(toppingProd.getCategory().getAllowTopping()))) {
                        throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Sản phẩm \"" + toppingProd.getName() + "\" không phải là topping hợp lệ");
                    }
                    if (order.getBranch() != null) {
                        var tbp = branchProductRepository.findByBranchIdAndProductId(order.getBranch().getId(), toppingProd.getId());
                        if (tbp.isEmpty() || Boolean.FALSE.equals(tbp.get().getActive()) || Boolean.FALSE.equals(tbp.get().getAvailable())) {
                            throw BusinessException.badRequest(ErrorCode.PRODUCT_UNAVAILABLE, "Topping \"" + toppingProd.getName() + "\" hiện không phục vụ tại chi nhánh này");
                        }
                    }
                    OrderItemTopping oit = OrderItemTopping.builder()
                            .orderItem(item)
                            .toppingProduct(toppingProd)
                            .snapshotName(toppingProd.getName())
                            .snapshotUnitPrice(toppingProd.getPrice())
                            .build();
                    item.getToppings().add(oit);
                    allToppings.add(oit);

                    List<ProductRecipe> tRecipes = recipeRepository.findByIdProductId(toppingProd.getId());
                    if (tRecipes.isEmpty()) {
                        throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED,
                                "Không tìm thấy công thức định lượng trong hệ thống cho topping: " + toppingProd.getName());
                    }
                    for (ProductRecipe tr : tRecipes) {
                        BigDecimal normQuantity = tr.getQuantity();
                        if (normQuantity == null || normQuantity.signum() <= 0) {
                            throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED,
                                    "Định lượng không hợp lệ trong công thức topping: " + toppingProd.getName() + " - " + tr.getMaterial().getName());
                        }
                        Long matId = tr.getMaterial().getId();
                        BigDecimal needed = normQuantity.multiply(BigDecimal.valueOf(req.getQuantity()));
                        aggregatedIngredients.merge(matId, needed, BigDecimal::add);
                        materialSampleMap.putIfAbsent(matId, tr.getMaterial());
                    }
                }
            }
            orderItems.add(item);

            List<ProductRecipe> recipes = recipeRepository.findByIdProductId(product.getId());
            if (recipes.isEmpty()) {
                throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED,
                        "Không tìm thấy công thức định lượng trong hệ thống cho món: " + product.getName());
            }
            Set<String> matched = new HashSet<>();
            for (ProductRecipe recipe : recipes) {
                String materialName = recipe.getMaterial().getName();
                if (!matched.add(materialName)) {
                    continue;
                }
                BigDecimal normQuantity = recipe.getQuantity();
                if (normQuantity == null || normQuantity.signum() <= 0) {
                    throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED,
                            "Định lượng không hợp lệ trong công thức món: " + product.getName() + " - " + materialName);
                }
                Long matId = recipe.getMaterial().getId();
                BigDecimal needed = normQuantity.multiply(BigDecimal.valueOf(req.getQuantity()));
                aggregatedIngredients.merge(matId, needed, BigDecimal::add);
                materialSampleMap.putIfAbsent(matId, recipe.getMaterial());
            }
        }
        if (aggregatedIngredients.isEmpty()) {
            throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED, "Đơn hàng không có định lượng nguyên liệu hợp lệ");
        }
        orderItems = orderItemRepository.saveAll(orderItems);
        if (!allToppings.isEmpty()) {
            orderItemToppingRepository.saveAll(allToppings);
        }
        // Lưu OrderIngredientSnapshot cho toàn đơn
        List<OrderIngredientSnapshot> snapshots = new ArrayList<>();
        for (var entry : aggregatedIngredients.entrySet()) {
            Long matId = entry.getKey();
            BigDecimal totalQty = entry.getValue();
            Material sample = materialSampleMap.get(matId);
            if (sample != null) {
                OrderIngredientSnapshot snap = OrderIngredientSnapshot.builder()
                        .id(new OrderIngredientSnapshotId(order.getId(), matId))
                        .order(order)
                        .material(sample)
                        .totalQuantity(totalQty)
                        .snapshotType(sample.getType())
                        .snapshotUnit(sample.getUnit())
                        .build();
                snapshots.add(snap);
            }
        }
        snapshotRepository.saveAll(snapshots);
        return orderItems;
    }

    private BigDecimal calculateOrderSubtotal(List<OrderItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem it : items) {
            BigDecimal toppingSum = BigDecimal.ZERO;
            if (it.getToppings() != null && !it.getToppings().isEmpty()) {
                for (OrderItemTopping t : it.getToppings()) {
                    if (t.getSnapshotUnitPrice() != null) {
                        toppingSum = toppingSum.add(t.getSnapshotUnitPrice());
                    }
                }
            }
            BigDecimal unitPriceWithTopping = it.getSnapshotUnitPrice().add(toppingSum);
            BigDecimal lineTotal = unitPriceWithTopping.multiply(BigDecimal.valueOf(it.getQuantity()));
            subtotal = subtotal.add(lineTotal);
        }
        return subtotal;
    }

    private OrderDto.OrderResponse mapToOrderResponse(
            Order order, Invoice invoice, List<OrderItem> items, String tableToken, String counterToken) {
        boolean isPaid = invoice != null && paymentRepository.existsByInvoiceId(invoice.getId());
        var payment = invoice != null ? paymentRepository.findByInvoiceId(invoice.getId()).orElse(null) : null;
        Account customer = order.getAccount();
        if (customer != null && customer.getRole() != vn.edu.ute.milktea.entity.account.Role.CUSTOMER) customer = null;
        PaymentNotice latestNotice = invoice != null && !isPaid
                ? paymentNoticeRepository.findFirstByInvoiceIdOrderByCreatedAtDescIdDesc(invoice.getId()).orElse(null)
                : null;
        boolean transferNotified = latestNotice != null
                && latestNotice.getMethod() == PaymentMethod.BANK_TRANSFER
                && latestNotice.getReference() != null
                && (latestNotice.getReference().startsWith("NOTICE_")
                    || latestNotice.getReference().startsWith("TRANSFERRED_")
                    || latestNotice.getReference().startsWith("CUSTOMER_TRANSFERRED_"));
        List<OrderDto.OrderItemResponse> itemResponses = items.stream().map(it -> {
            List<CartDto.ToppingItemResponse> toppingsList = new ArrayList<>();
            BigDecimal toppingSum = BigDecimal.ZERO;
            List<OrderItemTopping> itemToppings = it.getToppings();
            if (itemToppings == null || itemToppings.isEmpty()) {
                itemToppings = orderItemToppingRepository.findByOrderItemId(it.getId());
            }
            if (itemToppings != null) {
                for (var t : itemToppings) {
                    toppingsList.add(CartDto.ToppingItemResponse.builder()
                            .id(t.getToppingProduct() != null ? t.getToppingProduct().getId() : null)
                            .productId(t.getToppingProduct() != null ? t.getToppingProduct().getId() : null)
                            .name(t.getSnapshotName())
                            .price(t.getSnapshotUnitPrice())
                            .build());
                    if (t.getSnapshotUnitPrice() != null) {
                        toppingSum = toppingSum.add(t.getSnapshotUnitPrice());
                    }
                }
            }
            BigDecimal unitPriceWithTopping = it.getSnapshotUnitPrice().add(toppingSum);
            BigDecimal lineTotal = unitPriceWithTopping.multiply(BigDecimal.valueOf(it.getQuantity()));
            return OrderDto.OrderItemResponse.builder()
                    .id(it.getId())
                    .productId(it.getProduct().getId())
                    .productName(it.getSnapshotName())
                    .size(it.getSnapshotSize())
                    .quantity(it.getQuantity())
                    .unitPrice(it.getSnapshotUnitPrice())
                    .lineTotal(lineTotal)
                    .note(it.getNote())
                    .toppings(toppingsList)
                    .build();
        }).toList();
        return OrderDto.OrderResponse.builder()
                .orderId(order.getId())
                .branchId(order.getBranch() != null ? order.getBranch().getId() : null)
                .branchName(order.getBranch() != null ? order.getBranch().getName() : null)
                .sessionId(order.getSession() != null ? order.getSession().getId() : null)
                .tableId(order.getSession() != null ? order.getSession().getTable().getId() : null)
                .tableName(order.getSession() != null ? order.getSession().getTable().getName() : null)
                .source(order.getSource())
                .status(order.getStatus())
                .customerName(customer != null ? customer.getFullName() : null)
                .customerEmail(customer != null ? customer.getEmail() : null)
                .subtotal(invoice != null ? invoice.getSubtotal() : BigDecimal.ZERO)
                .discountAmount(invoice != null ? invoice.getDiscountAmount() : BigDecimal.ZERO)
                .voucherDiscountAmount(invoice != null && invoice.getVoucherDiscountAmount() != null ? invoice.getVoucherDiscountAmount() : BigDecimal.ZERO)
                .voucherId(order.getVoucher() != null ? order.getVoucher().getId() : (invoice != null && invoice.getVoucher() != null ? invoice.getVoucher().getId() : null))
                .voucherCode(order.getVoucherCode() != null ? order.getVoucherCode() : (invoice != null ? invoice.getVoucherCode() : null))
                .surchargeAmount(BigDecimal.ZERO)
                .totalAmount(invoice != null ? invoice.getTotalAmount() : BigDecimal.ZERO)
                .discountPercent(invoice != null ? invoice.getSnapshotDiscountPercent() : BigDecimal.ZERO)
                .discountType(invoice != null ? invoice.getSnapshotDiscountType() : null)
                .discountConfigValue(invoice != null ? invoice.getSnapshotDiscountValue() : null)
                .invoiceId(invoice != null ? invoice.getId() : null)
                .invoiceStatus(invoice != null ? invoice.getStatus() : null)
                .paymentMethod(payment != null ? payment.getMethod().name() : null)
                .paidAt(payment != null ? payment.getPaidAt() : null)
                .cashierName(payment != null && payment.getRecordedBy() != null ? payment.getRecordedBy().getFullName() : null)
                .refund(refundRepository.findByOrderId(order.getId()).map(r -> vn.edu.ute.milktea.service.cancellation.RefundService.toResponse(r)).orElse(null))
                .cancellationPending(cancellationRepository.findFirstByOrderIdAndStatus(order.getId(), CancellationStatus.CHO).isPresent())
                .staffCreated(order.getAccount() != null && (order.getAccount().getRole() == Role.CASHIER || order.getAccount().getRole() == Role.ADMIN || order.getAccount().getRole() == Role.MANAGER))
                .paymentRequestMethod(latestNotice != null ? latestNotice.getMethod().name() : null)
                .transferNotified(transferNotified)
                .paid(isPaid)
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryType(order.getDeliveryType())
                .guestTableToken(tableToken)
                .guestCounterToken(counterToken)
                .build();
    }

    public OrderDto.OrderResponse mapOrderToResponse(Order order) {
        Invoice invoice = invoiceRepository.findByOrderId(order.getId()).orElse(null);
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        return mapToOrderResponse(order, invoice, items, null, null);
    }

    @Transactional(readOnly = true)
    public List<OrderDto.OrderResponse> getOrdersForCustomer(CurrentActor actor) {
        List<Order> orders = new ArrayList<>();
        if (actor != null && actor.hasRole(Role.ADMIN)) {
            orders = orderRepository.findAll();
        } else if (actor != null && (actor.hasRole(Role.CASHIER) || actor.hasRole(Role.MANAGER))) {
            if (actor.getBranchId() != null) {
                orders = orderRepository.findByBranchIdOrderByCreatedAtDesc(actor.getBranchId());
            } else {
                orders = Collections.emptyList();
            }
        } else if (actor != null && actor.getAccountId() != null) {
            orders = orderRepository.findByAccountIdOrderByCreatedAtDesc(actor.getAccountId());
        } else if (actor != null && actor.getSessionId() != null) {
            orders = orderRepository.findBySessionId(actor.getSessionId());
        }
        if (actor != null && actor.getOrderId() != null) {
            orders = orderRepository.findById(actor.getOrderId()).map(List::of).orElse(List.of());
        }
        return orders.stream().sorted(java.util.Comparator.comparing(Order::getCreatedAt).reversed()).map(this::mapOrderToResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto.OrderResponse getOrderForCustomer(Long orderId, CurrentActor actor) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));
        if (actor == null) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Bạn không có quyền xem đơn hàng này");
        }
        if (actor.hasRole(Role.ADMIN)) {
            return mapOrderToResponse(order);
        }
        if (actor.hasRole(Role.CASHIER) || actor.hasRole(Role.MANAGER)) {
            if (order.getBranch() != null && actor.canAccessBranch(order.getBranch().getId())) {
                return mapOrderToResponse(order);
            }
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Bạn không có quyền xem đơn hàng của chi nhánh khác");
        }
        boolean allowed = false;
        if (actor.getAccountId() != null && order.getAccount() != null) {
            allowed = actor.getAccountId().equals(order.getAccount().getId());
        } else if (actor.getSessionId() != null && order.getSession() != null) {
            allowed = actor.getSessionId().equals(order.getSession().getId());
        } else if (actor.getOrderId() != null) {
            allowed = actor.getOrderId().equals(order.getId());
        }
        if (!allowed) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Bạn không có quyền xem đơn hàng này");
        }
        return mapOrderToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDto.OrderResponse> getOrdersForCashier(OrderStatus status) {
        return getOrdersForCashier(null, status, null, null, null);
    }

    private static class CalculatedDiscount {
        final BigDecimal discountAmount;
        final BigDecimal totalAmount;
        final BigDecimal snapshotPercent;
        final String discountType;
        final BigDecimal configValue;

        CalculatedDiscount(BigDecimal discountAmount, BigDecimal totalAmount, BigDecimal snapshotPercent, String discountType, BigDecimal configValue) {
            this.discountAmount = discountAmount;
            this.totalAmount = totalAmount;
            this.snapshotPercent = snapshotPercent;
            this.discountType = discountType;
            this.configValue = configValue;
        }
    }

    private CalculatedDiscount computeDiscount(BigDecimal subtotal, GlobalSettings settings) {
        String discountType = (settings != null && settings.getDiscountType() != null) ? settings.getDiscountType().trim().toUpperCase() : "PERCENT";
        BigDecimal configValue = BigDecimal.ZERO;
        BigDecimal snapshotPercent = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (settings != null) {
            if ("FIXED_AMOUNT".equals(discountType)) {
                configValue = settings.getDiscountValue() != null ? settings.getDiscountValue() : BigDecimal.ZERO;
                discountAmount = configValue.min(subtotal).max(BigDecimal.ZERO);
                snapshotPercent = BigDecimal.ZERO;
            } else {
                discountType = "PERCENT";
                configValue = settings.getDiscountValue() != null && settings.getDiscountValue().signum() > 0
                        ? settings.getDiscountValue()
                        : (settings.getDiscountPercent() != null ? settings.getDiscountPercent() : BigDecimal.ZERO);
                snapshotPercent = configValue;
                discountAmount = subtotal.multiply(snapshotPercent)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        .min(subtotal)
                        .max(BigDecimal.ZERO);
            }
        }
        BigDecimal totalAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);
        return new CalculatedDiscount(discountAmount, totalAmount, snapshotPercent, discountType, configValue);
    }

    @Transactional(readOnly = true)
    public List<OrderDto.OrderResponse> getOrdersForCashier(Long branchId, OrderStatus status, String paymentStatus, String tab, String query) {
        List<Order> orders;
        boolean isAllTab = (tab == null || tab.isBlank() || "all".equalsIgnoreCase(tab));
        if (branchId != null) {
            if (isAllTab && status != null) {
                orders = orderRepository.findByBranchIdAndStatusInOrderByCreatedAtAsc(branchId, List.of(status));
            } else {
                orders = orderRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
            }
        } else {
            if (isAllTab && status != null) {
                orders = orderRepository.findByStatusInOrderByCreatedAtAsc(List.of(status));
            } else {
                orders = orderRepository.findAll();
            }
        }
        var stream = orders.stream()
                .sorted(java.util.Comparator.comparing(Order::getCreatedAt).reversed())
                .map(this::mapOrderToResponse);
        if (query != null && !query.isBlank()) {
            String q = query.trim().toUpperCase().replaceFirst("^#", "");
            stream = stream.filter(o -> {
                String code = "ORD" + String.format("%03d", o.getOrderId());
                return code.contains(q) || String.valueOf(o.getOrderId()).equals(q)
                        || (o.getTableName() != null && o.getTableName().toUpperCase().contains(q))
                        || (o.getCustomerName() != null && o.getCustomerName().toUpperCase().contains(q));
            });
        }
        if (!isAllTab) {
            stream = stream.filter(o -> matchesTab(o, tab));
        } else {
            if (status != null) {
                stream = stream.filter(o -> o.getStatus() == status);
            }
            if (paymentStatus != null && !paymentStatus.isBlank()) {
                String ps = paymentStatus.trim().toUpperCase();
                if ("PAID".equals(ps)) {
                    stream = stream.filter(OrderDto.OrderResponse::isPaid);
                } else if ("PAID_TM".equals(ps) || "PAID_CASH".equals(ps)) {
                    stream = stream.filter(o -> o.isPaid() && (!"BANK_TRANSFER".equalsIgnoreCase(o.getPaymentMethod()) && !o.isTransferNotified()));
                } else if ("PAID_CK".equals(ps) || "PAID_TRANSFER".equals(ps) || "PAID_BANK_TRANSFER".equals(ps)) {
                    stream = stream.filter(o -> o.isPaid() && ("BANK_TRANSFER".equalsIgnoreCase(o.getPaymentMethod()) || o.isTransferNotified()));
                } else if ("UNPAID".equals(ps)) {
                    stream = stream.filter(o -> !o.isPaid() && o.getStatus() != OrderStatus.DA_HUY);
                } else if ("TRANSFER_PENDING".equals(ps)) {
                    stream = stream.filter(o -> o.isTransferNotified() && !o.isPaid());
                }
            }
        }
        return stream.toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDto.OrderResponse> getOrdersForCashier(OrderStatus status, String paymentStatus, String tab, String query) {
        return getOrdersForCashier(null, status, paymentStatus, tab, query);
    }

    @Transactional(readOnly = true)
    public List<OrderDto.OrderResponse> getOrdersForCashier(OrderStatus status, String tab, String query) {
        return getOrdersForCashier(null, status, null, tab, query);
    }

    public boolean matchesTab(OrderDto.OrderResponse o, String tab) {
        if (tab == null || tab.isBlank() || "all".equalsIgnoreCase(tab)) return true;
        switch (tab.toLowerCase()) {
            case "confirm":
                return o.getStatus() == OrderStatus.CHO_XAC_NHAN && !o.isStaffCreated();
            case "cooking":
                return o.getStatus() == OrderStatus.CHO_CHE_BIEN || o.getStatus() == OrderStatus.DANG_CHE_BIEN;
            case "completed":
                return o.getStatus() == OrderStatus.HOAN_THANH;
            case "transfer":
                return o.isTransferNotified() && !o.isPaid() && o.getStatus() != OrderStatus.DA_HUY;
            case "cancel":
                return o.isCancellationPending() && o.getStatus() == OrderStatus.CHO_CHE_BIEN && o.isPaid();
            case "refund":
                return o.getStatus() == OrderStatus.DA_HUY && o.isPaid();
            default:
                return true;
        }
    }

    @Transactional(readOnly = true)
    public List<OrderDto.OrderResponse> getOrdersForKitchen(Long branchId) {
        List<Order> orders;
        if (branchId != null) {
            orders = orderRepository.findByBranchIdAndStatusInOrderByCreatedAtAsc(
                    branchId, List.of(OrderStatus.CHO_CHE_BIEN, OrderStatus.DANG_CHE_BIEN));
        } else {
            orders = orderRepository.findByStatusInOrderByCreatedAtAsc(
                    List.of(OrderStatus.CHO_CHE_BIEN, OrderStatus.DANG_CHE_BIEN));
        }
        return orders.stream().map(this::mapOrderToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDto.OrderResponse> getOrdersForKitchen() {
        return getOrdersForKitchen(null);
    }

    @Transactional(readOnly = true)
    public OrderDto.OrderWorkSummaryResponse getOrderWorkSummary(Long branchId) {
        List<Order> allOrders = (branchId != null)
                ? orderRepository.findByBranchIdOrderByCreatedAtDesc(branchId)
                : orderRepository.findAll();
        long totalCount = allOrders.size();
        long confirmCount = 0;
        long cookingCount = 0;
        long completedCount = 0;
        long transferCount = 0;
        long cancelCount = 0;
        long refundCount = 0;
        Set<Long> pendingActionOrderIds = new HashSet<>();

        var allRefunds = refundRepository.findAll();
        Map<Long, vn.edu.ute.milktea.entity.cancellation.RefundRequest> refundByOrderId = allRefunds.stream()
                .collect(java.util.stream.Collectors.toMap(r -> r.getOrder().getId(), r -> r, (r1, r2) -> r1));

        for (Order o : allOrders) {
            boolean isStaffCreated = o.getAccount() != null && (o.getAccount().getRole() == Role.CASHIER || o.getAccount().getRole() == Role.ADMIN || o.getAccount().getRole() == Role.MANAGER);
            if (o.getStatus() == OrderStatus.CHO_XAC_NHAN && !isStaffCreated) {
                confirmCount++;
                pendingActionOrderIds.add(o.getId());
            }

            if (o.getStatus() == OrderStatus.CHO_CHE_BIEN || o.getStatus() == OrderStatus.DANG_CHE_BIEN) {
                cookingCount++;
            }

            if (o.getStatus() == OrderStatus.HOAN_THANH) {
                completedCount++;
            }

            Invoice inv = invoiceRepository.findByOrderId(o.getId()).orElse(null);
            boolean isPaid = inv != null && paymentRepository.existsByInvoiceId(inv.getId());

            boolean hasPendingCancel = cancellationRepository.findFirstByOrderIdAndStatus(
                    o.getId(), CancellationStatus.CHO).isPresent();
            if (hasPendingCancel && o.getStatus() == OrderStatus.CHO_CHE_BIEN && isPaid) {
                cancelCount++;
                pendingActionOrderIds.add(o.getId());
            }

            if (inv != null && inv.getStatus() == InvoiceStatus.HIEU_LUC && !isPaid && o.getStatus() != OrderStatus.DA_HUY) {
                var latestNotice = paymentNoticeRepository.findFirstByInvoiceIdOrderByCreatedAtDescIdDesc(inv.getId());
                if (latestNotice.isPresent() && latestNotice.get().getMethod() == vn.edu.ute.milktea.entity.payment.PaymentMethod.BANK_TRANSFER) {
                    String ref = latestNotice.get().getReference();
                    if (ref != null && (ref.startsWith("NOTICE_") || ref.startsWith("TRANSFERRED_") || ref.startsWith("CUSTOMER_TRANSFERRED_"))) {
                        transferCount++;
                        pendingActionOrderIds.add(o.getId());
                    }
                }
            }

            if (o.getStatus() == OrderStatus.DA_HUY && isPaid) {
                var refReq = refundByOrderId.get(o.getId());
                if (refReq == null || refReq.getStatus() != vn.edu.ute.milktea.entity.cancellation.RefundStatus.DA_HOAN) {
                    refundCount++;
                    pendingActionOrderIds.add(o.getId());
                }
            }
        }

        return OrderDto.OrderWorkSummaryResponse.builder()
                .totalCount(totalCount)
                .confirmCount(confirmCount)
                .cookingCount(cookingCount)
                .completedCount(completedCount)
                .transferCount(transferCount)
                .cancelCount(cancelCount)
                .refundCount(refundCount)
                .pendingActionCount(pendingActionOrderIds.size())
                .build();
    }

    @Transactional(readOnly = true)
    public OrderDto.OrderWorkSummaryResponse getOrderWorkSummary() {
        return getOrderWorkSummary(null);
    }

    private String canonicalizeCartItems(List<CartDto.CartItemRequest> items) {
        if (items == null || items.isEmpty()) return "[]";
        List<String> formatted = new java.util.ArrayList<>();
        for (CartDto.CartItemRequest item : items) {
            if (item == null) continue;
            Long pid = item.getProductId() != null ? item.getProductId() : 0L;
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            String note = item.getNote() != null ? item.getNote().trim() : "";
            List<Long> toppings = item.getToppingIds() != null
                    ? item.getToppingIds().stream().filter(java.util.Objects::nonNull).sorted().toList()
                    : java.util.Collections.emptyList();
            formatted.add(pid + ":" + qty + ":" + note + ":" + toppings);
        }
        java.util.Collections.sort(formatted);
        return String.join(";", formatted);
    }

    private String buildCanonicalTablePayload(OrderDto.CreateTableOrderRequest request) {
        String voucherStr = (request.getVoucherCode() != null) ? request.getVoucherCode().trim().toUpperCase() : "";
        String noteStr = (request.getNote() != null) ? request.getNote().trim() : "";
        String itemsStr = canonicalizeCartItems(request.getItems());
        String remainingStr = canonicalizeCartItems(request.getRemainingCartItems());
        return "tableId=" + request.getTableId() +
                ";session=" + request.getExpectedSessionId() +
                ";note=" + noteStr +
                ";voucher=" + voucherStr +
                ";items=" + itemsStr +
                ";remaining=" + remainingStr;
    }

    private String buildCanonicalCounterPayload(OrderDto.CreateCounterOrderRequest request, Long targetBranchId) {
        String voucherStr = (request.getVoucherCode() != null) ? request.getVoucherCode().trim().toUpperCase() : "";
        String noteStr = (request.getNote() != null) ? request.getNote().trim() : "";
        String itemsStr = canonicalizeCartItems(request.getItems());
        return "branchId=" + targetBranchId +
                ";note=" + noteStr +
                ";voucher=" + voucherStr +
                ";items=" + itemsStr;
    }

    private String resolveCallerScope(CurrentActor actor, Long tableId) {
        if (actor != null && actor.isAuthenticated() && actor.getAccountId() != null) {
            return "USER:" + actor.getAccountId();
        }
        if (actor != null && actor.getSessionId() != null) {
            return "SESSION:" + actor.getSessionId();
        }
        if (tableId != null) {
            return "TABLE:" + tableId;
        }
        return "GUEST";
    }
}
