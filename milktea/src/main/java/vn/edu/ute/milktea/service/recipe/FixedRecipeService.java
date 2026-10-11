
package vn.edu.ute.milktea.service.recipe;

import org.springframework.stereotype.Service;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class FixedRecipeService {

    // Công thức mẫu, cần xác nhận lại lượng thực tế.
    // Key: Tên món | Size
    // Value: Tên nguyên liệu -> Số lượng cố định cho 1 món
    private static final Map<String, Map<String, BigDecimal>> RECIPES =
            Map.of(
                "Trà Sữa Truyền Thống|M", Map.of(
                    "Trà Đen", new BigDecimal("0.015"),
                    "Sữa Đặc", new BigDecimal("0.030"),
                    "Đường Cát", new BigDecimal("0.020"),
                    "Ly Nhựa", BigDecimal.ONE
                ),

                "Trà Sữa Truyền Thống|L", Map.of(
                    "Trà Đen", new BigDecimal("0.020"),
                    "Sữa Đặc", new BigDecimal("0.040"),
                    "Đường Cát", new BigDecimal("0.025"),
                    "Ly Nhựa", BigDecimal.ONE
                ),

                "Trà Đào Cam Sả|M", Map.of(
                    "Trà Đen", new BigDecimal("0.015"),
                    "Đường Cát", new BigDecimal("0.020"),
                    "Ly Nhựa", BigDecimal.ONE
                ),

                "Trân Châu Đen|M", Map.of(
                    "Trân Châu Nấu Chín", BigDecimal.ONE
                )
            );

    public BigDecimal getQuantity(
            String productName,
            String size,
            String materialName) {

        String key = productName + "|" + size;
        Map<String, BigDecimal> ingredients = RECIPES.get(key);

        if (ingredients == null) {
            throw BusinessException.conflict(
                ErrorCode.VALIDATION_FAILED,
                "Chưa cấu hình công thức cố định: " + key
            );
        }

        BigDecimal quantity = ingredients.get(materialName);

        if (quantity == null || quantity.signum() <= 0) {
            throw BusinessException.conflict(
                ErrorCode.VALIDATION_FAILED,
                "Nguyên liệu " + materialName
                    + " không thuộc công thức " + key
            );
        }

        return quantity;
    }

    public Map<String, BigDecimal> getIngredients(
            String productName,
            String size) {

        String key = productName + "|" + size;
        Map<String, BigDecimal> ingredients = RECIPES.get(key);

        if (ingredients == null) {
            throw BusinessException.conflict(
                ErrorCode.VALIDATION_FAILED,
                "Chưa cấu hình công thức cố định: " + key
            );
        }

        return ingredients;
    }
}
