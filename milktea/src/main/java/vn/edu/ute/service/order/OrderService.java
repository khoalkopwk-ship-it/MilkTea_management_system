package vn.edu.ute.service.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.dto.customer.*;
import vn.edu.ute.entity.menu.Product;
import vn.edu.ute.entity.menu.ProductSize;
import vn.edu.ute.entity.order.Order;
import vn.edu.ute.entity.order.OrderItem;
import vn.edu.ute.enums.order.OrderSource;
import vn.edu.ute.enums.order.OrderStatus;
import vn.edu.ute.repository.menu.ProductRepository;
import vn.edu.ute.repository.menu.ProductSizeRepository;
import vn.edu.ute.repository.order.OrderItemRepository;
import vn.edu.ute.repository.order.OrderRepository;
import vn.edu.ute.util.GuestTokenGenerator;
import vn.edu.ute.util.OrderCodeGenerator;
import vn.edu.ute.websocket.OrderSocketService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductRepository productRepository;
    private final ProductSizeRepository productSizeRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderSocketService orderSocketService;

    @Transactional
    public CreateOrderResponse createGuestOrder(
            CreateOrderRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Dữ liệu đơn hàng không hợp lệ"
            );
        }

        if (request.getItems() == null ||
                request.getItems().isEmpty()) {

            throw new IllegalArgumentException(
                    "Đơn hàng phải có ít nhất một món"
            );
        }

        String guestToken =
                GuestTokenGenerator.generate();

        LocalDateTime now =
                LocalDateTime.now();

        Order order =
                Order.builder()
                        .orderCode(
                                OrderCodeGenerator.generate()
                        )
                        .tableId(
                                request.getTableId()
                        )
                        .guestToken(
                                guestToken
                        )
                        .source(
                                OrderSource.GUEST
                        )
                        .status(
                                OrderStatus.PENDING
                        )
                        .totalAmount(
                                BigDecimal.ZERO
                        )
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

        order = orderRepository.save(order);

        BigDecimal orderTotal =
                BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest :
                request.getItems()) {

            validateItemRequest(itemRequest);

            Product product =
                    productRepository
                            .findById(
                                    itemRequest.getProductId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Món không tồn tại"
                                    )
                            );

            if (!Boolean.TRUE.equals(
                    product.getActive()
            )) {
                throw new IllegalArgumentException(
                        "Món "
                                + product.getName()
                                + " hiện không còn bán"
                );
            }

            ProductSize size =
                    productSizeRepository
                            .findByIdAndProductIdAndActiveTrue(
                                    itemRequest.getSizeId(),
                                    product.getId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Size không hợp lệ cho món "
                                                    + product.getName()
                                    )
                            );

            BigDecimal basePrice =
                    product.getBasePrice() == null
                            ? BigDecimal.ZERO
                            : product.getBasePrice();

            BigDecimal extraPrice =
                    size.getExtraPrice() == null
                            ? BigDecimal.ZERO
                            : size.getExtraPrice();

            BigDecimal unitPrice =
                    basePrice.add(extraPrice);

            BigDecimal lineTotal =
                    unitPrice.multiply(
                            BigDecimal.valueOf(
                                    itemRequest.getQuantity()
                            )
                    );

            OrderItem orderItem =
                    OrderItem.builder()
                            .order(order)
                            .product(product)
                            .size(size)
                            .quantity(
                                    itemRequest.getQuantity()
                            )
                            .unitPrice(unitPrice)
                            .totalPrice(lineTotal)
                            .note(
                                    normalizeNote(
                                            itemRequest.getNote()
                                    )
                            )
                            .build();

            orderItemRepository.save(orderItem);

            orderTotal =
                    orderTotal.add(lineTotal);
        }

        order.setTotalAmount(orderTotal);
        order.setUpdatedAt(LocalDateTime.now());

        orderRepository.save(order);

        return CreateOrderResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .guestToken(order.getGuestToken())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .build();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getGuestOrder(
            Long orderId,
            String guestToken
    ) {

        Order order =
                findGuestOrder(
                        orderId,
                        guestToken
                );

        return toOrderDetailResponse(order);
    }

    @Transactional
    public CancelOrderResponse cancelGuestOrder(
            Long orderId,
            String guestToken
    ) {

        Order order =
                findGuestOrder(
                        orderId,
                        guestToken
                );

        if (order.getStatus()
                != OrderStatus.PENDING &&
                order.getStatus()
                        != OrderStatus.CONFIRMED) {

            throw new IllegalStateException(
                    "Đơn đã vào pha chế hoặc đã hoàn tất, không thể hủy"
            );
        }

        order.setStatus(
                OrderStatus.CANCELLED
        );

        order.setUpdatedAt(
                LocalDateTime.now()
        );

        orderRepository.save(order);

        orderSocketService.sendOrderStatus(
                order.getId(),
                order.getStatus()
        );

        return CancelOrderResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .message(
                        "Đã hủy toàn bộ đơn hàng"
                )
                .build();
    }

    @Transactional
    public OrderDetailResponse updateOrderStatus(
            Long orderId,
            OrderStatus newStatus
    ) {

        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "Trạng thái mới không được để trống"
            );
        }

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Không tìm thấy đơn hàng"
                                )
                        );

        validateStatusTransition(
                order.getStatus(),
                newStatus
        );

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        orderRepository.save(order);

        orderSocketService.sendOrderStatus(
                order.getId(),
                order.getStatus()
        );

        return toOrderDetailResponse(order);
    }

    private Order findGuestOrder(
            Long orderId,
            String guestToken
    ) {

        if (orderId == null) {
            throw new IllegalArgumentException(
                    "orderId không hợp lệ"
            );
        }

        if (guestToken == null ||
                guestToken.isBlank()) {

            throw new IllegalArgumentException(
                    "GuestToken không được để trống"
            );
        }

        return orderRepository
                .findByIdAndGuestToken(
                        orderId,
                        guestToken
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy đơn hoặc GuestToken không hợp lệ"
                        )
                );
    }

    private void validateItemRequest(
            CreateOrderItemRequest item
    ) {

        if (item == null) {
            throw new IllegalArgumentException(
                    "Dữ liệu món không hợp lệ"
            );
        }

        if (item.getProductId() == null) {
            throw new IllegalArgumentException(
                    "productId không được để trống"
            );
        }

        if (item.getSizeId() == null) {
            throw new IllegalArgumentException(
                    "sizeId không được để trống"
            );
        }

        if (item.getQuantity() == null ||
                item.getQuantity() <= 0) {

            throw new IllegalArgumentException(
                    "Số lượng phải lớn hơn 0"
            );
        }

        if (item.getQuantity() > 100) {
            throw new IllegalArgumentException(
                    "Số lượng món vượt quá giới hạn cho phép"
            );
        }
    }

    private String normalizeNote(
            String note
    ) {

        if (note == null) {
            return null;
        }

        String result =
                note.trim();

        if (result.isEmpty()) {
            return null;
        }

        if (result.length() > 500) {
            throw new IllegalArgumentException(
                    "Ghi chú không được vượt quá 500 ký tự"
            );
        }

        return result;
    }

    private void validateStatusTransition(
            OrderStatus oldStatus,
            OrderStatus newStatus
    ) {

        if (oldStatus == newStatus) {
            throw new IllegalStateException(
                    "Đơn hàng đã ở trạng thái "
                            + oldStatus
            );
        }

        boolean valid =
                switch (oldStatus) {

                    case PENDING ->
                            newStatus == OrderStatus.CONFIRMED
                                    ||
                            newStatus == OrderStatus.CANCELLED;

                    case CONFIRMED ->
                            newStatus == OrderStatus.PREPARING
                                    ||
                            newStatus == OrderStatus.CANCELLED;

                    case PREPARING ->
                            newStatus == OrderStatus.READY;

                    case READY ->
                            newStatus == OrderStatus.COMPLETED;

                    case COMPLETED, CANCELLED ->
                            false;
                };

        if (!valid) {
            throw new IllegalStateException(
                    "Không thể chuyển trạng thái từ "
                            + oldStatus
                            + " sang "
                            + newStatus
            );
        }
    }

    private OrderDetailResponse toOrderDetailResponse(
            Order order
    ) {

        List<OrderItemResponse> items =
                orderItemRepository
                        .findByOrderId(order.getId())
                        .stream()
                        .map(item ->
                                OrderItemResponse.builder()
                                        .id(item.getId())
                                        .productId(
                                                item.getProduct()
                                                        .getId()
                                        )
                                        .productName(
                                                item.getProduct()
                                                        .getName()
                                        )
                                        .sizeId(
                                                item.getSize()
                                                        .getId()
                                        )
                                        .sizeName(
                                                item.getSize()
                                                        .getName()
                                        )
                                        .quantity(
                                                item.getQuantity()
                                        )
                                        .unitPrice(
                                                item.getUnitPrice()
                                        )
                                        .totalPrice(
                                                item.getTotalPrice()
                                        )
                                        .note(
                                                item.getNote()
                                        )
                                        .build()
                        )
                        .toList();

        return OrderDetailResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .tableId(order.getTableId())
                .source(order.getSource())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(items)
                .build();
    }
}