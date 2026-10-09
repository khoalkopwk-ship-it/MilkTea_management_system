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
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;

import java.io.IOException;
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

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Transactional
    public AdminDto.ProductImageView uploadProductImage(Long productId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "File ảnh không được để trống");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Kích thước file ảnh vượt quá 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Chỉ chấp nhận định dạng ảnh JPEG, PNG hoặc WebP");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.PRODUCT_UNAVAILABLE, "Không tìm thấy món uống"));

        String oldPublicId = product.getImagePublicId();
        String secureUrl;
        String publicId;

        // Kiểm tra xem Cloudinary có được cấu hình thực tế hay không
        if (cloudName != null && !cloudName.isBlank()) {
            try {
                String customPublicId = "milktea/products/prod_" + productId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
                Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                        "public_id", customPublicId,
                        "resource_type", "image"
                ));
                secureUrl = (String) uploadResult.get("secure_url");
                publicId = (String) uploadResult.get("public_id");
            } catch (IOException e) {
                log.error("Lỗi khi tải ảnh lên Cloudinary cho món id={}: {}", productId, e.getMessage());
                throw BusinessException.badRequest(ErrorCode.IMAGE_UPLOAD_FAILED, "Không thể tải ảnh lên dịch vụ Cloudinary: " + e.getMessage());
            }
        } else {
            // Môi trường dev/test khi chưa cấu hình key Cloudinary
            log.warn("Cloudinary chưa cấu hình CLOUDINARY_CLOUD_NAME. Sử dụng URL placeholder an toàn cho dev/test.");
            publicId = "milktea/products/simulated_" + productId + "_" + System.currentTimeMillis();
            secureUrl = "https://images.unsplash.com/photo-1558857563-b37cf5efee6a?w=400&q=80"; // Ảnh trà sữa chất lượng cao
        }

        // Lưu thông tin ảnh mới vào DB
        product.setImageUrl(secureUrl);
        product.setImagePublicId(publicId);
        productRepository.save(product);

        // Xóa ảnh cũ trên Cloudinary best-effort sau khi DB lưu thành công
        if (oldPublicId != null && !oldPublicId.isBlank() && cloudName != null && !cloudName.isBlank()) {
            try {
                cloudinary.uploader().destroy(oldPublicId, ObjectUtils.emptyMap());
                log.info("Đã xóa ảnh cũ trên Cloudinary publicId={}", oldPublicId);
            } catch (Exception e) {
                log.warn("Không thể xóa ảnh cũ trên Cloudinary publicId={}: {}", oldPublicId, e.getMessage());
            }
        }

        return AdminDto.ProductImageView.builder()
                .imageUrl(secureUrl)
                .imagePublicId(publicId)
                .build();
    }
}
