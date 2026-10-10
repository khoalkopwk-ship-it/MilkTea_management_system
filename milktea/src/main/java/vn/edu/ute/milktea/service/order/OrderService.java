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
    private final OrderIngredientSnapshotRepository snapshotRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final vn.edu.ute.milktea.repository.cancellation.RefundRequestRepository refundRepository;
    private final CancellationRequestRepository cancellationRepository;
    private final GlobalSettingsRepository settingsRepository;
    private final AccountRepository accountRepository;
    private final BusinessAuditRepository auditRepository;
    private final GuestAccessService guestAccessService;
    private final InventoryService inventoryService;
    private final vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher realtimeEventPublisher;

    @Transactional
    public OrderDto.OrderResponse createTableOrder(OrderDto.CreateTableOrderRequest request, CurrentActor actor, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Order order = existing.get();
                Invoice invoice = invoiceRepository.findByOrderId(order.getId()).orElse(null);
                List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                return mapToOrderResponse(order, invoice, items, null, null);
            }
        }

        DiningTable table = tableRepository.findByIdWithLock(request.getTableId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_NOT_FOUND, "Không tìm thấy bàn"));

        if (!Boolean.TRUE.equals(table.getActive())) {
            throw BusinessException.badRequest(ErrorCode.TABLE_NOT_FOUND, "Bàn đang ngừng hoạt động");
        }

        // Kiểm tra expectedSessionId
        TableSession session;
        String rawGuestTableToken = null;

        if (table.getActiveSessionId() == null) {
            if (request.getExpectedSessionId() != null) {
                throw BusinessException.conflict(ErrorCode.TABLE_SESSION_CHANGED, "Bàn chưa có phiên hoặc phiên đã kết thúc");
            }
            // Mở phiên mới
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
            if (request.getExpectedSessionId() == null) {
                throw BusinessException.conflict(ErrorCode.TABLE_SESSION_CHANGED, "Bàn đã có khách đang phục vụ. Vui lòng tải lại.");
            }
            if (!table.getActiveSessionId().equals(request.getExpectedSessionId())) {
                throw BusinessException.conflict(ErrorCode.TABLE_SESSION_CHANGED, "Phiên bàn đã thay đổi");
            }
            session = sessionRepository.findById(table.getActiveSessionId())
                    .orElseThrow(() -> BusinessException.conflict(ErrorCode.TABLE_SESSION_CHANGED, "Không tìm thấy phiên"));
        }

        // Tạo đơn hàng và tính giá snapshot
        Account customerAccount = (actor != null && actor.isAuthenticated()) ?
                accountRepository.findById(actor.getAccountId()).orElse(null) : null;

        Order order = Order.builder()
                .session(session)
                .account(customerAccount)
                .source(OrderSource.TABLE)
                .status(OrderStatus.CHO_THANH_TOAN)
                .idempotencyKey(idempotencyKey)
                .createdAt(Instant.now())
                .version(0L)
                .build();
        order = orderRepository.save(order);

        List<OrderItem> savedItems = createOrderItemsAndSnapshots(order, request.getItems());

        // Tính toán tổng tiền và giảm giá từ GlobalSettings
        BigDecimal subtotal = savedItems.stream()
                .map(it -> it.getSnapshotUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        BigDecimal discountPercent = settings != null ? settings.getDiscountPercent() : BigDecimal.ZERO;
        BigDecimal discountAmount = subtotal.multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);

        Invoice invoice = Invoice.builder()
                .order(order)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .snapshotDiscountPercent(discountPercent)
                .status(InvoiceStatus.HIEU_LUC)
                .createdAt(Instant.now())
                .build();
        invoice = invoiceRepository.save(invoice);

        // Cập nhật phần còn lại của giỏ hàng trên server
        sessionCartService.replaceCart(session.getId(),
                new CartDto.ReplaceCartRequest(request.getRemainingCartItems() != null ? request.getRemainingCartItems() : Collections.emptyList()),
                null);

        auditRepository.save(BusinessAudit.builder()
                .sessionId(session.getId())
                .orderId(order.getId())
                .accountId(customerAccount != null ? customerAccount.getId() : null)
                .action("ORDER_CREATED")
                .afterState("CHO_THANH_TOAN")
                .createdAt(Instant.now())
                .build());

        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "ORDER_CREATED", order.getId().toString(), session.getId().toString(), "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        realtimeEventPublisher.publishAfterCommit("/topic/table-sessions/" + session.getId(), "ORDER_CREATED", order.getId().toString(), session.getId().toString(), "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));

        return mapToOrderResponse(order, invoice, savedItems, rawGuestTableToken, null);
    }

    @Transactional
    public OrderDto.OrderResponse createCounterOrder(OrderDto.CreateCounterOrderRequest request, CurrentActor actor, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Order order = existing.get();
                Invoice invoice = invoiceRepository.findByOrderId(order.getId()).orElse(null);
                List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                return mapToOrderResponse(order, invoice, items, null, null);
            }
        }

        Account creator = (actor != null && actor.isAuthenticated()) ?
                accountRepository.findById(actor.getAccountId()).orElse(null) : null;

        Order order = Order.builder()
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

        BigDecimal subtotal = savedItems.stream()
                .map(it -> it.getSnapshotUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        BigDecimal discountPercent = settings != null ? settings.getDiscountPercent() : BigDecimal.ZERO;
        BigDecimal discountAmount = subtotal.multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);

        Invoice invoice = Invoice.builder()
                .order(order)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .snapshotDiscountPercent(discountPercent)
                .status(InvoiceStatus.HIEU_LUC)
                .createdAt(Instant.now())
                .build();
        invoice = invoiceRepository.save(invoice);

        var issued = guestAccessService.issueCounterOrderToken(order, 24);

        auditRepository.save(BusinessAudit.builder()
                .orderId(order.getId())
                .accountId(creator != null ? creator.getId() : null)
                .action("COUNTER_ORDER_CREATED")
                .afterState("CHO_THANH_TOAN")
                .createdAt(Instant.now())
                .build());

        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "ORDER_CREATED", order.getId().toString(), null, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));

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

        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "ORDER_ASSIGNED_TO_TABLE", order.getId().toString(), session.getId().toString(), "1", Map.of("orderId", order.getId()));
        realtimeEventPublisher.publishAfterCommit("/topic/table-sessions/" + session.getId(), "ORDER_ASSIGNED_TO_TABLE", order.getId().toString(), session.getId().toString(), "1", Map.of("orderId", order.getId()));
    }

    @Transactional
    public void confirm(Long orderId, Long cashierId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));

        if (order.getStatus() == OrderStatus.CHO_CHE_BIEN) {
            return;
        }

        if (order.getStatus() != OrderStatus.CHO_XAC_NHAN) {
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT,
                    "Không thể xác nhận đơn ở trạng thái: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CHO_CHE_BIEN);
        
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> BusinessException.notFound(
                        ErrorCode.ORDER_STATE_CONFLICT,
                        "Không tìm thấy hóa đơn của đơn hàng"));

        if (!paymentRepository.existsByInvoiceId(invoice.getId())) {
            throw BusinessException.conflict(
                    ErrorCode.ORDER_STATE_CONFLICT,
                    "Đơn hàng chưa được thanh toán, không thể xác nhận");
        }

        orderRepository.save(order);

        auditRepository.save(BusinessAudit.builder()
                .orderId(order.getId())
                .accountId(cashierId)
                .action("ORDER_CONFIRMED")
                .beforeState("CHO_XAC_NHAN")
                .afterState("CHO_CHE_BIEN")
                .createdAt(Instant.now())
                .build());

        String sId = order.getSession() != null ? order.getSession().getId().toString() : null;
        realtimeEventPublisher.publishAfterCommit("/topic/kitchen", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        if (order.getSession() != null) {
            realtimeEventPublisher.publishAfterCommit("/topic/table-sessions/" + order.getSession().getId(), "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        }
    }

    @Transactional
    public void start(Long orderId, Long kitchenId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));

        if (order.getStatus() == OrderStatus.DANG_CHE_BIEN) {
            return;
        }

        if (order.getStatus() != OrderStatus.CHO_CHE_BIEN) {
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT,
                    "Không thể bắt đầu chế biến đơn ở trạng thái: " + order.getStatus());
        }
        
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> BusinessException.notFound(
                        ErrorCode.ORDER_STATE_CONFLICT,
                        "Không tìm thấy hóa đơn của đơn hàng"));

        if (!paymentRepository.existsByInvoiceId(invoice.getId())) {
            throw BusinessException.conflict(
                    ErrorCode.ORDER_STATE_CONFLICT,
                    "Đơn hàng chưa được thanh toán, bếp không thể bắt đầu");
        }


        // Kiểm tra không có yêu cầu hủy đang chờ xử lý
        var pendingCancel = cancellationRepository.findFirstByOrderIdAndStatus(orderId,
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
        realtimeEventPublisher.publishAfterCommit("/topic/kitchen", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        if (order.getSession() != null) {
            realtimeEventPublisher.publishAfterCommit("/topic/table-sessions/" + order.getSession().getId(), "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        }
    }

    @Transactional
    public void complete(Long orderId, Long kitchenId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));

        if (order.getStatus() == OrderStatus.HOAN_THANH) {
            return;
        }

        if (order.getStatus() != OrderStatus.DANG_CHE_BIEN) {
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT,
                    "Chỉ có thể hoàn thành đơn đang ở trạng thái DANG_CHE_BIEN. Trạng thái hiện tại: " + order.getStatus());
        }

        Account kitchen = accountRepository.findById(kitchenId).orElse(null);

        // Tiêu hao kho BEP theo OrderIngredientSnapshot trong cùng transaction (REQUIRED)
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
        realtimeEventPublisher.publishAfterCommit("/topic/kitchen", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "ORDER_STATUS_CHANGED", order.getId().toString(), sId, "1", Map.of("orderId", order.getId(), "status", order.getStatus().name()));
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
        Map<Long, ProductRecipe> recipeSampleMap = new HashMap<>();

        for (var req : itemRequests) {
            Product product = productRepository.findById(req.getProductId())
                    .orElseThrow(() -> BusinessException.badRequest(ErrorCode.PRODUCT_UNAVAILABLE, "Món không tồn tại: ID " + req.getProductId()));

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw BusinessException.badRequest(ErrorCode.PRODUCT_UNAVAILABLE, "Món đã ngừng bán: " + product.getName());
            }

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .snapshotName(product.getName())
                    .snapshotSize(product.getSize())
                    .snapshotUnitPrice(product.getPrice())
                    .quantity(req.getQuantity())
                    .note(req.getNote())
                    .build();
            orderItems.add(item);

            // Tính định mức nguyên liệu
            List<ProductRecipe> recipes = recipeRepository.findByIdProductId(product.getId());
            for (ProductRecipe recipe : recipes) {
                Long matId = recipe.getMaterial().getId();
                BigDecimal needed = recipe.getQuantity().multiply(BigDecimal.valueOf(req.getQuantity()));
                aggregatedIngredients.merge(matId, needed, BigDecimal::add);
                recipeSampleMap.putIfAbsent(matId, recipe);
            }
        }

        orderItems = orderItemRepository.saveAll(orderItems);

        // Lưu OrderIngredientSnapshot cho toàn đơn
        List<OrderIngredientSnapshot> snapshots = new ArrayList<>();
        for (var entry : aggregatedIngredients.entrySet()) {
            Long matId = entry.getKey();
            BigDecimal totalQty = entry.getValue();
            ProductRecipe sample = recipeSampleMap.get(matId);

            OrderIngredientSnapshot snap = OrderIngredientSnapshot.builder()
                    .id(new OrderIngredientSnapshotId(order.getId(), matId))
                    .order(order)
                    .material(sample.getMaterial())
                    .totalQuantity(totalQty)
                    .snapshotType(sample.getMaterial().getType())
                    .snapshotUnit(sample.getMaterial().getUnit())
                    .build();
            snapshots.add(snap);
        }
        snapshotRepository.saveAll(snapshots);

        return orderItems;
    }

    private OrderDto.OrderResponse mapToOrderResponse(
            Order order, Invoice invoice, List<OrderItem> items, String tableToken, String counterToken) {

        boolean isPaid = invoice != null && paymentRepository.existsByInvoiceId(invoice.getId());
        var payment = invoice != null ? paymentRepository.findByInvoiceId(invoice.getId()).orElse(null) : null;
        Account customer = order.getAccount();
        if (customer != null && customer.getRole() != vn.edu.ute.milktea.entity.account.Role.CUSTOMER) customer = null;

        List<OrderDto.OrderItemResponse> itemResponses = items.stream().map(it ->
                OrderDto.OrderItemResponse.builder()
                        .id(it.getId())
                        .productId(it.getProduct().getId())
                        .productName(it.getSnapshotName())
                        .size(it.getSnapshotSize())
                        .quantity(it.getQuantity())
                        .unitPrice(it.getSnapshotUnitPrice())
                        .lineTotal(it.getSnapshotUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity())))
                        .note(it.getNote())
                        .build()
        ).toList();

        return OrderDto.OrderResponse.builder()
                .orderId(order.getId())
                .sessionId(order.getSession() != null ? order.getSession().getId() : null)
                .tableId(order.getSession() != null ? order.getSession().getTable().getId() : null)
                .tableName(order.getSession() != null ? order.getSession().getTable().getName() : null)
                .source(order.getSource())
                .status(order.getStatus())
                .customerName(customer != null ? customer.getFullName() : null)
                .customerEmail(customer != null ? customer.getEmail() : null)
                .subtotal(invoice != null ? invoice.getSubtotal() : BigDecimal.ZERO)
                .discountAmount(invoice != null ? invoice.getDiscountAmount() : BigDecimal.ZERO)
                .surchargeAmount(BigDecimal.ZERO)
                .totalAmount(invoice != null ? invoice.getTotalAmount() : BigDecimal.ZERO)
                .discountPercent(invoice != null ? invoice.getSnapshotDiscountPercent() : BigDecimal.ZERO)
                .invoiceId(invoice != null ? invoice.getId() : null)
                .invoiceStatus(invoice != null ? invoice.getStatus() : null)
                .paymentMethod(payment != null ? payment.getMethod().name() : null)
                .paidAt(payment != null ? payment.getPaidAt() : null)
                .cashierName(payment != null && payment.getRecordedBy() != null ? payment.getRecordedBy().getFullName() : null)
                .refund(refundRepository.findByOrderId(order.getId()).map(r -> vn.edu.ute.milktea.service.cancellation.RefundService.toResponse(r)).orElse(null))
                .cancellationPending(cancellationRepository.findFirstByOrderIdAndStatus(order.getId(), CancellationStatus.CHO).isPresent())
                .staffCreated(order.getAccount() != null && (order.getAccount().getRole() == Role.CASHIER || order.getAccount().getRole() == Role.ADMIN))
                .paid(isPaid)
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
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
        if (actor != null && (actor.hasRole(vn.edu.ute.milktea.entity.account.Role.CASHIER) || actor.hasRole(vn.edu.ute.milktea.entity.account.Role.ADMIN))) {
            orders = orderRepository.findAll();
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
    public List<OrderDto.OrderResponse> getOrderHistoryForCustomerAccount(CurrentActor actor) {
        if (actor == null || actor.getAccountId() == null) {
            throw BusinessException.unauthorized(ErrorCode.ACCESS_DENIED, "Vui lòng đăng nhập để xem lịch sử đơn hàng");
        }
        return orderRepository.findByAccountIdOrderByCreatedAtDesc(actor.getAccountId())
                .stream()
                .map(this::mapOrderToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDto.OrderResponse getOrderForCustomer(Long orderId, CurrentActor actor) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));

        if (actor == null) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Bạn không có quyền xem đơn hàng này");
        }

        boolean allowed = actor.hasRole(vn.edu.ute.milktea.entity.account.Role.CASHIER) || actor.hasRole(vn.edu.ute.milktea.entity.account.Role.ADMIN);
        if (allowed) return mapOrderToResponse(order);
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
        List<Order> orders;
        if (status != null) {
            orders = orderRepository.findByStatusInOrderByCreatedAtAsc(List.of(status));
        } else {
            orders = orderRepository.findAll();
        }
        return orders.stream().map(this::mapOrderToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDto.OrderResponse> getOrdersForKitchen() {
        List<Order> orders = orderRepository.findByStatusInOrderByCreatedAtAsc(
                List.of(OrderStatus.CHO_CHE_BIEN, OrderStatus.DANG_CHE_BIEN));
        return orders.stream().map(this::mapOrderToResponse).toList();
    }
}
