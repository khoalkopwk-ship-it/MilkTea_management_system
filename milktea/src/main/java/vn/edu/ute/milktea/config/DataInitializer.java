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
import vn.edu.ute.milktea.repository.inventory.ImportReceiptItemRepository;
import vn.edu.ute.milktea.repository.inventory.ImportReceiptRepository;
import vn.edu.ute.milktea.repository.inventory.MaterialRepository;
import vn.edu.ute.milktea.repository.inventory.PreparationBatchItemRepository;
import vn.edu.ute.milktea.repository.inventory.PreparationBatchRepository;
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
@Profile({ "test", "demo" })
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
        private final PreparationBatchRepository prepBatchRepository;
        private final PreparationBatchItemRepository prepBatchItemRepository;
        private final ImportReceiptRepository importReceiptRepository;
        private final ImportReceiptItemRepository importReceiptItemRepository;
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
                String passHash = passwordEncoder.encode(
                                defaultAdminPassword != null && !defaultAdminPassword.isBlank() ? defaultAdminPassword
                                                : "Password@123");
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
                                .bankAccountNumber("08881706888")
                                .bankAccountHolder("DANG NGUYEN MINH KHOA")
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
                Category catTraSua = categoryRepository
                                .save(Category.builder().name("Trà Sữa Truyền Thống").active(true).build());
                Category catTraTraiCay = categoryRepository
                                .save(Category.builder().name("Trà Trái Cây").active(true).build());
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

                Product daoL = productRepository.save(Product.builder()
                                .category(catTraTraiCay)
                                .name("Trà Đào Cam Sả")
                                .size("L")
                                .description("Trà hoa quả giải nhiệt sảng khoái")
                                .price(new BigDecimal("33000"))
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

                Product toppingTD = productRepository.save(Product.builder()
                                .category(catTopping)
                                .name("Thạch Đào")
                                .size("M")
                                .description("Thạch đào giòn ngọt thơm mát")
                                .price(new BigDecimal("8000"))
                                .active(true)
                                .build());

                Product toppingTCT = productRepository.save(Product.builder()
                                .category(catTopping)
                                .name("Trân Châu Trắng")
                                .size("M")
                                .description("Trân châu trắng giòn ngọt sần sật")
                                .price(new BigDecimal("7000"))
                                .active(true)
                                .build());

                Product toppingKC = productRepository.save(Product.builder()
                                .category(catTopping)
                                .name("Kem Cheese")
                                .size("M")
                                .description("Lớp váng sữa phô mai mặn mà béo ngậy")
                                .price(new BigDecimal("10000"))
                                .active(true)
                                .build());

                // 6. Nguyên liệu
                Material nlTraDen = materialRepository.save(Material.builder().name("Trà Đen").type(MaterialType.THO)
                                .unit("kg").active(true).build());
                Material nlSuaDac = materialRepository.save(Material.builder().name("Sữa Đặc").type(MaterialType.THO)
                                .unit("kg").active(true).build());
                Material nlDuongCat = materialRepository.save(Material.builder().name("Đường Cát")
                                .type(MaterialType.THO).unit("kg").active(true).build());
                Material nlLyNhua = materialRepository.save(Material.builder().name("Ly Nhựa").type(MaterialType.THO)
                                .unit("cai").active(true).build());
                Material nlTCSong = materialRepository.save(Material.builder().name("Trân Châu Thô")
                                .type(MaterialType.THO).unit("kg").active(true).build());
                Material nlDaoNgam = materialRepository.save(Material.builder().name("Đào Ngâm").type(MaterialType.THO)
                                .unit("kg").active(true).build());
                Material nlCamTuoi = materialRepository.save(Material.builder().name("Cam Tươi").type(MaterialType.THO)
                                .unit("kg").active(true).build());
                Material nlSaCay = materialRepository.save(Material.builder().name("Sả Cây").type(MaterialType.THO)
                                .unit("kg").active(true).build());
                Material nlTCTrang = materialRepository.save(Material.builder().name("Trân Châu Trắng")
                                .type(MaterialType.THO).unit("kg").active(true).build());
                Material nlKemCheese = materialRepository.save(Material.builder().name("Bột Kem Cheese")
                                .type(MaterialType.THO).unit("kg").active(true).build());
                Material nlThachDao = materialRepository.save(Material.builder().name("Thạch Đào")
                                .type(MaterialType.THO).unit("kg").active(true).build());
                Material nlTCChin = materialRepository.save(Material.builder().name("Trân Châu Nấu Chín")
                                .type(MaterialType.SOCHE).unit("g").active(true).build());
                Material nlCotTra = materialRepository.save(Material.builder().name("Nước Cốt Trà Đen")
                                .type(MaterialType.SOCHE).unit("ml").active(true).build());

                // 7. Tồn kho KHO & BEP
                List<Material> allMaterials = List.of(
                                nlTraDen, nlSuaDac, nlDuongCat, nlLyNhua, nlTCSong,
                                nlDaoNgam, nlCamTuoi, nlSaCay, nlTCTrang, nlKemCheese, nlThachDao,
                                nlTCChin, nlCotTra);
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

                // Cập nhật tồn kho thực tế cho nguyên liệu sơ chế tại BEP
                Stock tcChinBep = stockRepository.findById(new StockId(nlTCChin.getId(), StockLocation.BEP)).get();
                tcChinBep.setQuantity(new BigDecimal("2500.000"));
                tcChinBep.setThreshold(new BigDecimal("250.000"));
                stockRepository.save(tcChinBep);

                Stock cotTraBep = stockRepository.findById(new StockId(nlCotTra.getId(), StockLocation.BEP)).get();
                cotTraBep.setQuantity(new BigDecimal("6000.000"));
                cotTraBep.setThreshold(new BigDecimal("1000.000"));
                stockRepository.save(cotTraBep);

                // 7b. Khởi tạo phiếu nhập kho mẫu để có giá vốn nguyên liệu thô
                ImportReceipt sampleImport = importReceiptRepository.save(ImportReceipt.builder()
                                .status(DocumentStatus.DA_GHI_SO)
                                .supplier("Công ty Trà Việt")
                                .reason("Nhập kho nguyên liệu đầu kỳ & thiết lập giá vốn")
                                .createdBy(admin)
                                .createdAt(Instant.now())
                                .idempotencyKey(java.util.UUID.randomUUID().toString())
                                .build());

                importReceiptItemRepository.save(ImportReceiptItem.builder()
                                .id(new ImportReceiptItemId(sampleImport.getId(), nlTraDen.getId()))
                                .receipt(sampleImport)
                                .material(nlTraDen)
                                .quantity(new BigDecimal("100.000"))
                                .unitPrice(new BigDecimal("50000.00"))
                                .build());

                importReceiptItemRepository.save(ImportReceiptItem.builder()
                                .id(new ImportReceiptItemId(sampleImport.getId(), nlSuaDac.getId()))
                                .receipt(sampleImport)
                                .material(nlSuaDac)
                                .quantity(new BigDecimal("100.000"))
                                .unitPrice(new BigDecimal("40000.00"))
                                .build());

                importReceiptItemRepository.save(ImportReceiptItem.builder()
                                .id(new ImportReceiptItemId(sampleImport.getId(), nlDuongCat.getId()))
                                .receipt(sampleImport)
                                .material(nlDuongCat)
                                .quantity(new BigDecimal("100.000"))
                                .unitPrice(new BigDecimal("20000.00"))
                                .build());

                importReceiptItemRepository.save(ImportReceiptItem.builder()
                                .id(new ImportReceiptItemId(sampleImport.getId(), nlTCSong.getId()))
                                .receipt(sampleImport)
                                .material(nlTCSong)
                                .quantity(new BigDecimal("100.000"))
                                .unitPrice(new BigDecimal("30000.00"))
                                .build());

                importReceiptItemRepository.save(ImportReceiptItem.builder()
                                .id(new ImportReceiptItemId(sampleImport.getId(), nlLyNhua.getId()))
                                .receipt(sampleImport)
                                .material(nlLyNhua)
                                .quantity(new BigDecimal("1000.000"))
                                .unitPrice(new BigDecimal("500.00"))
                                .build());

                importReceiptItemRepository.save(ImportReceiptItem.builder()
                                .id(new ImportReceiptItemId(sampleImport.getId(), nlDaoNgam.getId()))
                                .receipt(sampleImport)
                                .material(nlDaoNgam)
                                .quantity(new BigDecimal("50.000"))
                                .unitPrice(new BigDecimal("45000.00"))
                                .build());

                importReceiptItemRepository.save(ImportReceiptItem.builder()
                                .id(new ImportReceiptItemId(sampleImport.getId(), nlCamTuoi.getId()))
                                .receipt(sampleImport)
                                .material(nlCamTuoi)
                                .quantity(new BigDecimal("50.000"))
                                .unitPrice(new BigDecimal("25000.00"))
                                .build());

                importReceiptItemRepository.save(ImportReceiptItem.builder()
                                .id(new ImportReceiptItemId(sampleImport.getId(), nlSaCay.getId()))
                                .receipt(sampleImport)
                                .material(nlSaCay)
                                .quantity(new BigDecimal("30.000"))
                                .unitPrice(new BigDecimal("15000.00"))
                                .build());

                importReceiptItemRepository.save(ImportReceiptItem.builder()
                                .id(new ImportReceiptItemId(sampleImport.getId(), nlTCTrang.getId()))
                                .receipt(sampleImport)
                                .material(nlTCTrang)
                                .quantity(new BigDecimal("40.000"))
                                .unitPrice(new BigDecimal("35000.00"))
                                .build());

                importReceiptItemRepository.save(ImportReceiptItem.builder()
                                .id(new ImportReceiptItemId(sampleImport.getId(), nlKemCheese.getId()))
                                .receipt(sampleImport)
                                .material(nlKemCheese)
                                .quantity(new BigDecimal("30.000"))
                                .unitPrice(new BigDecimal("90000.00"))
                                .build());

                importReceiptItemRepository.save(ImportReceiptItem.builder()
                                .id(new ImportReceiptItemId(sampleImport.getId(), nlThachDao.getId()))
                                .receipt(sampleImport)
                                .material(nlThachDao)
                                .quantity(new BigDecimal("40.000"))
                                .unitPrice(new BigDecimal("40000.00"))
                                .build());

                // 8. Công thức món (dùng đơn vị thực tế: Nước cốt trà đen theo ml, topping theo
                // g)
                // 8.1 Trà Sữa Truyền Thống M (150 ml nước cốt trà)
                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(tsM.getId(), nlCotTra.getId()))
                                .product(tsM)
                                .material(nlCotTra)
                                .quantity(new BigDecimal("150.000"))
                                .build());

                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(tsM.getId(), nlSuaDac.getId()))
                                .product(tsM)
                                .material(nlSuaDac)
                                .quantity(new BigDecimal("0.030"))
                                .build());

                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(tsM.getId(),
                                                nlDuongCat.getId()))
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

                // 8.2 Trà Sữa Truyền Thống L (200 ml nước cốt trà)
                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(tsL.getId(), nlCotTra.getId()))
                                .product(tsL)
                                .material(nlCotTra)
                                .quantity(new BigDecimal("200.000"))
                                .build());

                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(tsL.getId(), nlSuaDac.getId()))
                                .product(tsL)
                                .material(nlSuaDac)
                                .quantity(new BigDecimal("0.040"))
                                .build());

                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(tsL.getId(),
                                                nlDuongCat.getId()))
                                .product(tsL)
                                .material(nlDuongCat)
                                .quantity(new BigDecimal("0.025"))
                                .build());

                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(tsL.getId(), nlLyNhua.getId()))
                                .product(tsL)
                                .material(nlLyNhua)
                                .quantity(new BigDecimal("1.000"))
                                .build());

                // 8.3 Trà Đào Cam Sả M
                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(daoM.getId(),
                                                nlCotTra.getId()))
                                .product(daoM)
                                .material(nlCotTra)
                                .quantity(new BigDecimal("100.000"))
                                .build());

                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(daoM.getId(),
                                                nlDaoNgam.getId()))
                                .product(daoM)
                                .material(nlDaoNgam)
                                .quantity(new BigDecimal("0.040"))
                                .build());

                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(daoM.getId(),
                                                nlCamTuoi.getId()))
                                .product(daoM)
                                .material(nlCamTuoi)
                                .quantity(new BigDecimal("0.030"))
                                .build());

                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(daoM.getId(), nlSaCay.getId()))
                                .product(daoM)
                                .material(nlSaCay)
                                .quantity(new BigDecimal("0.010"))
                                .build());

                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(daoM.getId(),
                                                nlDuongCat.getId()))
                                .product(daoM)
                                .material(nlDuongCat)
                                .quantity(new BigDecimal("0.020"))
                                .build());

                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(daoM.getId(),
                                                nlLyNhua.getId()))
                                .product(daoM)
                                .material(nlLyNhua)
                                .quantity(new BigDecimal("1.000"))
                                .build());

                // 8.4 Trà Đào Cam Sả L
                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(daoL.getId(),
                                                nlCotTra.getId()))
                                .product(daoL)
                                .material(nlCotTra)
                                .quantity(new BigDecimal("150.000"))
                                .build());

                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(daoL.getId(),
                                                nlDaoNgam.getId()))
                                .product(daoL)
                                .material(nlDaoNgam)
                                .quantity(new BigDecimal("0.050"))
                                .build());

                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(daoL.getId(),
                                                nlCamTuoi.getId()))
                                .product(daoL)
                                .material(nlCamTuoi)
                                .quantity(new BigDecimal("0.040"))
                                .build());

                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(daoL.getId(), nlSaCay.getId()))
                                .product(daoL)
                                .material(nlSaCay)
                                .quantity(new BigDecimal("0.015"))
                                .build());

                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(daoL.getId(),
                                                nlDuongCat.getId()))
                                .product(daoL)
                                .material(nlDuongCat)
                                .quantity(new BigDecimal("0.025"))
                                .build());

                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(daoL.getId(),
                                                nlLyNhua.getId()))
                                .product(daoL)
                                .material(nlLyNhua)
                                .quantity(new BigDecimal("1.000"))
                                .build());

                // 8.5 Topping Trân Châu Đen (50 g Trân Châu Nấu Chín)
                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(toppingTC.getId(),
                                                nlTCChin.getId()))
                                .product(toppingTC)
                                .material(nlTCChin)
                                .quantity(new BigDecimal("50.000"))
                                .build());

                // 8.6 Topping Thạch Đào (50 g Thạch Đào)
                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(toppingTD.getId(),
                                                nlThachDao.getId()))
                                .product(toppingTD)
                                .material(nlThachDao)
                                .quantity(new BigDecimal("0.050"))
                                .build());

                // 8.7 Topping Trân Châu Trắng (50 g Trân Châu Trắng)
                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(toppingTCT.getId(),
                                                nlTCTrang.getId()))
                                .product(toppingTCT)
                                .material(nlTCTrang)
                                .quantity(new BigDecimal("0.050"))
                                .build());

                // 8.8 Topping Kem Cheese (40 g Bột Kem Cheese)
                productRecipeRepository.save(ProductRecipe.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(toppingKC.getId(),
                                                nlKemCheese.getId()))
                                .product(toppingKC)
                                .material(nlKemCheese)
                                .quantity(new BigDecimal("0.040"))
                                .build());

                // 9. Công thức sơ chế
                // 9.1 Ủ Nước Cốt Trà Đen: 1 kg Trà Đen -> 3000 ml Nước Cốt
                PreparationRecipe prepTra = prepRecipeRepository.save(PreparationRecipe.builder()
                                .name("Ủ Nước Cốt Trà Đen")
                                .outputMaterial(nlCotTra)
                                .standardOutputQuantity(new BigDecimal("3000.000"))
                                .tolerancePercent(new BigDecimal("15.00"))
                                .active(true)
                                .build());

                prepRecipeItemRepository.save(PreparationRecipeItem.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.PreparationRecipeItemId(prepTra.getId(),
                                                nlTraDen.getId()))
                                .recipe(prepTra)
                                .inputMaterial(nlTraDen)
                                .standardInputQuantity(new BigDecimal("1.000"))
                                .build());

                // 9.2 Nấu Trân Châu Đen: 1 kg Trân Châu Thô -> 2500 g Trân Châu Chín
                PreparationRecipe prepTC = prepRecipeRepository.save(PreparationRecipe.builder()
                                .name("Nấu Trân Châu Đen")
                                .outputMaterial(nlTCChin)
                                .standardOutputQuantity(new BigDecimal("2500.000"))
                                .tolerancePercent(new BigDecimal("15.00"))
                                .active(true)
                                .build());

                prepRecipeItemRepository.save(PreparationRecipeItem.builder()
                                .id(new vn.edu.ute.milktea.entity.recipe.PreparationRecipeItemId(prepTC.getId(),
                                                nlTCSong.getId()))
                                .recipe(prepTC)
                                .inputMaterial(nlTCSong)
                                .standardInputQuantity(new BigDecimal("1.000"))
                                .build());

                // 10. Mẻ sơ chế mẫu hoàn thành & thiết lập giá vốn bình quân cho nguyên liệu sơ
                // chế
                PreparationBatch batchTra = prepBatchRepository.save(PreparationBatch.builder()
                                .recipe(prepTra)
                                .outputMaterial(nlCotTra)
                                .expectedQuantity(new BigDecimal("3000.000"))
                                .actualQuantity(new BigDecimal("3000.000"))
                                .status(DocumentStatus.DA_GHI_SO)
                                .discrepancyReason("Mẻ ủ cốt trà chuẩn đầu kỳ")
                                .totalCost(new BigDecimal("50000.00"))
                                .unitCost(new BigDecimal("16.666667"))
                                .ratioVariancePercent(BigDecimal.ZERO)
                                .toleranceWarning(false)
                                .createdBy(kitchen)
                                .postedAt(Instant.now())
                                .idempotencyKey(java.util.UUID.randomUUID().toString())
                                .build());

                prepBatchItemRepository.save(PreparationBatchItem.builder()
                                .id(new PreparationBatchItemId(batchTra.getId(), nlTraDen.getId()))
                                .batch(batchTra)
                                .material(nlTraDen)
                                .recipeQuantity(new BigDecimal("1.000"))
                                .actualQuantity(new BigDecimal("1.000"))
                                .unitPrice(new BigDecimal("50000.00"))
                                .totalCost(new BigDecimal("50000.00"))
                                .build());

                PreparationBatch batchTC = prepBatchRepository.save(PreparationBatch.builder()
                                .recipe(prepTC)
                                .outputMaterial(nlTCChin)
                                .expectedQuantity(new BigDecimal("2500.000"))
                                .actualQuantity(new BigDecimal("2500.000"))
                                .status(DocumentStatus.DA_GHI_SO)
                                .discrepancyReason("Mẻ nấu trân châu chuẩn đầu kỳ")
                                .totalCost(new BigDecimal("30000.00"))
                                .unitCost(new BigDecimal("12.000000"))
                                .ratioVariancePercent(BigDecimal.ZERO)
                                .toleranceWarning(false)
                                .createdBy(kitchen)
                                .postedAt(Instant.now())
                                .idempotencyKey(java.util.UUID.randomUUID().toString())
                                .build());

                prepBatchItemRepository.save(PreparationBatchItem.builder()
                                .id(new PreparationBatchItemId(batchTC.getId(), nlTCSong.getId()))
                                .batch(batchTC)
                                .material(nlTCSong)
                                .recipeQuantity(new BigDecimal("1.000"))
                                .actualQuantity(new BigDecimal("1.000"))
                                .unitPrice(new BigDecimal("30000.00"))
                                .totalCost(new BigDecimal("30000.00"))
                                .build());

                log.info("Khởi tạo dữ liệu mẫu hoàn tất thành công!");
        }
}
