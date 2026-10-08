package vn.edu.ute.milktea.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.config.DataInitializer;
import vn.edu.ute.milktea.dto.auth.AuthDto;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.security.JwtService;
import vn.edu.ute.milktea.service.auth.AuthService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AuthServiceTest {

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        dataInitializer.run();
    }

    @Test
    @DisplayName("Đăng nhập thành công với 4 tài khoản demo và đúng phân quyền")
    void testLoginDemoAccounts() {
        // ADMIN
        var adminRes = authService.login(AuthDto.LoginRequest.builder()
                .email("admin@milktea.vn")
                .password("Password@123")
                .build());
        assertNotNull(adminRes.getToken());
        assertEquals("admin@milktea.vn", adminRes.getUser().getEmail());
        assertEquals(Role.ADMIN, adminRes.getUser().getRole());

        // Verify claims
        Map<String, Object> claims = jwtService.validateAndExtractClaims(adminRes.getToken());
        assertNotNull(claims);
        assertEquals("admin@milktea.vn", claims.get("email"));
        assertEquals("ADMIN", claims.get("role"));

        // CASHIER
        var cashierRes = authService.login(AuthDto.LoginRequest.builder()
                .email("cashier@milktea.vn")
                .password("Password@123")
                .build());
        assertEquals(Role.CASHIER, cashierRes.getUser().getRole());

        // KITCHEN
        var kitchenRes = authService.login(AuthDto.LoginRequest.builder()
                .email("kitchen@milktea.vn")
                .password("Password@123")
                .build());
        assertEquals(Role.KITCHEN, kitchenRes.getUser().getRole());

        // CUSTOMER
        var customerRes = authService.login(AuthDto.LoginRequest.builder()
                .email("customer@milktea.vn")
                .password("Password@123")
                .build());
        assertEquals(Role.CUSTOMER, customerRes.getUser().getRole());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi sai mật khẩu")
    void testLoginWrongPassword() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.login(AuthDto.LoginRequest.builder()
                        .email("admin@milktea.vn")
                        .password("WrongPassword")
                        .build()));
        assertEquals(ErrorCode.AUTH_INVALID_CREDENTIALS, ex.getErrorCode());
    }

    @Test
    @DisplayName("Đăng xuất vô hiệu hóa token bằng cách tăng TokenVersion")
    void testLogoutIncrementsTokenVersion() {
        var loginRes = authService.login(AuthDto.LoginRequest.builder()
                .email("cashier@milktea.vn")
                .password("Password@123")
                .build());

        authService.logout(loginRes.getUser().getId());

        // Đăng nhập lại để kiểm tra TokenVersion mới
        var newLoginRes = authService.login(AuthDto.LoginRequest.builder()
                .email("cashier@milktea.vn")
                .password("Password@123")
                .build());

        var oldClaims = jwtService.validateAndExtractClaims(loginRes.getToken());
        var newClaims = jwtService.validateAndExtractClaims(newLoginRes.getToken());

        long oldVer = ((Number) oldClaims.get("tokenVersion")).longValue();
        long newVer = ((Number) newClaims.get("tokenVersion")).longValue();

        assertTrue(newVer > oldVer, "TokenVersion phải tăng lên sau khi đăng xuất");
    }
}
