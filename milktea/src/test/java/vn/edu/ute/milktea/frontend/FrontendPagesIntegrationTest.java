package vn.edu.ute.milktea.frontend;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import vn.edu.ute.milktea.config.DataInitializer;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.security.JwtService;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FrontendPagesIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JwtService jwtService;

    private String cashierToken;
    private String kitchenToken;
    private String adminToken;
    private Product product;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        dataInitializer.run();

        product = productRepository.findAll().stream().findFirst().orElseThrow();

        Account cashier = accountRepository.findByEmail("cashier@milktea.vn").orElseThrow();
        Account kitchen = accountRepository.findByEmail("kitchen@milktea.vn").orElseThrow();
        Account admin = accountRepository.findByEmail("admin@milktea.vn").orElseThrow();

        cashierToken = jwtService.generateToken(cashier);
        kitchenToken = jwtService.generateToken(kitchen);
        adminToken = jwtService.generateToken(admin);
    }

    // ==========================================
    // 1. TRANG KHÁCH HÀNG (PUBLIC)
    // ==========================================
    @Test
    @DisplayName("Khách hàng: Trang chủ/Menu, Cart, Orders, Login tải thành công 200 OK")
    void testPublicPages_Return200() throws Exception {
        // Trang chủ / Menu
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("MilkTea")));

        // Trang menu kèm query nhãn bàn
        mockMvc.perform(get("/menu").param("table", "Ban-01"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("MilkTea")));

        // Giỏ hàng
        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Giỏ Hàng")));

        // Danh sách đơn hàng
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đơn Hàng Của Bạn")));

        // Đăng nhập
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đăng Nhập")));
    }

    // ==========================================
    // 2. CHUYỂN HƯỚNG BẢO MẬT (REDIRECT KHI CHƯA AUTH)
    // ==========================================
    @Test
    @DisplayName("Bảo mật: Trang nhân viên chưa đăng nhập chuyển hướng về /login")
    void testProtectedPages_RedirectWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/cashier"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?redirect=/cashier"));

        mockMvc.perform(get("/kitchen"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?redirect=/kitchen"));

        mockMvc.perform(get("/admin/catalog"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?redirect=/admin/catalog"));

        mockMvc.perform(get("/admin/tables"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?redirect=/admin/tables"));

        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?redirect=/admin/reports"));

        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?redirect=/admin/settings"));
    }

    // ==========================================
    // 3. TRUY CẬP HỢP LỆ VỚI COOKIE / TOKEN
    // ==========================================
    @Test
    @DisplayName("Nhân viên: Thu ngân, Bếp, Admin truy cập thành công giao diện nghiệp vụ")
    void testProtectedPages_AuthorizedAccess() throws Exception {
        // Thu ngân vào /cashier
        mockMvc.perform(get("/cashier")
                        .cookie(new Cookie(vn.edu.ute.milktea.security.JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, cashierToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thu Ngân")));

        // Bếp vào /kitchen
        mockMvc.perform(get("/kitchen")
                        .cookie(new Cookie(vn.edu.ute.milktea.security.JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, kitchenToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pha Chế")));

        // Admin vào /admin/catalog
        mockMvc.perform(get("/admin/catalog")
                        .cookie(new Cookie(vn.edu.ute.milktea.security.JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, adminToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Quản Trị Quán")));

        // Admin vào /admin/tables
        mockMvc.perform(get("/admin/tables")
                        .cookie(new Cookie(vn.edu.ute.milktea.security.JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, adminToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sơ Đồ Bàn")));

        // Admin vào /admin/staff
        mockMvc.perform(get("/admin/staff")
                        .cookie(new Cookie(vn.edu.ute.milktea.security.JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, adminToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tài Khoản Nhân Viên")));

        // Admin vào /admin/inventory
        mockMvc.perform(get("/admin/inventory")
                        .cookie(new Cookie(vn.edu.ute.milktea.security.JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, adminToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Quản Lý Kho")));

        // Admin vào /admin/reports
        mockMvc.perform(get("/admin/reports")
                        .cookie(new Cookie(vn.edu.ute.milktea.security.JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, adminToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Báo Cáo Tài Chính")));

        // Admin vào /admin/settings
        mockMvc.perform(get("/admin/settings")
                        .cookie(new Cookie(vn.edu.ute.milktea.security.JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, adminToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cài Đặt Quán")));
    }

    // ==========================================
    // 4. CSRF VÀ CLOUDINARY UPLOAD
    // ==========================================
    @Test
    @DisplayName("CSRF: Trả về CSRF token hợp lệ cho client")
    void testCsrfTokenEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("Cloudinary: Upload ảnh sản phẩm qua multipart request")
    void testCloudinaryProductImageUpload() throws Exception {
        MockMultipartFile validImage = new MockMultipartFile(
                "file",
                "test-milktea.png",
                "image/png",
                new byte[]{1, 2, 3, 4, 5}
        );

        mockMvc.perform(multipart("/api/v1/admin/products/{id}/image", product.getId())
                        .file(validImage)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.imageUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.imagePublicId").isNotEmpty());

        // Test ảnh định dạng không hợp lệ (text/plain) -> 400 Bad Request
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "not an image".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/admin/products/{id}/image", product.getId())
                        .file(invalidFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
}
