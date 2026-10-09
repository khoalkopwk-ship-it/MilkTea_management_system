package vn.edu.ute.milktea.service.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.AdminDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.catalog.Category;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.inventory.Material;
import vn.edu.ute.milktea.entity.recipe.*;
import vn.edu.ute.milktea.entity.table.DiningTable;
import vn.edu.ute.milktea.entity.table.TableStatus;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.catalog.CategoryRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.inventory.MaterialRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeItemRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeRepository;
import vn.edu.ute.milktea.repository.recipe.ProductRecipeRepository;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductRecipeRepository productRecipeRepository;
    private final DiningTableRepository tableRepository;
    private final MaterialRepository materialRepository;
    private final PreparationRecipeRepository preparationRecipeRepository;
    private final PreparationRecipeItemRepository preparationRecipeItemRepository;
    private final PasswordEncoder passwordEncoder;

    // ==========================================
    // 1. QUẢN LÝ NHÂN VIÊN
    // ==========================================
    @Transactional(readOnly = true)
    public List<AdminDto.StaffResponse> listStaffAccounts() {
        return accountRepository.findAll().stream()
                .filter(a -> a.getRole() != Role.CUSTOMER)
                .map(this::mapStaff)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminDto.StaffResponse getStaffAccount(Long id) {
        Account acc = accountRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy tài khoản nhân viên"));
        return mapStaff(acc);
    }

    @Transactional
    public AdminDto.StaffResponse createStaffAccount(AdminDto.CreateStaffRequest request) {
        if (accountRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED, "Email đã được đăng ký trong hệ thống");
        }

        if (request.getRole() == Role.CUSTOMER) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Không thể tạo tài khoản CUSTOMER qua giao diện quản trị nhân viên");
        }

        Account account = Account.builder()
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(request.getRole())
                .active(true)
                .tokenVersion(1L)
                .createdAt(Instant.now())
                .build();

        return mapStaff(accountRepository.save(account));
    }

    @Transactional
    public AdminDto.StaffResponse updateStaffAccount(Long id, AdminDto.UpdateStaffRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy tài khoản"));

        if (request.getFullName() != null) account.setFullName(request.getFullName());
        if (request.getPhone() != null) account.setPhone(request.getPhone());
        if (request.getRole() != null && request.getRole() != Role.CUSTOMER) {
            account.setRole(request.getRole());
            account.setTokenVersion(account.getTokenVersion() + 1); // Thu hồi token cũ khi đổi vai trò
        }
        if (request.getActive() != null) {
            account.setActive(request.getActive());
            if (!request.getActive()) {
                account.setTokenVersion(account.getTokenVersion() + 1);
            }
        }

        return mapStaff(accountRepository.save(account));
    }

    @Transactional
    public void deleteStaffAccount(Long id, Long currentAdminId) {
        if (id.equals(currentAdminId)) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Không thể tự xóa hoặc vô hiệu hóa tài khoản của chính mình");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy tài khoản"));

        account.setActive(false);
        account.setTokenVersion(account.getTokenVersion() + 1);
        accountRepository.save(account);
    }

    private AdminDto.StaffResponse mapStaff(Account a) {
        return AdminDto.StaffResponse.builder()
                .id(a.getId())
                .email(a.getEmail())
                .fullName(a.getFullName())
                .phone(a.getPhone())
                .role(a.getRole())
                .active(a.getActive())
                .createdAt(a.getCreatedAt())
                .build();
    }

    // ==========================================
    // 2. QUẢN LÝ DANH MỤC
    // ==========================================
    @Transactional(readOnly = true)
    public List<AdminDto.CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream().map(c -> AdminDto.CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .active(c.getActive())
                .build()
        ).toList();
    }

    @Transactional(readOnly = true)
    public AdminDto.CategoryResponse getCategory(Long id) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy danh mục"));
        return AdminDto.CategoryResponse.builder().id(c.getId()).name(c.getName()).active(c.getActive()).build();
    }

    @Transactional
    public AdminDto.CategoryResponse createCategory(AdminDto.CategoryRequest request) {
        Category c = Category.builder()
                .name(request.getName().trim())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        c = categoryRepository.save(c);
        return AdminDto.CategoryResponse.builder().id(c.getId()).name(c.getName()).active(c.getActive()).build();
    }

    @Transactional
    public AdminDto.CategoryResponse updateCategory(Long id, AdminDto.CategoryRequest request) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy danh mục"));
        if (request.getName() != null) c.setName(request.getName().trim());
        if (request.getActive() != null) c.setActive(request.getActive());
        c = categoryRepository.save(c);
        return AdminDto.CategoryResponse.builder().id(c.getId()).name(c.getName()).active(c.getActive()).build();
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy danh mục"));
        c.setActive(false);
        categoryRepository.save(c);
    }

    // ==========================================
    // 3. QUẢN LÝ SẢN PHẨM & CÔNG THỨC SẢN PHẨM
    // ==========================================
    @Transactional(readOnly = true)
    public List<AdminDto.ProductResponse> listProducts() {
        return productRepository.findAll().stream().map(this::mapProduct).toList();
    }

    @Transactional(readOnly = true)
    public AdminDto.ProductResponse getProduct(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.PRODUCT_UNAVAILABLE, "Không tìm thấy món uống"));
        return mapProduct(p);
    }

    @Transactional
    public AdminDto.ProductResponse createProduct(AdminDto.ProductRequest request) {
        Category cat = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy danh mục"));

        Product p = Product.builder()
                .category(cat)
                .name(request.getName().trim())
                .size(request.getSize().trim().toUpperCase())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        return mapProduct(productRepository.save(p));
    }

    @Transactional
    public AdminDto.ProductResponse updateProduct(Long id, AdminDto.ProductRequest request) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.PRODUCT_UNAVAILABLE, "Không tìm thấy món uống"));

        if (request.getCategoryId() != null) {
            Category cat = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy danh mục"));
            p.setCategory(cat);
        }
        if (request.getName() != null) p.setName(request.getName().trim());
        if (request.getSize() != null) p.setSize(request.getSize().trim().toUpperCase());
        if (request.getDescription() != null) p.setDescription(request.getDescription());
        if (request.getPrice() != null) p.setPrice(request.getPrice());
        if (request.getImageUrl() != null) p.setImageUrl(request.getImageUrl());
        if (request.getActive() != null) p.setActive(request.getActive());

        return mapProduct(productRepository.save(p));
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.PRODUCT_UNAVAILABLE, "Không tìm thấy món uống"));
        p.setActive(false);
        productRepository.save(p);
    }

    @Transactional(readOnly = true)
    public AdminDto.ProductRecipeResponse getProductRecipe(Long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.PRODUCT_UNAVAILABLE, "Không tìm thấy món uống"));

        List<ProductRecipe> recipes = productRecipeRepository.findByIdProductId(productId);
        List<AdminDto.RecipeItemDto> items = recipes.stream().map(r -> AdminDto.RecipeItemDto.builder()
                .materialId(r.getMaterial().getId())
                .materialName(r.getMaterial().getName())
                .unit(r.getMaterial().getUnit())
                .quantity(r.getQuantity())
                .build()
        ).toList();

        return AdminDto.ProductRecipeResponse.builder()
                .productId(p.getId())
                .productName(p.getName())
                .ingredients(items)
                .build();
    }

    @Transactional
    public AdminDto.ProductRecipeResponse updateProductRecipe(Long productId, AdminDto.UpdateProductRecipeRequest request) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.PRODUCT_UNAVAILABLE, "Không tìm thấy món uống"));

        // Xóa công thức cũ
        productRecipeRepository.deleteByIdProductId(productId);

        List<ProductRecipe> newRecipes = new ArrayList<>();
        for (var item : request.getItems()) {
            Material mat = materialRepository.findById(item.getMaterialId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Nguyên liệu không tồn tại: ID " + item.getMaterialId()));

            newRecipes.add(ProductRecipe.builder()
                    .id(new ProductRecipeId(productId, mat.getId()))
                    .product(p)
                    .material(mat)
                    .quantity(item.getQuantity())
                    .build());
        }
        productRecipeRepository.saveAll(newRecipes);

        return getProductRecipe(productId);
    }

    private AdminDto.ProductResponse mapProduct(Product p) {
        return AdminDto.ProductResponse.builder()
                .id(p.getId())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .name(p.getName())
                .size(p.getSize())
                .description(p.getDescription())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .active(p.getActive())
                .build();
    }

    // ==========================================
    // 4. QUẢN LÝ BÀN
    // ==========================================
    @Transactional(readOnly = true)
    public List<AdminDto.TableResponse> listTables() {
        return tableRepository.findAll().stream().map(this::mapTable).toList();
    }

    @Transactional(readOnly = true)
    public AdminDto.TableResponse getTable(Long id) {
        DiningTable t = tableRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_NOT_FOUND, "Không tìm thấy bàn"));
        return mapTable(t);
    }

    @Transactional
    public AdminDto.TableResponse createTable(AdminDto.TableRequest request) {
        String qrCode = "TABLE_QR_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        DiningTable table = DiningTable.builder()
                .name(request.getName().trim())
                .qrCode(qrCode)
                .status(TableStatus.TRONG)
                .active(request.getActive() != null ? request.getActive() : true)
                .version(0L)
                .build();
        return mapTable(tableRepository.save(table));
    }

    @Transactional
    public AdminDto.TableResponse updateTable(Long id, AdminDto.TableRequest request) {
        DiningTable t = tableRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_NOT_FOUND, "Không tìm thấy bàn"));
        if (request.getName() != null) t.setName(request.getName().trim());
        if (request.getActive() != null) t.setActive(request.getActive());
        return mapTable(tableRepository.save(t));
    }

    @Transactional
    public void deleteTable(Long id) {
        DiningTable t = tableRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_NOT_FOUND, "Không tìm thấy bàn"));
        if (t.getStatus() == TableStatus.CO_KHACH) {
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Không thể xóa bàn đang có khách");
        }
        t.setActive(false);
        tableRepository.save(t);
    }

    @Transactional(readOnly = true)
    public String getTableQr(Long id) {
        DiningTable t = tableRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_NOT_FOUND, "Không tìm thấy bàn"));
        return t.getQrCode();
    }

    private AdminDto.TableResponse mapTable(DiningTable t) {
        return AdminDto.TableResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .qrCode(t.getQrCode())
                .status(t.getStatus())
                .activeSessionId(t.getActiveSessionId())
                .active(t.getActive())
                .build();
    }

    // ==========================================
    // 5. QUẢN LÝ NGUYÊN LIỆU
    // ==========================================
    @Transactional(readOnly = true)
    public List<AdminDto.MaterialResponse> listMaterials() {
        return materialRepository.findAll().stream().map(m -> AdminDto.MaterialResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .type(m.getType())
                .unit(m.getUnit())
                .active(m.getActive())
                .build()
        ).toList();
    }

    @Transactional(readOnly = true)
    public AdminDto.MaterialResponse getMaterial(Long id) {
        Material m = materialRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy nguyên liệu"));
        return AdminDto.MaterialResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .type(m.getType())
                .unit(m.getUnit())
                .active(m.getActive())
                .build();
    }

    @Transactional
    public AdminDto.MaterialResponse createMaterial(AdminDto.MaterialRequest request) {
        Material m = Material.builder()
                .name(request.getName().trim())
                .type(request.getType())
                .unit(request.getUnit().trim())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        m = materialRepository.save(m);
        return AdminDto.MaterialResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .type(m.getType())
                .unit(m.getUnit())
                .active(m.getActive())
                .build();
    }

    @Transactional
    public AdminDto.MaterialResponse updateMaterial(Long id, AdminDto.MaterialRequest request) {
        Material m = materialRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy nguyên liệu"));

        if (request.getName() != null) m.setName(request.getName().trim());
        if (request.getType() != null) m.setType(request.getType());
        if (request.getUnit() != null) m.setUnit(request.getUnit().trim());
        if (request.getActive() != null) m.setActive(request.getActive());

        m = materialRepository.save(m);
        return AdminDto.MaterialResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .type(m.getType())
                .unit(m.getUnit())
                .active(m.getActive())
                .build();
    }

    @Transactional
    public void deleteMaterial(Long id) {
        Material m = materialRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy nguyên liệu"));
        m.setActive(false);
        materialRepository.save(m);
    }

    // ==========================================
    // 6. QUẢN LÝ CÔNG THỨC SƠ CHẾ
    // ==========================================
    @Transactional(readOnly = true)
    public List<AdminDto.PreparationRecipeResponse> listPreparationRecipes() {
        return preparationRecipeRepository.findAll().stream().map(this::mapPrepRecipe).toList();
    }

    @Transactional(readOnly = true)
    public AdminDto.PreparationRecipeResponse getPreparationRecipe(Long id) {
        PreparationRecipe r = preparationRecipeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy công thức sơ chế"));
        return mapPrepRecipe(r);
    }

    @Transactional
    public AdminDto.PreparationRecipeResponse createPreparationRecipe(AdminDto.CreatePreparationRecipeRequest request) {
        Material outMat = materialRepository.findById(request.getOutputMaterialId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy bán thành phẩm đầu ra"));

        PreparationRecipe recipe = PreparationRecipe.builder()
                .name(request.getName().trim())
                .outputMaterial(outMat)
                .standardOutputQuantity(request.getStandardOutputQuantity())
                .active(true)
                .build();
        recipe = preparationRecipeRepository.save(recipe);

        if (request.getInputItems() != null) {
            List<PreparationRecipeItem> items = new ArrayList<>();
            for (var in : request.getInputItems()) {
                Material inMat = materialRepository.findById(in.getInputMaterialId())
                        .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy nguyên liệu đầu vào: ID " + in.getInputMaterialId()));

                items.add(PreparationRecipeItem.builder()
                        .id(new PreparationRecipeItemId(recipe.getId(), inMat.getId()))
                        .recipe(recipe)
                        .inputMaterial(inMat)
                        .standardInputQuantity(in.getStandardQuantity())
                        .build());
            }
            preparationRecipeItemRepository.saveAll(items);
        }

        return getPreparationRecipe(recipe.getId());
    }

    @Transactional
    public AdminDto.PreparationRecipeResponse updatePreparationRecipe(Long id, AdminDto.CreatePreparationRecipeRequest request) {
        PreparationRecipe recipe = preparationRecipeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy công thức sơ chế"));

        if (request.getName() != null) recipe.setName(request.getName().trim());
        if (request.getOutputMaterialId() != null) {
            Material outMat = materialRepository.findById(request.getOutputMaterialId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy bán thành phẩm đầu ra"));
            recipe.setOutputMaterial(outMat);
        }
        if (request.getStandardOutputQuantity() != null) {
            recipe.setStandardOutputQuantity(request.getStandardOutputQuantity());
        }
        preparationRecipeRepository.save(recipe);

        if (request.getInputItems() != null) {
            preparationRecipeItemRepository.deleteByIdRecipeId(id);
            List<PreparationRecipeItem> items = new ArrayList<>();
            for (var in : request.getInputItems()) {
                Material inMat = materialRepository.findById(in.getInputMaterialId())
                        .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy nguyên liệu đầu vào: ID " + in.getInputMaterialId()));

                items.add(PreparationRecipeItem.builder()
                        .id(new PreparationRecipeItemId(recipe.getId(), inMat.getId()))
                        .recipe(recipe)
                        .inputMaterial(inMat)
                        .standardInputQuantity(in.getStandardQuantity())
                        .build());
            }
            preparationRecipeItemRepository.saveAll(items);
        }

        return getPreparationRecipe(id);
    }

    @Transactional
    public void deletePreparationRecipe(Long id) {
        PreparationRecipe r = preparationRecipeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy công thức sơ chế"));
        r.setActive(false);
        preparationRecipeRepository.save(r);
    }

    private AdminDto.PreparationRecipeResponse mapPrepRecipe(PreparationRecipe r) {
        List<PreparationRecipeItem> items = preparationRecipeItemRepository.findByIdRecipeId(r.getId());
        List<AdminDto.PrepRecipeItemDto> itemDtos = items.stream().map(it -> AdminDto.PrepRecipeItemDto.builder()
                .inputMaterialId(it.getInputMaterial().getId())
                .inputMaterialName(it.getInputMaterial().getName())
                .unit(it.getInputMaterial().getUnit())
                .standardQuantity(it.getStandardInputQuantity())
                .build()
        ).toList();

        return AdminDto.PreparationRecipeResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .outputMaterialId(r.getOutputMaterial().getId())
                .outputMaterialName(r.getOutputMaterial().getName())
                .outputUnit(r.getOutputMaterial().getUnit())
                .standardOutputQuantity(r.getStandardOutputQuantity())
                .inputItems(itemDtos)
                .build();
    }
}
