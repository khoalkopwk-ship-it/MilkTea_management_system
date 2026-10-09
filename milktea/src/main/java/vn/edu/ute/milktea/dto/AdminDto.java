package vn.edu.ute.milktea.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.inventory.MaterialType;
import vn.edu.ute.milktea.entity.table.TableStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class AdminDto {

    // === TÀI KHOẢN NHÂN VIÊN ===
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateStaffRequest {
        @NotBlank(message = "Email không được để trống")
        private String email;

        @NotBlank(message = "Mật khẩu không được để trống")
        private String password;

        @NotBlank(message = "Họ tên không được để trống")
        private String fullName;

        private String phone;

        @NotNull(message = "Vai trò không được để trống")
        private Role role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStaffRequest {
        private String fullName;
        private String phone;
        private Role role;
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffResponse {
        private Long id;
        private String email;
        private String fullName;
        private String phone;
        private Role role;
        private Boolean active;
        private Instant createdAt;
    }

    // === DANH MỤC ===
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryRequest {
        @NotBlank(message = "Tên danh mục không được để trống")
        private String name;
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResponse {
        private Long id;
        private String name;
        private Boolean active;
    }

    // === SẢN PHẨM ===
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductRequest {
        @NotNull(message = "Danh mục không được để trống")
        private Long categoryId;

        @NotBlank(message = "Tên món không được để trống")
        private String name;

        @NotBlank(message = "Size không được để trống (M, L...)")
        private String size;

        private String description;

        @NotNull(message = "Giá không được để trống")
        @Positive(message = "Giá phải lớn hơn 0")
        private BigDecimal price;

        private String imageUrl;
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductResponse {
        private Long id;
        private Long categoryId;
        private String categoryName;
        private String name;
        private String size;
        private String description;
        private BigDecimal price;
        private String imageUrl;
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImageView {
        private String imageUrl;
        private String imagePublicId;
    }

    // === CÔNG THỨC SẢN PHẨM ===
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeItemDto {
        @NotNull(message = "Nguyên liệu không được để trống")
        private Long materialId;

        private String materialName;
        private String unit;

        @NotNull(message = "Định mức không được để trống")
        @Positive(message = "Định mức phải lớn hơn 0")
        private BigDecimal quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProductRecipeRequest {
        @NotNull(message = "Danh sách nguyên liệu không được để trống")
        private List<RecipeItemDto> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductRecipeResponse {
        private Long productId;
        private String productName;
        private List<RecipeItemDto> ingredients;
    }

    // === BÀN ===
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableRequest {
        @NotBlank(message = "Tên bàn không được để trống")
        private String name;
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableResponse {
        private Long id;
        private String name;
        private String qrCode;
        private TableStatus status;
        private Long activeSessionId;
        private Boolean active;
    }

    // === NGUYÊN LIỆU ===
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialRequest {
        @NotBlank(message = "Tên nguyên liệu không được để trống")
        private String name;

        @NotNull(message = "Loại nguyên liệu không được để trống (THO hoặc SOCHE)")
        private MaterialType type;

        @NotBlank(message = "Đơn vị tính không được để trống")
        private String unit;

        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialResponse {
        private Long id;
        private String name;
        private MaterialType type;
        private String unit;
        private Boolean active;
    }

    // === CÔNG THỨC SƠ CHẾ ===
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrepRecipeItemDto {
        @NotNull(message = "Nguyên liệu đầu vào không được để trống")
        private Long inputMaterialId;
        private String inputMaterialName;
        private String unit;

        @NotNull(message = "Định mức đầu vào không được để trống")
        @Positive(message = "Định mức đầu vào phải lớn hơn 0")
        private BigDecimal standardQuantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePreparationRecipeRequest {
        @NotBlank(message = "Tên công thức không được để trống")
        private String name;

        @NotNull(message = "Bán thành phẩm đầu ra không được để trống")
        private Long outputMaterialId;

        @NotNull(message = "Sản lượng chuẩn không được để trống")
        @Positive(message = "Sản lượng chuẩn phải lớn hơn 0")
        private BigDecimal standardOutputQuantity;

        private List<PrepRecipeItemDto> inputItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreparationRecipeResponse {
        private Long id;
        private String name;
        private Long outputMaterialId;
        private String outputMaterialName;
        private String outputUnit;
        private BigDecimal standardOutputQuantity;
        private List<PrepRecipeItemDto> inputItems;
    }
}
