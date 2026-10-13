package vn.edu.ute.milktea.service.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.AdminDto;
import vn.edu.ute.milktea.dto.SettingsDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.settings.GlobalSettings;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.settings.GlobalSettingsRepository;
import vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MiB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final Cloudinary cloudinary;
    private final ProductRepository productRepository;
    private final GlobalSettingsRepository settingsRepository;
    private final AccountRepository accountRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.cloudinary.api-key:}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret:}")
    private String apiSecret;

    public boolean isCloudinaryConfigured() {
        return cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
    }

    private void ensureCloudinaryReady() {
        if (!isCloudinaryConfigured()) {
            log.error(
                    "Cloudinary chưa được cấu hình. Thiếu biến môi trường CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY hoặc CLOUDINARY_API_SECRET.");
            throw BusinessException.badRequest(ErrorCode.IMAGE_UPLOAD_FAILED,
                    "Dịch vụ Cloudinary chưa được cấu hình. Vui lòng thiết lập biến môi trường CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY và CLOUDINARY_API_SECRET trước khi tải ảnh.");
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "File ảnh không được để trống");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED,
                    "Kích thước file ảnh vượt quá giới hạn 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED,
                    "Chỉ chấp nhận định dạng ảnh JPEG, PNG hoặc WebP");
        }

        // Kiểm tra magic bytes thực tế của file để chống giả mạo extension
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read < 4) {
                throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Nội dung file ảnh không hợp lệ");
            }

            boolean isJpeg = (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            boolean isPng = (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47;
            boolean isWebp = read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';

            if (!isJpeg && !isPng && !isWebp) {
                throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED,
                        "Tập tin không phải định dạng ảnh hợp lệ (chữ ký nhị phân không khớp)");
            }
        } catch (IOException e) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Không thể đọc nội dung file ảnh");
        }
    }

    // ==========================================
    // 1. UPLOAD ẢNH SẢN PHẨM / TOPPING
    // ==========================================
    @Transactional
    public AdminDto.ProductImageView uploadProductImage(Long productId, MultipartFile file) {
        validateImageFile(file);
        ensureCloudinaryReady();

        Product product = productRepository.findById(productId)
                .orElseThrow(
                        () -> BusinessException.notFound(ErrorCode.PRODUCT_UNAVAILABLE, "Không tìm thấy món uống"));

        String oldPublicId = product.getImagePublicId();
        String secureUrl;
        String publicId;

        String customPublicId = "milktea/products/prod_" + productId + "_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8);
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", customPublicId,
                    "resource_type", "image",
                    "overwrite", true));
            secureUrl = (String) uploadResult.get("secure_url");
            publicId = (String) uploadResult.get("public_id");
        } catch (IOException e) {
            log.error("Lỗi khi tải ảnh lên Cloudinary cho món id={}: {}", productId, e.getMessage());
            throw BusinessException.badRequest(ErrorCode.IMAGE_UPLOAD_FAILED,
                    "Không thể tải ảnh lên Cloudinary: " + e.getMessage());
        }

        // Lưu DB với cơ chế rollback ảnh mới nếu DB save thất bại (tránh ảnh mồ côi)
        try {
            product.setImageUrl(secureUrl);
            product.setImagePublicId(publicId);
            productRepository.save(product);
        } catch (Exception dbEx) {
            log.error("Lưu DB thất bại sau khi upload Cloudinary cho món id={}. Dọn dẹp ảnh mồ côi publicId={}",
                    productId, publicId);
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            } catch (Exception destroyEx) {
                log.warn("Không thể dọn dẹp ảnh mồ côi publicId={}: {}", publicId, destroyEx.getMessage());
            }
            throw dbEx;
        }

        // Xóa ảnh cũ an toàn sau khi lưu DB thành công (chỉ xóa nếu không bản ghi nào
        // khác dùng chung)
        if (oldPublicId != null && !oldPublicId.isBlank() && !oldPublicId.equals(publicId)) {
            long sharedCount = productRepository.countByImagePublicId(oldPublicId);
            if (sharedCount <= 0) {
                try {
                    cloudinary.uploader().destroy(oldPublicId, ObjectUtils.emptyMap());
                    log.info("Đã xóa ảnh cũ trên Cloudinary publicId={}", oldPublicId);
                } catch (Exception e) {
                    log.warn("Không thể xóa ảnh cũ publicId={}: {}", oldPublicId, e.getMessage());
                }
            }
        }

        // Phát sự kiện WebSocket CATALOG_CHANGED after-commit để client tự làm mới ảnh
        // ngay lập tức
        realtimeEventPublisher.publishAfterCommit("/topic/admin", "CATALOG_CHANGED", productId.toString(), null, "1",
                Map.of("action", "IMAGE_UPDATED", "productId", productId, "imageUrl", secureUrl));
        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "CATALOG_CHANGED", productId.toString(), null, "1",
                Map.of("action", "IMAGE_UPDATED", "productId", productId, "imageUrl", secureUrl));

        return AdminDto.ProductImageView.builder()
                .imageUrl(secureUrl)
                .imagePublicId(publicId)
                .build();
    }

    // ==========================================
    // 2. UPLOAD LOGO QUÁN (STORE BRANDING)
    // ==========================================
    @Transactional
    public SettingsDto.LogoImageView uploadLogoImage(MultipartFile file, Long adminId) {
        validateImageFile(file);
        ensureCloudinaryReady();

        GlobalSettings settings = settingsRepository.findById(1)
                .orElseGet(() -> GlobalSettings.builder()
                        .id(1)
                        .shopName("MilkTea Quán 01")
                        .address("Số 1 Võ Văn Ngân, TP. Thủ Đức")
                        .modifiedAt(Instant.now())
                        .build());

        String oldPublicId = settings.getLogoPublicId();
        String secureUrl;
        String publicId;

        String customPublicId = "milktea/branding/logo_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8);
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", customPublicId,
                    "resource_type", "image",
                    "overwrite", true));
            secureUrl = (String) uploadResult.get("secure_url");
            publicId = (String) uploadResult.get("public_id");
        } catch (IOException e) {
            log.error("Lỗi khi tải logo quán lên Cloudinary: {}", e.getMessage());
            throw BusinessException.badRequest(ErrorCode.IMAGE_UPLOAD_FAILED,
                    "Không thể tải logo lên Cloudinary: " + e.getMessage());
        }

        // Lưu DB và cleanup ảnh mới nếu thất bại
        try {
            settings.setLogoUrl(secureUrl);
            settings.setLogoPublicId(publicId);
            settings.setModifiedAt(Instant.now());
            if (adminId != null) {
                accountRepository.findById(adminId).ifPresent(settings::setModifiedBy);
            }
            settingsRepository.save(settings);
        } catch (Exception dbEx) {
            log.error("Lưu DB thất bại sau khi upload logo. Dọn dẹp ảnh mồ côi publicId={}", publicId);
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            } catch (Exception destroyEx) {
                log.warn("Không thể dọn dẹp logo mồ côi: {}", destroyEx.getMessage());
            }
            throw dbEx;
        }

        // Xóa logo cũ an toàn
        if (oldPublicId != null && !oldPublicId.isBlank() && !oldPublicId.equals(publicId)) {
            try {
                cloudinary.uploader().destroy(oldPublicId, ObjectUtils.emptyMap());
                log.info("Đã xóa logo cũ trên Cloudinary publicId={}", oldPublicId);
            } catch (Exception e) {
                log.warn("Không thể xóa logo cũ publicId={}: {}", oldPublicId, e.getMessage());
            }
        }

        // Phát sự kiện WebSocket SETTINGS_CHANGED after-commit để header/navbar trên
        // mọi màn hình cập nhật logo ngay lập tức
        realtimeEventPublisher.publishAfterCommit("/topic/admin", "SETTINGS_CHANGED", "1", null, "1",
                Map.of("action", "LOGO_UPDATED", "logoUrl", secureUrl));
        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "SETTINGS_CHANGED", "1", null, "1",
                Map.of("action", "LOGO_UPDATED", "logoUrl", secureUrl));

        return SettingsDto.LogoImageView.builder()
                .logoUrl(secureUrl)
                .logoPublicId(publicId)
                .build();
    }

    // ==========================================
    // 3. UPLOAD AVATAR TÀI KHOẢN
    // ==========================================
    @Transactional
    public String uploadAvatarImage(Long accountId, MultipartFile file) {
        validateImageFile(file);
        ensureCloudinaryReady();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy tài khoản"));

        String oldPublicId = account.getAvatarPublicId();
        String secureUrl;
        String publicId;

        String customPublicId = "milktea/avatars/user_" + accountId + "_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8);
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", customPublicId,
                    "resource_type", "image",
                    "overwrite", true));
            secureUrl = (String) uploadResult.get("secure_url");
            publicId = (String) uploadResult.get("public_id");
        } catch (IOException e) {
            log.error("Lỗi khi tải avatar lên Cloudinary cho tài khoản id={}: {}", accountId, e.getMessage());
            throw BusinessException.badRequest(ErrorCode.IMAGE_UPLOAD_FAILED,
                    "Không thể tải avatar lên Cloudinary: " + e.getMessage());
        }

        try {
            account.setAvatarUrl(secureUrl);
            account.setAvatarPublicId(publicId);
            accountRepository.save(account);
        } catch (Exception dbEx) {
            log.error("Lưu DB thất bại sau khi upload avatar. Dọn dẹp ảnh mồ côi publicId={}", publicId);
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            } catch (Exception destroyEx) {
                log.warn("Không thể dọn dẹp avatar mồ côi: {}", destroyEx.getMessage());
            }
            throw dbEx;
        }

        if (oldPublicId != null && !oldPublicId.isBlank() && !oldPublicId.equals(publicId)) {
            try {
                cloudinary.uploader().destroy(oldPublicId, ObjectUtils.emptyMap());
            } catch (Exception ignored) {
            }
        }

        realtimeEventPublisher.publishAfterCommit("/topic/admin", "STAFF_CHANGED", accountId.toString(), null, "1",
                Map.of("action", "AVATAR_UPDATED", "accountId", accountId, "avatarUrl", secureUrl));

        return secureUrl;
    }
}
