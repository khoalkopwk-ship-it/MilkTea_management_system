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
    private final vn.edu.ute.milktea.service.media.ProductImageService productImageService;

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
    public ResponseEntity<ApiResponse<AuthDto.RegisterResponse>> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {

        AuthDto.RegisterResponse registerResponse = authService.register(request);
        // KHÔNG cấp cookie JWT khi chưa xác thực email
        return ResponseEntity.status(201).body(ApiResponse.ok(registerResponse));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyEmail(
            @Valid @RequestBody AuthDto.VerifyEmailRequest request) {
        String message = authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", message)));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> resendOtp(
            @Valid @RequestBody AuthDto.ResendOtpRequest request) {
        String message = authService.resendRegisterOtp(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", message)));
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
        String message = authService.requestPasswordReset(request);
        // Response an toàn: Không trả token thô ra ngoài API
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", message)));
    }

    @PostMapping("/password-reset/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyPasswordResetOtp(
            @Valid @RequestBody AuthDto.VerifyEmailRequest request) {
        String resetToken = authService.verifyPasswordResetOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "resetToken", resetToken,
                "message", "Xác thực mã OTP thành công. Vui lòng nhập mật khẩu mới."
        )));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Map<String, String>>> confirmReset(
            @Valid @RequestBody AuthDto.PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.")));
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

    @PostMapping(value = "/profile/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProfileAvatar(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor == null || actor.getAccountId() == null) {
            throw vn.edu.ute.milktea.common.BusinessException.unauthorized(vn.edu.ute.milktea.common.ErrorCode.ACCESS_DENIED, "Vui lòng đăng nhập");
        }
        String avatarUrl = productImageService.uploadAvatarImage(actor.getAccountId(), file);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("avatarUrl", avatarUrl)));
    }
}
