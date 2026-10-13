package vn.edu.ute.milktea.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.SettingsDto;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.settings.SettingsService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final vn.edu.ute.milktea.service.media.ProductImageService productImageService;

    @GetMapping("/public/shop-info")
    public ResponseEntity<ApiResponse<SettingsDto.ShopInfoResponse>> getShopInfo() {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.getShopInfo()));
    }

    @GetMapping("/admin/settings")
    public ResponseEntity<ApiResponse<SettingsDto.ShopInfoResponse>> getAdminSettings() {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.getShopInfo()));
    }

    @PostMapping(value = "/admin/settings/logo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SettingsDto.LogoImageView>> uploadLogo(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal CurrentActor actor) {
        Long adminId = actor != null ? actor.getAccountId() : null;
        return ResponseEntity.ok(ApiResponse.ok(productImageService.uploadLogoImage(file, adminId)));
    }

    @PutMapping("/admin/settings")
    public ResponseEntity<ApiResponse<Void>> updateSettings(
            @Valid @RequestBody SettingsDto.UpdateSettingsRequest request,
            @AuthenticationPrincipal CurrentActor actor) {

        Long adminId = actor != null ? actor.getAccountId() : null;
        settingsService.updateSettings(request, adminId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
