package vn.edu.ute.milktea.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class BranchDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchResponse {
        private Long id;
        private String code;
        private String name;
        private String address;
        private String phone;
        private String openingHours;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateAvailabilityRequest {
        @NotNull(message = "Trạng thái phục vụ không được để trống")
        private Boolean available;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateBranchRequest {
        @jakarta.validation.constraints.NotBlank(message = "Mã định danh chi nhánh không được để trống")
        private String code;
        @jakarta.validation.constraints.NotBlank(message = "Tên chi nhánh không được để trống")
        private String name;
        @jakarta.validation.constraints.NotBlank(message = "Địa chỉ không được để trống")
        private String address;
        private String phone;
        private String openingHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateBranchRequest {
        @jakarta.validation.constraints.NotBlank(message = "Tên chi nhánh không được để trống")
        private String name;
        @jakarta.validation.constraints.NotBlank(message = "Địa chỉ không được để trống")
        private String address;
        private String phone;
        private String openingHours;
    }
}
