package vn.edu.ute.milktea.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.catalog.Category;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.inventory.*;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipe;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipeItem;
import vn.edu.ute.milktea.entity.recipe.ProductRecipe;
import vn.edu.ute.milktea.entity.settings.GlobalSettings;
import vn.edu.ute.milktea.entity.table.DiningTable;
import vn.edu.ute.milktea.entity.table.TableStatus;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.catalog.CategoryRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.inventory.MaterialRepository;
import vn.edu.ute.milktea.repository.inventory.StockRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeItemRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeRepository;
import vn.edu.ute.milktea.repository.recipe.ProductRecipeRepository;
import vn.edu.ute.milktea.repository.settings.GlobalSettingsRepository;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@Profile({"test", "demo"})
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final GlobalSettingsRepository settingsRepository;
    private final DiningTableRepository tableRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final MaterialRepository materialRepository;
    private final StockRepository stockRepository;
    private final ProductRecipeRepository productRecipeRepository;
    private final PreparationRecipeRepository prepRecipeRepository;
    private final PreparationRecipeItemRepository prepRecipeItemRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${app.admin.default-password:Password@123}")
    private String defaultAdminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (accountRepository.count() > 0) {
            log.info("Dữ liệu hệ thống đã tồn tại. Bỏ qua khởi tạo tự động.");
            return;
        }

        log.info("Bắt đầu khởi tạo dữ liệu mẫu cho hệ thống MilkTea...");

        // 1. Tài khoản mẫu
        String passHash = passwordEncoder.encode(defaultAdminPassword != null && !defaultAdminPassword.isBlank() ? defaultAdminPassword : "Password@123");
        Account admin = accountRepository.save(Account.builder()
                .email("admin@milktea.vn")
                .passwordHash(passHash)
                .fullName("Quản Trị Viên")
                .phone("0901000001")
                .role(Role.ADMIN)
                .active(true)
                .tokenVersion(1L)
                .createdAt(Instant.now())
                .build());

        Account cashier = accountRepository.save(Account.builder()
                .email("cashier@milktea.vn")
                .passwordHash(passHash)
                .fullName("Thu Ngân Quỳnh")
                .phone("0901000002")
                .role(Role.CASHIER)
                .active(true)
                .tokenVersion(1L)
                .createdAt(Instant.now())
                .build());

        Account kitchen = accountRepository.save(Account.builder()
                .email("kitchen@milktea.vn")
                .passwordHash(passHash)
                .fullName("Bếp Trưởng Lân")
                .phone("0901000003")
                .role(Role.KITCHEN)
                .active(true)
                .tokenVersion(1L)
                .createdAt(Instant.now())
                .build());

        Account customer = accountRepository.save(Account.builder()
                .email("customer@milktea.vn")
                .passwordHash(passHash)
                .fullName("Khách Hàng Mai")
                .phone("0901000004")
                .role(Role.CUSTOMER)
                .active(true)
                .tokenVersion(1L)
                .createdAt(Instant.now())
                .build());

        // 2. Cấu hình chung
        settingsRepository.save(GlobalSettings.builder()
                .id(1)
                .shopName("MilkTea Quán 01")
                .address("Số 1 Võ Văn Ngân, TP. Thủ Đức")
                .bankName("Vietcombank")
                .bankAccountNumber("999988887777")
                .bankAccountHolder("QUAN TRA SUA MILKTEA")
                .discountPercent(new BigDecimal("10.00"))
                .modifiedBy(admin)
                .modifiedAt(Instant.now())
                .build());

        // 3. Bàn mẫu & Mã QR
        for (int i = 1; i <= 5; i++) {
            String pad = String.format("%02d", i);
            tableRepository.save(DiningTable.builder()
                    .name("Bàn " + pad)
                    .qrCode("TABLE_QR_BAN" + pad + "_SECURE_TOKEN_XYZ" + i)
                    .status(TableStatus.TRONG)
                    .active(true)
                    .version(0L)
                    .build());
        }

        // 4. Danh mục
        Category catTraSua = categoryRepository.save(Category.builder().name("Trà Sữa Truyền Thống").active(true).build());
        Category catTraTraiCay = categoryRepository.save(Category.builder().name("Trà Trái Cây").active(true).build());
        Category catTopping = categoryRepository.save(Category.builder().name("Topping").active(true).build());

        // 5. Món uống
        Product tsM = productRepository.save(Product.builder()
                .category(catTraSua)
                .name("Trà Sữa Truyền Thống")
                .size("M")
                .description("Trà đen đậm vị kết hợp sữa tươi thơm béo")
                .price(new BigDecimal("25000"))
                .active(true)
                .build());

        Product tsL = productRepository.save(Product.builder()
                .category(catTraSua)
                .name("Trà Sữa Truyền Thống")
                .size("L")
                .description("Trà đen đậm vị kết hợp sữa tươi thơm béo")
                .price(new BigDecimal("30000"))
                .active(true)
                .build());

        Product daoM = productRepository.save(Product.builder()
                .category(catTraTraiCay)
                .name("Trà Đào Cam Sả")
                .size("M")
                .description("Trà hoa quả giải nhiệt sảng khoái")
                .price(new BigDecimal("28000"))
                .active(true)
                .build());

        Product toppingTC = productRepository.save(Product.builder()
                .category(catTopping)
                .name("Trân Châu Đen")
                .size("M")
                .description("Trân châu dẻo dai nấu mới mỗi ngày")
                .price(new BigDecimal("5000"))
                .active(true)
                .build());

        // 6. Nguyên liệu
        Material nlTraDen = materialRepository.save(Material.builder().name("Trà Đen").type(MaterialType.THO).unit("kg").active(true).build());
        Material nlSuaDac = materialRepository.save(Material.builder().name("Sữa Đặc").type(MaterialType.THO).unit("kg").active(true).build());
        Material nlDuongCat = materialRepository.save(Material.builder().name("Đường Cát").type(MaterialType.THO).unit("kg").active(true).build());
        Material nlLyNhua = materialRepository.save(Material.builder().name("Ly Nhựa").type(MaterialType.THO).unit("cai").active(true).build());
        Material nlTCSong = materialRepository.save(Material.builder().name("Trân Châu Thô").type(MaterialType.THO).unit("kg").active(true).build());
        Material nlTCChin = materialRepository.save(Material.builder().name("Trân Châu Nấu Chín").type(MaterialType.SOCHE).unit("suat").active(true).build());

        // 7. Tồn kho KHO & BEP
        List<Material> allMaterials = List.of(nlTraDen, nlSuaDac, nlDuongCat, nlLyNhua, nlTCSong, nlTCChin);
        for (Material m : allMaterials) {
            stockRepository.save(Stock.builder()
                    .id(new StockId(m.getId(), StockLocation.KHO))
                    .material(m)
                    .quantity(new BigDecimal("100.000"))
                    .threshold(new BigDecimal("10.000"))
                    .version(0L)
                    .build());

            stockRepository.save(Stock.builder()
                    .id(new StockId(m.getId(), StockLocation.BEP))
                    .material(m)
                    .quantity(new BigDecimal("20.000"))
                    .threshold(new BigDecimal("5.000"))
                    .version(0L)
                    .build());
        }

        // Cập nhật riêng cho Trân châu chín tại BEP là 50 suất
        Stock tcChinBep = stockRepository.findById(new StockId(nlTCChin.getId(), StockLocation.BEP)).get();
        tcChinBep.setQuantity(new BigDecimal("50.000"));
        tcChinBep.setThreshold(new BigDecimal("5.000"));
        stockRepository.save(tcChinBep);

        // 8. Công thức món
        productRecipeRepository.save(ProductRecipe.builder()
                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(tsM.getId(), nlTraDen.getId()))
                .product(tsM)
                .material(nlTraDen)
                .quantity(new BigDecimal("0.015"))
                .build());

        productRecipeRepository.save(ProductRecipe.builder()
                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(tsM.getId(), nlSuaDac.getId()))
                .product(tsM)
                .material(nlSuaDac)
                .quantity(new BigDecimal("0.030"))
                .build());

        productRecipeRepository.save(ProductRecipe.builder()
                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(tsM.getId(), nlDuongCat.getId()))
                .product(tsM)
                .material(nlDuongCat)
                .quantity(new BigDecimal("0.020"))
                .build());

        productRecipeRepository.save(ProductRecipe.builder()
                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(tsM.getId(), nlLyNhua.getId()))
                .product(tsM)
                .material(nlLyNhua)
                .quantity(new BigDecimal("1.000"))
                .build());

        productRecipeRepository.save(ProductRecipe.builder()
                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(toppingTC.getId(), nlTCChin.getId()))
                .product(toppingTC)
                .material(nlTCChin)
                .quantity(new BigDecimal("1.000"))
                .build());

        // 9. Công thức sơ chế
        PreparationRecipe prepRecipe = prepRecipeRepository.save(PreparationRecipe.builder()
                .name("Nấu Trân Châu Đen")
                .outputMaterial(nlTCChin)
                .standardOutputQuantity(new BigDecimal("50.000"))
                .active(true)
                .build());

        prepRecipeItemRepository.save(PreparationRecipeItem.builder()
                .id(new vn.edu.ute.milktea.entity.recipe.PreparationRecipeItemId(prepRecipe.getId(), nlTCSong.getId()))
                .recipe(prepRecipe)
                .inputMaterial(nlTCSong)
                .standardInputQuantity(new BigDecimal("5.000"))
                .build());

        log.info("Khởi tạo dữ liệu mẫu hoàn tất thành công!");
    }
}
