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

import vn.edu.ute.milktea.entity.inventory.ImportReceiptItem;
import vn.edu.ute.milktea.repository.inventory.ImportReceiptItemRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

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
    private final ImportReceiptItemRepository importReceiptItemRepository;
    private final vn.edu.ute.milktea.repository.inventory.StockRepository stockRepository;
    private final vn.edu.ute.milktea.repository.audit.BusinessAuditRepository auditRepository;
    private final vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher realtimeEventPublisher;
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

        Account saved = accountRepository.save(account);
        realtimeEventPublisher.publishAfterCommit("/topic/admin", "STAFF_CHANGED", saved.getId().toString(), null, "1", java.util.Map.of("action", "CREATE", "email", saved.getEmail()));
        return mapStaff(saved);
    }

    @Transactional
    public AdminDto.StaffResponse updateStaffAccount(Long id, AdminDto.UpdateStaffRequest request, Long currentAdminId) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy tài khoản"));

        String beforeState = "role=" + account.getRole() + ",active=" + account.getActive() + ",email=" + account.getEmail();

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(account.getEmail())) {
                accountRepository.findByEmail(newEmail).ifPresent(other -> {
                    if (!other.getId().equals(account.getId())) {
                        throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED, "Email đã được sử dụng bởi tài khoản khác");
                    }
                });
                account.setEmail(newEmail);
            }
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            account.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            account.setPhone(request.getPhone().trim());
        }

        if (request.getRole() != null && request.getRole() != account.getRole()) {
            if (request.getRole() == Role.CUSTOMER) {
                throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Không thể chuyển vai trò nhân viên sang CUSTOMER");
            }
            if (account.getRole() == Role.ADMIN && request.getRole() != Role.ADMIN) {
                long activeAdminCount = accountRepository.findAll().stream()
                        .filter(a -> a.getRole() == Role.ADMIN && Boolean.TRUE.equals(a.getActive()))
                        .count();
                if (activeAdminCount <= 1) {
                    throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Không thể chuyển vai trò của quản trị viên hoạt động cuối cùng");
                }
            }
            account.setRole(request.getRole());
            account.setTokenVersion(account.getTokenVersion() + 1); // Thu hồi JWT phiên cũ ngay lập tức
        }

        if (request.getActive() != null && !request.getActive().equals(account.getActive())) {
            if (!request.getActive()) {
                if (id.equals(currentAdminId)) {
                    throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Không thể tự khóa tài khoản của chính mình");
                }
                if (account.getRole() == Role.ADMIN) {
                    long activeAdminCount = accountRepository.findAll().stream()
                            .filter(a -> a.getRole() == Role.ADMIN && Boolean.TRUE.equals(a.getActive()))
                            .count();
                    if (activeAdminCount <= 1) {
                        throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Không thể khóa tài khoản quản trị viên hoạt động cuối cùng");
                    }
                }
                account.setActive(false);
                account.setTokenVersion(account.getTokenVersion() + 1);
            } else {
                account.setActive(true);
            }
        }

        Account saved = accountRepository.save(account);
        String afterState = "role=" + saved.getRole() + ",active=" + saved.getActive() + ",email=" + saved.getEmail();

        auditRepository.save(vn.edu.ute.milktea.entity.audit.BusinessAudit.builder()
                .accountId(currentAdminId)
                .action("STAFF_ACCOUNT_UPDATED")
                .beforeState(beforeState)
                .afterState(afterState)
                .reason("Cập nhật thông tin nhân viên #" + id)
                .createdAt(Instant.now())
                .build());

        realtimeEventPublisher.publishAfterCommit("/topic/admin", "STAFF_CHANGED", saved.getId().toString(), null, "1", java.util.Map.of("action", "UPDATE", "email", saved.getEmail()));

        return mapStaff(saved);
    }

    @Transactional
    public AdminDto.StaffResponse toggleStaffStatus(Long id, boolean active, Long currentAdminId) {
        if (!active && id.equals(currentAdminId)) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Không thể tự khóa tài khoản của chính mình");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy tài khoản"));

        if (!active && account.getRole() == Role.ADMIN) {
            long activeAdminCount = accountRepository.findAll().stream()
                    .filter(a -> a.getRole() == Role.ADMIN && Boolean.TRUE.equals(a.getActive()))
                    .count();
            if (activeAdminCount <= 1) {
                throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Không thể khóa tài khoản quản trị viên hoạt động cuối cùng");
            }
        }

        String before = "active=" + account.getActive();
        account.setActive(active);
        if (!active) {
            account.setTokenVersion(account.getTokenVersion() + 1); // Thu hồi JWT phiên cũ lập tức
        }
        Account saved = accountRepository.save(account);
        String after = "active=" + saved.getActive();

        auditRepository.save(vn.edu.ute.milktea.entity.audit.BusinessAudit.builder()
                .accountId(currentAdminId)
                .action(active ? "STAFF_ACCOUNT_UNLOCKED" : "STAFF_ACCOUNT_LOCKED")
                .beforeState(before)
                .afterState(after)
                .reason((active ? "Mở khóa" : "Khóa") + " tài khoản nhân viên #" + id)
                .createdAt(Instant.now())
                .build());

        realtimeEventPublisher.publishAfterCommit("/topic/admin", "STAFF_CHANGED", saved.getId().toString(), null, "1", java.util.Map.of("action", active ? "UNLOCK" : "LOCK", "email", saved.getEmail()));

        return mapStaff(saved);
    }

    @Transactional
    public void deleteStaffAccount(Long id, Long currentAdminId) {
        toggleStaffStatus(id, false, currentAdminId);
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
                .allowTopping(c.getAllowTopping())
                .build()
        ).toList();
    }

    @Transactional(readOnly = true)
    public AdminDto.CategoryResponse getCategory(Long id) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy danh mục"));
        return AdminDto.CategoryResponse.builder().id(c.getId()).name(c.getName()).active(c.getActive()).allowTopping(c.getAllowTopping()).build();
    }

    @Transactional
    public AdminDto.CategoryResponse createCategory(AdminDto.CategoryRequest request) {
        Category c = Category.builder()
                .name(request.getName().trim())
                .active(request.getActive() != null ? request.getActive() : true)
                .allowTopping(request.getAllowTopping() != null ? request.getAllowTopping() : true)
                .build();
        c = categoryRepository.save(c);
        return AdminDto.CategoryResponse.builder().id(c.getId()).name(c.getName()).active(c.getActive()).allowTopping(c.getAllowTopping()).build();
    }

    @Transactional
    public AdminDto.CategoryResponse updateCategory(Long id, AdminDto.CategoryRequest request) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy danh mục"));
        if (request.getName() != null) c.setName(request.getName().trim());
        if (request.getActive() != null) c.setActive(request.getActive());
        if (request.getAllowTopping() != null) c.setAllowTopping(request.getAllowTopping());
        c = categoryRepository.save(c);
        return AdminDto.CategoryResponse.builder().id(c.getId()).name(c.getName()).active(c.getActive()).allowTopping(c.getAllowTopping()).build();
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
    public AdminDto.ProductPageResponse searchProducts(Long categoryId, Boolean active, String query, int page, int limit) {
        List<Product> all = productRepository.findAll();
        var stream = all.stream();
        if (categoryId != null) {
            stream = stream.filter(p -> p.getCategory() != null && categoryId.equals(p.getCategory().getId()));
        }
        if (active != null) {
            stream = stream.filter(p -> active.equals(p.getActive()));
        }
        if (query != null && !query.isBlank()) {
            String q = query.trim().toLowerCase();
            stream = stream.filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(q))
                    || (p.getDescription() != null && p.getDescription().toLowerCase().contains(q))
                    || (p.getSize() != null && p.getSize().toLowerCase().contains(q)));
        }
        List<AdminDto.ProductResponse> filtered = stream.map(this::mapProduct).toList();
        int totalItems = filtered.size();
        int safeLimit = limit <= 0 ? 10 : limit;
        int totalPages = (int) Math.ceil((double) totalItems / safeLimit);
        if (totalPages == 0) totalPages = 1;
        int safePage = Math.max(1, Math.min(page, totalPages));
        int fromIdx = (safePage - 1) * safeLimit;
        int toIdx = Math.min(fromIdx + safeLimit, totalItems);
        List<AdminDto.ProductResponse> paged = fromIdx < totalItems ? filtered.subList(fromIdx, toIdx) : List.of();
        return AdminDto.ProductPageResponse.builder()
                .items(paged)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .page(safePage)
                .limit(safeLimit)
                .build();
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

        String productName = request.getName().trim();
        String productSize = request.getSize().trim().toUpperCase();
        if (productRepository.existsByNameIgnoreCaseAndSizeIgnoreCase(productName, productSize)) {
            throw BusinessException.conflict(
                    ErrorCode.VALIDATION_FAILED,
                    "Món uống này đã tồn tại với cùng size"
            );
        }

        Product p = Product.builder()
                .category(cat)
                .name(productName)
                .size(productSize)
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

        Set<Long> materialIds = new HashSet<>();
        for (var item : request.getItems()) {
            if (item.getQuantity() == null || item.getQuantity().signum() <= 0 || !materialIds.add(item.getMaterialId())) {
                throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Định lượng phải lớn hơn 0 và mỗi nguyên liệu chỉ xuất hiện một lần");
            }
        }
        // Xóa công thức cũ
        productRecipeRepository.deleteByIdProductId(productId);
        productRecipeRepository.flush();

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
                .averageImportPrice(getMaterialAverageImportPrice(m.getId()))
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
                .averageImportPrice(getMaterialAverageImportPrice(m.getId()))
                .build();
    }

    private BigDecimal getMaterialAverageImportPrice(Long materialId) {
        List<ImportReceiptItem> items = importReceiptItemRepository.findByIdMaterialId(materialId);
        if (items == null || items.isEmpty()) return null;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;
        for (ImportReceiptItem it : items) {
            if (it.getQuantity() != null && it.getQuantity().signum() > 0 && it.getUnitPrice() != null) {
                totalCost = totalCost.add(it.getQuantity().multiply(it.getUnitPrice()));
                totalQty = totalQty.add(it.getQuantity());
            }
        }
        if (totalQty.signum() <= 0) return null;
        return totalCost.divide(totalQty, 2, RoundingMode.HALF_UP);
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
        realtimeEventPublisher.publishAfterCommit("/topic/stock", "MATERIAL_CHANGED", m.getId().toString(), null, "1", java.util.Map.of("action", "CREATE", "name", m.getName()));
        realtimeEventPublisher.publishAfterCommit("/topic/admin", "MATERIAL_CHANGED", m.getId().toString(), null, "1", java.util.Map.of("action", "CREATE", "name", m.getName()));
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
        realtimeEventPublisher.publishAfterCommit("/topic/stock", "MATERIAL_CHANGED", m.getId().toString(), null, "1", java.util.Map.of("action", "UPDATE", "name", m.getName()));
        realtimeEventPublisher.publishAfterCommit("/topic/admin", "MATERIAL_CHANGED", m.getId().toString(), null, "1", java.util.Map.of("action", "UPDATE", "name", m.getName()));
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
        realtimeEventPublisher.publishAfterCommit("/topic/stock", "MATERIAL_CHANGED", m.getId().toString(), null, "1", java.util.Map.of("action", "DELETE", "name", m.getName()));
        realtimeEventPublisher.publishAfterCommit("/topic/admin", "MATERIAL_CHANGED", m.getId().toString(), null, "1", java.util.Map.of("action", "DELETE", "name", m.getName()));
    }

    // ==========================================
    // 6. QUẢN LÝ CÔNG THỨC SƠ CHẾ & ĐỊNH MỨC
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

        boolean isActive = request.getActive() == null || Boolean.TRUE.equals(request.getActive());
        if (isActive && preparationRecipeRepository.existsByOutputMaterialIdAndActiveTrue(outMat.getId())) {
            throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED,
                    "Thành phẩm '" + outMat.getName() + "' đã có định mức đang áp dụng. Mỗi thành phẩm chỉ được có một định mức hoạt động.");
        }

        String recipeName = (request.getName() != null && !request.getName().isBlank())
                ? request.getName().trim()
                : "Định mức " + outMat.getName();

        PreparationRecipe recipe = PreparationRecipe.builder()
                .name(recipeName)
                .outputMaterial(outMat)
                .standardOutputQuantity(request.getStandardOutputQuantity())
                .tolerancePercent(request.getTolerancePercent())
                .active(isActive)
                .note(request.getNote() != null ? request.getNote().trim() : null)
                .version(1L)
                .build();
        recipe = preparationRecipeRepository.save(recipe);

        var inputItems = request.getInputItems();
        if (inputItems != null && !inputItems.isEmpty()) {
            List<PreparationRecipeItem> items = new ArrayList<>();
            for (var in : inputItems) {
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
        } else {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Định mức phải có ít nhất một nguyên liệu đầu vào");
        }

        AdminDto.PreparationRecipeResponse resp = getPreparationRecipe(recipe.getId());
        realtimeEventPublisher.publishAfterCommit("/topic/stock", "RECIPE_CHANGED", recipe.getId().toString(), null, "1", java.util.Map.of("action", "CREATE", "outputMaterialId", outMat.getId()));
        realtimeEventPublisher.publishAfterCommit("/topic/admin", "RECIPE_CHANGED", recipe.getId().toString(), null, "1", java.util.Map.of("action", "CREATE", "outputMaterialId", outMat.getId()));
        return resp;
    }

    @Transactional
    public AdminDto.PreparationRecipeResponse updatePreparationRecipe(Long id, AdminDto.CreatePreparationRecipeRequest request) {
        PreparationRecipe recipe = preparationRecipeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy công thức sơ chế"));

        Long targetOutputId = request.getOutputMaterialId() != null ? request.getOutputMaterialId() : recipe.getOutputMaterial().getId();
        boolean willBeActive = request.getActive() != null ? request.getActive() : Boolean.TRUE.equals(recipe.getActive());

        if (willBeActive && preparationRecipeRepository.existsByOutputMaterialIdAndActiveTrueAndIdNot(targetOutputId, id)) {
            throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED,
                    "Thành phẩm này đã có một định mức khác đang áp dụng. Vui lòng tắt định mức cũ trước khi kích hoạt định mức này.");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            recipe.setName(request.getName().trim());
        } else if (recipe.getName() == null || recipe.getName().isBlank()) {
            recipe.setName("Định mức " + recipe.getOutputMaterial().getName());
        }
        if (request.getOutputMaterialId() != null) {
            Material outMat = materialRepository.findById(request.getOutputMaterialId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy bán thành phẩm đầu ra"));
            recipe.setOutputMaterial(outMat);
        }
        if (request.getStandardOutputQuantity() != null) {
            recipe.setStandardOutputQuantity(request.getStandardOutputQuantity());
        }
        if (request.getTolerancePercent() != null) {
            recipe.setTolerancePercent(request.getTolerancePercent());
        }
        if (request.getActive() != null) {
            recipe.setActive(request.getActive());
        }
        if (request.getNote() != null) {
            recipe.setNote(request.getNote().trim());
        }
        long newVersion = (recipe.getVersion() != null ? recipe.getVersion() : 1L) + 1L;
        recipe.setVersion(newVersion);
        preparationRecipeRepository.save(recipe);

        var inputItems = request.getInputItems();
        if (inputItems != null && !inputItems.isEmpty()) {
            preparationRecipeItemRepository.deleteByIdRecipeId(id);
            List<PreparationRecipeItem> items = new ArrayList<>();
            for (var in : inputItems) {
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

        AdminDto.PreparationRecipeResponse resp = getPreparationRecipe(id);
        realtimeEventPublisher.publishAfterCommit("/topic/stock", "RECIPE_CHANGED", id.toString(), null, String.valueOf(newVersion), java.util.Map.of("action", "UPDATE", "outputMaterialId", recipe.getOutputMaterial().getId()));
        realtimeEventPublisher.publishAfterCommit("/topic/admin", "RECIPE_CHANGED", id.toString(), null, String.valueOf(newVersion), java.util.Map.of("action", "UPDATE", "outputMaterialId", recipe.getOutputMaterial().getId()));
        return resp;
    }

    @Transactional
    public void deletePreparationRecipe(Long id) {
        PreparationRecipe r = preparationRecipeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy công thức sơ chế"));
        r.setActive(false);
        preparationRecipeRepository.save(r);
        realtimeEventPublisher.publishAfterCommit("/topic/stock", "RECIPE_CHANGED", id.toString(), null, "1", java.util.Map.of("action", "DEACTIVATE", "outputMaterialId", r.getOutputMaterial().getId()));
        realtimeEventPublisher.publishAfterCommit("/topic/admin", "RECIPE_CHANGED", id.toString(), null, "1", java.util.Map.of("action", "DEACTIVATE", "outputMaterialId", r.getOutputMaterial().getId()));
    }

    private String formatQty(BigDecimal val) {
        if (val == null) return "0";
        return val.stripTrailingZeros().toPlainString();
    }

    private BigDecimal getRawMaterialCost(Long matId) {
        var stockOpt = stockRepository.findByIdMaterialIdAndIdLocation(matId, vn.edu.ute.milktea.entity.inventory.StockLocation.KHO);
        if (stockOpt.isPresent() && stockOpt.get().getCostPrice() != null && stockOpt.get().getCostPrice().signum() > 0) {
            return stockOpt.get().getCostPrice();
        }

        List<ImportReceiptItem> items = importReceiptItemRepository.findByIdMaterialId(matId);
        if (items.isEmpty()) return null;
        BigDecimal sumCost = BigDecimal.ZERO;
        BigDecimal sumQty = BigDecimal.ZERO;
        for (var it : items) {
            if (it.getQuantity() != null && it.getQuantity().signum() > 0 && it.getUnitPrice() != null) {
                sumCost = sumCost.add(it.getQuantity().multiply(it.getUnitPrice()));
                sumQty = sumQty.add(it.getQuantity());
            }
        }
        if (sumQty.signum() <= 0) return null;
        return sumCost.divide(sumQty, 6, RoundingMode.HALF_UP);
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

        // Tỉ lệ pha chế: "1 kg → 5000 ml" hoặc "1 kg Trà Đen, 0.5 kg Đường → 5000 ml"
        String inText = items.stream()
                .map(it -> formatQty(it.getStandardInputQuantity()) + " " + it.getInputMaterial().getUnit()
                        + (items.size() > 1 ? " " + it.getInputMaterial().getName() : ""))
                .collect(java.util.stream.Collectors.joining(", "));
        String outText = formatQty(r.getStandardOutputQuantity()) + " " + (r.getOutputMaterial().getUnit() != null ? r.getOutputMaterial().getUnit() : "");
        String ratioText = inText + " → " + outText;

        // Ước tính giá vốn 1 đơn vị thành phẩm theo định mức
        BigDecimal estimatedUnitCost = null;
        if (r.getStandardOutputQuantity() != null && r.getStandardOutputQuantity().signum() > 0) {
            BigDecimal totalStdCost = BigDecimal.ZERO;
            boolean hasAllPrices = true;
            for (var it : items) {
                BigDecimal p = getRawMaterialCost(it.getInputMaterial().getId());
                if (p != null) {
                    totalStdCost = totalStdCost.add(it.getStandardInputQuantity().multiply(p));
                } else {
                    hasAllPrices = false;
                    break;
                }
            }
            if (hasAllPrices) {
                estimatedUnitCost = totalStdCost.divide(r.getStandardOutputQuantity(), 6, RoundingMode.HALF_UP);
            }
        }

        return AdminDto.PreparationRecipeResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .outputMaterialId(r.getOutputMaterial().getId())
                .outputMaterialName(r.getOutputMaterial().getName())
                .outputUnit(r.getOutputMaterial().getUnit())
                .standardOutputQuantity(r.getStandardOutputQuantity())
                .tolerancePercent(r.getTolerancePercent())
                .active(r.getActive())
                .version(r.getVersion() != null ? r.getVersion() : 1L)
                .note(r.getNote())
                .ratioText(ratioText)
                .estimatedUnitCost(estimatedUnitCost)
                .inputItems(itemDtos)
                .build();
    }
}
