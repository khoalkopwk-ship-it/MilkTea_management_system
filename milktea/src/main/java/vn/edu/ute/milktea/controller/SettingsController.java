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

    @GetMapping("/public/shop-info")
    public ResponseEntity<ApiResponse<SettingsDto.ShopInfoResponse>> getShopInfo() {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.getShopInfo()));
    }

    @GetMapping("/admin/settings")
    public ResponseEntity<ApiResponse<SettingsDto.ShopInfoResponse>> getAdminSettings() {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.getShopInfo()));
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
