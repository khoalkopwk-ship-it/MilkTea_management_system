package vn.edu.ute.milktea.service.review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.ReviewDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.order.Invoice;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.order.OrderItem;
import vn.edu.ute.milktea.entity.order.OrderStatus;
import vn.edu.ute.milktea.entity.review.ProductReview;
import vn.edu.ute.milktea.entity.review.ReviewMedia;
import vn.edu.ute.milktea.entity.review.ReviewMediaType;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.order.InvoiceRepository;
import vn.edu.ute.milktea.repository.order.OrderItemRepository;
import vn.edu.ute.milktea.repository.payment.PaymentRepository;
import vn.edu.ute.milktea.repository.review.ProductReviewRepository;
import vn.edu.ute.milktea.repository.review.ReviewMediaRepository;
import vn.edu.ute.milktea.security.CurrentActor;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Service quản lý Đánh giá, Bình luận & Media (CUS-06).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private static final Set<String> ALLOWED_IMAGE_MIME = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_VIDEO_MIME = Set.of("video/mp4", "video/webm");

    private final ProductReviewRepository productReviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final OrderItemRepository orderItemRepository;
    private final AccountRepository accountRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public ReviewDto.ReviewResponse createReview(ReviewDto.CreateReviewRequest req, CurrentActor actor) {
        validateCustomerActor(actor);

        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Điểm đánh giá phải từ 1 đến 5 sao");
        }

        String content = req.getContent() != null ? req.getContent().trim() : "";
        if (content.length() < 50) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED,
                    "Nội dung đánh giá phải có độ dài tối thiểu 50 ký tự (sau khi loại bỏ khoảng trắng)");
        }
        if (content.length() > 1000) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED,
                    "Nội dung đánh giá không được vượt quá 1000 ký tự");
        }

        OrderItem orderItem = orderItemRepository.findById(req.getOrderItemId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy chi tiết đơn hàng"));

        if (productReviewRepository.existsByOrderItemId(orderItem.getId())) {
            throw BusinessException.conflict(ErrorCode.IDEMPOTENCY_CONFLICT,
                    "Món này trong đơn hàng đã được đánh giá trước đó");
        }

        Order order = orderItem.getOrder();
        if (order.getAccount() == null || !order.getAccount().getId().equals(actor.getAccountId())) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Bạn không có quyền đánh giá đơn hàng của khách hàng khác");
        }

        if (order.getStatus() != OrderStatus.HOAN_THANH) {
            throw BusinessException.badRequest(ErrorCode.ORDER_STATE_CONFLICT,
                    "Chỉ có thể đánh giá các món trong đơn hàng đã hoàn thành phục vụ");
        }

        Invoice invoice = invoiceRepository.findByOrderId(order.getId())
                .orElseThrow(() -> BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Đơn hàng chưa có hóa đơn"));

        if (!paymentRepository.existsByInvoiceId(invoice.getId())) {
            throw BusinessException.badRequest(ErrorCode.ORDER_STATE_CONFLICT,
                    "Đơn hàng chưa được thanh toán thành công, không thể gửi đánh giá");
        }

        Account customer = accountRepository.findById(actor.getAccountId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy tài khoản khách hàng"));

        ProductReview review = ProductReview.builder()
                .orderItem(orderItem)
                .account(customer)
                .product(orderItem.getProduct())
                .branch(order.getBranch())
                .rating(req.getRating())
                .content(content)
                .hidden(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        if (req.getMediaList() != null && !req.getMediaList().isEmpty()) {
            List<ReviewMedia> mediaEntities = new ArrayList<>();
            for (ReviewDto.MediaItemRequest mReq : req.getMediaList()) {
                if (mReq.getMediaUrl() != null && !mReq.getMediaUrl().isBlank()) {
                    ReviewMedia media = ReviewMedia.builder()
                            .review(review)
                            .url(mReq.getMediaUrl().trim())
                            .mediaType(mReq.getMediaType() != null ? mReq.getMediaType() : ReviewMediaType.IMAGE)
                            .build();
                    mediaEntities.add(media);
                }
            }
            review.setMediaList(mediaEntities);
        }

        ProductReview saved = productReviewRepository.save(review);
        return mapReviewToDto(saved);
    }

    @Transactional(readOnly = true)
    public ReviewDto.ReviewListResponse getProductReviews(Long productId, Pageable pageable) {
        Page<ProductReview> page = productReviewRepository.findByProductIdAndHiddenFalseOrderByCreatedAtDesc(productId, pageable);
        Double avgRating = productReviewRepository.findAverageRatingByProductId(productId);
        long count = productReviewRepository.countByProductIdAndHiddenFalse(productId);

        List<ReviewDto.ReviewResponse> items = page.getContent().stream().map(this::mapReviewToDto).toList();

        return ReviewDto.ReviewListResponse.builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                .totalReviews(count)
                .build();
    }

    @Transactional(readOnly = true)
    public ReviewDto.ReviewListResponse getReviewsForModeration(Long branchId, CurrentActor actor, Pageable pageable) {
        validateModeratorActor(actor, branchId);

        Page<ProductReview> page;
        if (actor.hasRole(Role.ADMIN) && branchId == null) {
            page = productReviewRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            Long targetBranchId = branchId != null ? branchId : actor.getBranchId();
            if (targetBranchId == null) {
                throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Chi nhánh không hợp lệ");
            }
            page = productReviewRepository.findByBranchIdOrderByCreatedAtDesc(targetBranchId, pageable);
        }

        List<ReviewDto.ReviewResponse> items = page.getContent().stream().map(this::mapReviewToDto).toList();

        return ReviewDto.ReviewListResponse.builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Transactional
    public ReviewDto.ReviewResponse moderateReview(Long reviewId, ReviewDto.ModerateReviewRequest req, CurrentActor actor) {
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy đánh giá"));

        validateModeratorActor(actor, review.getBranch().getId());

        if (Boolean.TRUE.equals(req.getHidden())) {
            if (req.getReason() == null || req.getReason().trim().isBlank()) {
                throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Bắt buộc nhập lý do khi ẩn đánh giá");
            }
            Account moderator = accountRepository.findById(actor.getAccountId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy tài khoản quản trị"));

            review.setHidden(true);
            review.setHiddenReason(req.getReason().trim());
            review.setHiddenBy(moderator);
            review.setHiddenAt(Instant.now());
        } else {
            review.setHidden(false);
            review.setHiddenReason(null);
            review.setHiddenBy(null);
            review.setHiddenAt(null);
        }

        review.setUpdatedAt(Instant.now());
        ProductReview saved = productReviewRepository.save(review);
        return mapReviewToDto(saved);
    }

    public void validateMediaFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Tập tin media không được để trống");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Không xác định được định dạng tập tin");
        }
        contentType = contentType.toLowerCase();

        boolean isImage = ALLOWED_IMAGE_MIME.contains(contentType);
        boolean isVideo = ALLOWED_VIDEO_MIME.contains(contentType);

        if (!isImage && !isVideo) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED,
                    "Chỉ hỗ trợ định dạng ảnh (JPEG, PNG, WebP) hoặc video (MP4, WebM)");
        }

        if (isImage) {
            if (file.getSize() > 5 * 1024 * 1024) {
                throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Ảnh không được vượt quá 5MB");
            }
            try (InputStream is = file.getInputStream()) {
                byte[] header = new byte[12];
                int read = is.read(header);
                if (read < 4) {
                    throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Nội dung file không hợp lệ");
                }
                boolean isJpeg = (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
                boolean isPng = (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47;
                boolean isWebp = read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                        && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
                if (!isJpeg && !isPng && !isWebp) {
                    throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED,
                            "Chữ ký nhị phân ảnh không khớp với định dạng cho phép");
                }
            } catch (IOException e) {
                throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Không thể đọc dữ liệu tập tin");
            }
        } else {
            if (file.getSize() > 50 * 1024 * 1024) {
                throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Video không được vượt quá 50MB");
            }
        }
    }

    private void validateCustomerActor(CurrentActor actor) {
        if (actor == null || !actor.isAuthenticated() || actor.getAccountId() == null) {
            throw BusinessException.unauthorized(ErrorCode.ACCESS_DENIED, "Vui lòng đăng nhập để gửi đánh giá sản phẩm");
        }
        if (actor.getRole() != Role.CUSTOMER) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Chỉ tài khoản khách hàng mới được gửi đánh giá");
        }
    }

    private void validateModeratorActor(CurrentActor actor, Long targetBranchId) {
        if (actor == null || !actor.isAuthenticated() || actor.getAccountId() == null) {
            throw BusinessException.unauthorized(ErrorCode.ACCESS_DENIED, "Vui lòng đăng nhập để thực hiện quyền kiểm duyệt");
        }
        if (actor.hasRole(Role.ADMIN)) {
            return;
        }
        if (actor.hasRole(Role.MANAGER)) {
            if (targetBranchId != null && !actor.canAccessBranch(targetBranchId)) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Quản lý chỉ có quyền kiểm duyệt đánh giá thuộc chi nhánh được phân công");
            }
            return;
        }
        throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Bạn không có quyền kiểm duyệt đánh giá");
    }

    private ReviewDto.ReviewResponse mapReviewToDto(ProductReview review) {
        List<ReviewDto.MediaItemResponse> mediaResponses = review.getMediaList() != null
                ? review.getMediaList().stream().map(m -> ReviewDto.MediaItemResponse.builder()
                        .id(m.getId())
                        .mediaUrl(m.getUrl())
                        .mediaType(m.getMediaType())
                        .build()).toList()
                : Collections.emptyList();

        return ReviewDto.ReviewResponse.builder()
                .id(review.getId())
                .orderItemId(review.getOrderItem() != null ? review.getOrderItem().getId() : null)
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                .customerId(review.getAccount() != null ? review.getAccount().getId() : null)
                .customerName(review.getAccount() != null ? review.getAccount().getFullName() : null)
                .branchId(review.getBranch() != null ? review.getBranch().getId() : null)
                .branchName(review.getBranch() != null ? review.getBranch().getName() : null)
                .rating(review.getRating())
                .content(review.getContent())
                .hidden(review.getHidden())
                .hiddenReason(review.getHiddenReason())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .mediaList(mediaResponses)
                .build();
    }
}
