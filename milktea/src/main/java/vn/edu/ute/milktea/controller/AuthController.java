package vn.edu.ute.milktea.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.AuthDto;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.security.JwtAuthenticationFilter;
import vn.edu.ute.milktea.service.auth.AuthService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request,
            HttpServletResponse response) {

        AuthDto.AuthResponse authResponse = authService.login(request);

        // Lưu JWT vào cookie HttpOnly theo author_rule.md
        Cookie cookie = new Cookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, authResponse.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60); // 1 hour
        response.addCookie(cookie);

        return ResponseEntity.ok(ApiResponse.ok(authResponse));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> register(
            @Valid @RequestBody AuthDto.RegisterRequest request,
            HttpServletResponse response) {

        AuthDto.AuthResponse authResponse = authService.register(request);

        Cookie cookie = new Cookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, authResponse.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60);
        response.addCookie(cookie);

        return ResponseEntity.status(201).body(ApiResponse.ok(authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CurrentActor actor,
            HttpServletResponse response) {

        if (actor != null && actor.getAccountId() != null) {
            authService.logout(actor.getAccountId());
        }

        // Xóa cookie JWT
        Cookie cookie = new Cookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/password-reset")
    public ResponseEntity<ApiResponse<Map<String, String>>> requestReset(
            @Valid @RequestBody AuthDto.PasswordResetRequest request) {
        String resetToken = authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Yêu cầu đặt lại mật khẩu đã được tiếp nhận", "tokenPreview", resetToken)));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Map<String, String>>> confirmReset(
            @Valid @RequestBody AuthDto.PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đặt lại mật khẩu thành công")));
    }

    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<Map<String, String>>> getCsrf(HttpServletRequest request) {
        // Cung cấp token CSRF chuẩn cho JavaScript browser mutations
        String token = UUID.randomUUID().toString();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("headerName", "X-CSRF-TOKEN", "token", token)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDto.UserInfo>> getMe(
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor == null || actor.getAccountId() == null) {
            throw vn.edu.ute.milktea.common.BusinessException.unauthorized(vn.edu.ute.milktea.common.ErrorCode.ACCESS_DENIED, "Vui lòng đăng nhập");
        }
        return ResponseEntity.ok(ApiResponse.ok(authService.getProfile(actor.getAccountId())));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<AuthDto.UserInfo>> updateProfile(
            @AuthenticationPrincipal CurrentActor actor,
            @Valid @RequestBody AuthDto.UpdateProfileRequest request) {
        if (actor == null || actor.getAccountId() == null) {
            throw vn.edu.ute.milktea.common.BusinessException.unauthorized(vn.edu.ute.milktea.common.ErrorCode.ACCESS_DENIED, "Vui lòng đăng nhập");
        }
        return ResponseEntity.ok(ApiResponse.ok(authService.updateProfile(actor.getAccountId(), request)));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CurrentActor actor,
            @Valid @RequestBody AuthDto.ChangePasswordRequest request) {
        if (actor == null || actor.getAccountId() == null) {
            throw vn.edu.ute.milktea.common.BusinessException.unauthorized(vn.edu.ute.milktea.common.ErrorCode.ACCESS_DENIED, "Vui lòng đăng nhập");
        }
        authService.changePassword(actor.getAccountId(), request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
