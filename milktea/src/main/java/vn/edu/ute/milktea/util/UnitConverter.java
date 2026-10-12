package vn.edu.ute.milktea.util;

import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;

/**
 * Tiện ích quy đổi đơn vị đo lường nguyên vật liệu trong hệ thống MilkTea.
 * Tuân thủ quy tắc:
 * - Chỉ quy đổi trực tiếp trong cùng nhóm đơn vị (kg <-> g, l <-> ml).
 * - Không tự quy đổi khối lượng sang thể tích bằng hệ số chung (vd kg -> ml).
 *   Tỷ lệ khối lượng sang thể tích là kết quả sản xuất thực tế của từng mẻ sơ chế.
 */
public final class UnitConverter {

    private UnitConverter() {}

    private static final Set<String> MASS_UNITS = Set.of("kg", "g", "gram", "kilogram");
    private static final Set<String> VOLUME_UNITS = Set.of("l", "ml", "lit", "mililit", "lít");
    private static final Set<String> COUNT_UNITS = Set.of("cai", "cái", "ly", "hop", "hộp", "chai", "suat", "suất");

    public enum UnitDimension {
        MASS,
        VOLUME,
        COUNT,
        UNKNOWN
    }

    public static UnitDimension getDimension(String unit) {
        if (unit == null || unit.isBlank()) {
            return UnitDimension.UNKNOWN;
        }
        String u = normalizeUnitName(unit);
        if (MASS_UNITS.contains(u)) {
            return UnitDimension.MASS;
        }
        if (VOLUME_UNITS.contains(u)) {
            return UnitDimension.VOLUME;
        }
        if (COUNT_UNITS.contains(u)) {
            return UnitDimension.COUNT;
        }
        return UnitDimension.UNKNOWN;
    }

    public static boolean isCompatible(String unit1, String unit2) {
        if (unit1 == null || unit2 == null) return false;
        String u1 = normalizeUnitName(unit1);
        String u2 = normalizeUnitName(unit2);
        if (u1.equals(u2)) return true;

        UnitDimension d1 = getDimension(u1);
        UnitDimension d2 = getDimension(u2);
        return d1 != UnitDimension.UNKNOWN && d1 == d2;
    }

    /**
     * Quy đổi số lượng từ đơn vị nguồn sang đơn vị đích trong cùng nhóm đơn vị.
     *
     * @param quantity Số lượng cần quy đổi
     * @param fromUnit Đơn vị nguồn
     * @param toUnit   Đơn vị đích
     * @return Số lượng đã quy đổi về toUnit
     */
    public static BigDecimal convert(BigDecimal quantity, String fromUnit, String toUnit) {
        if (quantity == null) {
            return BigDecimal.ZERO;
        }
        if (fromUnit == null || toUnit == null) {
            return quantity;
        }
        String uFrom = normalizeUnitName(fromUnit);
        String uTo = normalizeUnitName(toUnit);
        if (uFrom.equals(uTo)) {
            return quantity;
        }

        UnitDimension dimFrom = getDimension(uFrom);
        UnitDimension dimTo = getDimension(uTo);

        if (dimFrom != dimTo || dimFrom == UnitDimension.UNKNOWN) {
            throw BusinessException.badRequest(
                    ErrorCode.VALIDATION_FAILED,
                    String.format("Không thể quy đổi giữa đơn vị '%s' và '%s'. " +
                                    "Quy đổi giữa khối lượng và thể tích phải qua kết quả mẻ sơ chế thực tế.",
                            fromUnit, toUnit)
            );
        }

        if (dimFrom == UnitDimension.MASS) {
            // kg <-> g
            BigDecimal inGrams = toGrams(quantity, uFrom);
            return fromGrams(inGrams, uTo);
        }

        if (dimFrom == UnitDimension.VOLUME) {
            // l <-> ml
            BigDecimal inMl = toMilliliters(quantity, uFrom);
            return fromMilliliters(inMl, uTo);
        }

        // Discrete count units (cai, ly, etc.) - cannot convert between different names
        if (!uFrom.equals(uTo)) {
            throw BusinessException.badRequest(
                    ErrorCode.VALIDATION_FAILED,
                    String.format("Không thể quy đổi giữa các đơn vị đếm '%s' và '%s'.", fromUnit, toUnit)
            );
        }
        return quantity;
    }

    private static String normalizeUnitName(String unit) {
        return unit.trim().toLowerCase(Locale.ROOT);
    }

    private static BigDecimal toGrams(BigDecimal qty, String unit) {
        if ("kg".equals(unit) || "kilogram".equals(unit)) {
            return qty.multiply(BigDecimal.valueOf(1000));
        }
        return qty; // "g"
    }

    private static BigDecimal fromGrams(BigDecimal grams, String targetUnit) {
        if ("kg".equals(targetUnit) || "kilogram".equals(targetUnit)) {
            return grams.divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        }
        return grams; // "g"
    }

    private static BigDecimal toMilliliters(BigDecimal qty, String unit) {
        if ("l".equals(unit) || "lit".equals(unit) || "lít".equals(unit)) {
            return qty.multiply(BigDecimal.valueOf(1000));
        }
        return qty; // "ml"
    }

    private static BigDecimal fromMilliliters(BigDecimal ml, String targetUnit) {
        if ("l".equals(targetUnit) || "lit".equals(targetUnit) || "lít".equals(targetUnit)) {
            return ml.divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        }
        return ml; // "ml"
    }
}
