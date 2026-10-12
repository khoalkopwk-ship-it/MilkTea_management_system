package vn.edu.ute.milktea.service.table;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.CartDto;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.table.SessionCart;
import vn.edu.ute.milktea.entity.table.SessionCartItem;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.table.SessionCartItemRepository;
import vn.edu.ute.milktea.repository.table.SessionCartRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionCartService {

    private final SessionCartRepository cartRepository;
    private final SessionCartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher realtimeEventPublisher;

    @Transactional(readOnly = true)
    public CartDto.CartResponse getCart(Long sessionId) {
        SessionCart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_SESSION_CLOSED, "Không tìm thấy giỏ hàng cho phiên"));

        List<SessionCartItem> items = cartItemRepository.findByCartId(cart.getId());
        return mapToCartResponse(cart, items);
    }

    @Transactional
    public CartDto.CartResponse replaceCart(Long sessionId, CartDto.ReplaceCartRequest request, String ifMatchVersion) {
        SessionCart cart = cartRepository.findBySessionIdWithLock(sessionId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_SESSION_CLOSED, "Phiên bàn đã kết thúc"));

        if (ifMatchVersion != null && !ifMatchVersion.isBlank()) {
            String cleanVersion = ifMatchVersion.replace("\"", "").trim();
            if (!String.valueOf(cart.getVersion()).equals(cleanVersion)) {
                throw BusinessException.conflict(ErrorCode.CART_VERSION_CONFLICT,
                        "Giỏ hàng đã được cập nhật từ thiết bị khác. Vui lòng tải lại.");
            }
        }

        cartItemRepository.deleteByCartId(cart.getId());

        List<SessionCartItem> newItems = new ArrayList<>();
        if (request.getItems() != null) {
            for (var itemReq : request.getItems()) {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> BusinessException.badRequest(ErrorCode.PRODUCT_UNAVAILABLE,
                                "Món uống không tồn tại: ID " + itemReq.getProductId()));

                if (!Boolean.TRUE.equals(product.getActive())) {
                    throw BusinessException.badRequest(ErrorCode.PRODUCT_UNAVAILABLE,
                            "Món uống đã ngừng phục vụ: " + product.getName());
                }

                if (itemReq.getToppingIds() != null && !itemReq.getToppingIds().isEmpty()) {
                    if (product.getCategory() != null && !Boolean.TRUE.equals(product.getCategory().getAllowTopping())) {
                        throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED,
                                "Danh mục \"" + product.getCategory().getName() + "\" không cho phép chọn topping đi kèm");
                    }
                }

                String toppingIdsStr = (itemReq.getToppingIds() != null && !itemReq.getToppingIds().isEmpty())
                        ? itemReq.getToppingIds().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))
                        : null;

                SessionCartItem item = SessionCartItem.builder()
                        .cart(cart)
                        .product(product)
                        .quantity(itemReq.getQuantity())
                        .note(itemReq.getNote())
                        .toppingIds(toppingIdsStr)
                        .build();
                newItems.add(item);
            }
            newItems = cartItemRepository.saveAll(newItems);
        }

        cart.setVersion(cart.getVersion() + 1);
        cart.setModifiedAt(Instant.now());
        cartRepository.save(cart);

        realtimeEventPublisher.publishAfterCommit("/topic/table-sessions/" + sessionId, "CART_CHANGED", cart.getId().toString(), sessionId.toString(), String.valueOf(cart.getVersion()), null);

        return mapToCartResponse(cart, newItems);
    }

    private CartDto.CartResponse mapToCartResponse(SessionCart cart, List<SessionCartItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        List<CartDto.CartItemResponse> itemResponses = new ArrayList<>();

        for (SessionCartItem item : items) {
            Product p = item.getProduct();
            List<CartDto.ToppingItemResponse> toppingsList = new ArrayList<>();
            BigDecimal toppingSum = BigDecimal.ZERO;

            if (item.getToppingIds() != null && !item.getToppingIds().isBlank()) {
                String[] tIds = item.getToppingIds().split(",");
                for (String tIdStr : tIds) {
                    try {
                        Long tId = Long.parseLong(tIdStr.trim());
                        productRepository.findById(tId).filter(tp -> Boolean.TRUE.equals(tp.getActive())).ifPresent(tp -> {
                            toppingsList.add(CartDto.ToppingItemResponse.builder()
                                    .id(tp.getId())
                                    .name(tp.getName())
                                    .price(tp.getPrice())
                                    .build());
                        });
                    } catch (NumberFormatException ignored) {}
                }
                for (var t : toppingsList) {
                    toppingSum = toppingSum.add(t.getPrice());
                }
            }

            BigDecimal unitPriceWithTopping = p.getPrice().add(toppingSum);
            BigDecimal lineTotal = unitPriceWithTopping.multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(lineTotal);

            itemResponses.add(CartDto.CartItemResponse.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .size(p.getSize())
                    .unitPrice(p.getPrice())
                    .quantity(item.getQuantity())
                    .note(item.getNote())
                    .lineTotal(lineTotal)
                    .toppings(toppingsList)
                    .toppingIds(toppingsList.stream().map(CartDto.ToppingItemResponse::getId).toList())
                    .build());
        }

        return CartDto.CartResponse.builder()
                .sessionId(cart.getSession().getId())
                .version(String.valueOf(cart.getVersion()))
                .items(itemResponses)
                .totalAmount(total)
                .build();
    }
}
