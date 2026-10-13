package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.AdminDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.inventory.*;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipe;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipeItem;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipeItemId;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.inventory.*;
import vn.edu.ute.milktea.repository.catalog.CategoryRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeItemRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeRepository;
import vn.edu.ute.milktea.repository.recipe.ProductRecipeRepository;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;
import vn.edu.ute.milktea.service.admin.AdminService;
import vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AdminPreparationRecipeAndStaffTest {

    @Mock AccountRepository accountRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock ProductRepository productRepository;
    @Mock ProductRecipeRepository productRecipeRepository;
    @Mock DiningTableRepository tableRepository;
    @Mock MaterialRepository materialRepository;
    @Mock PreparationRecipeRepository preparationRecipeRepository;
    @Mock PreparationRecipeItemRepository preparationRecipeItemRepository;
    @Mock ImportReceiptItemRepository importReceiptItemRepository;
    @Mock StockRepository stockRepository;
    @Mock BusinessAuditRepository auditRepository;
    @Mock RealtimeEventPublisher realtimeEventPublisher;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AdminService adminService;

    private Material rawTea;
    private Material preparedTea;
    private PreparationRecipe existingRecipe;

    @BeforeEach
    void setUp() {
        rawTea = Material.builder()
                .id(10L)
                .name("Trà Đen Thô")
                .type(MaterialType.THO)
                .unit("kg")
                .build();

        preparedTea = Material.builder()
                .id(20L)
                .name("Nước Cốt Trà Đen")
                .type(MaterialType.SOCHE)
                .unit("ml")
                .build();

        existingRecipe = PreparationRecipe.builder()
                .id(100L)
                .name("Định mức Nước Cốt Trà Đen")
                .outputMaterial(preparedTea)
                .standardOutputQuantity(BigDecimal.valueOf(5000))
                .active(true)
                .version(1L)
                .build();

        when(materialRepository.findById(10L)).thenReturn(Optional.of(rawTea));
        when(materialRepository.findById(20L)).thenReturn(Optional.of(preparedTea));
    }

    @Test
    @DisplayName("3. Tạo định mức: Tự sinh tên khi request name bị null/trống, kiểm tra duy nhất 1 định mức active")
    void testCreatePreparationRecipeAutoNameAndSingleActive() {
        // Giả lập thành phẩm 20 chưa có công thức active
        when(preparationRecipeRepository.existsByOutputMaterialIdAndActiveTrue(20L)).thenReturn(false);

        when(preparationRecipeRepository.save(any(PreparationRecipe.class))).thenAnswer(inv -> {
            PreparationRecipe r = inv.getArgument(0);
            r.setId(101L);
            return r;
        });

        // Mock getPreparationRecipe(101L)
        PreparationRecipeItem item = PreparationRecipeItem.builder()
                .id(new PreparationRecipeItemId(101L, 10L))
                .recipe(existingRecipe)
                .inputMaterial(rawTea)
                .standardInputQuantity(BigDecimal.valueOf(1.0))
                .build();
        when(preparationRecipeRepository.findById(101L)).thenReturn(Optional.of(existingRecipe));
        when(preparationRecipeItemRepository.findByIdRecipeId(101L)).thenReturn(List.of(item));

        // Request không gửi "name", gửi inputItems
        AdminDto.CreatePreparationRecipeRequest request = AdminDto.CreatePreparationRecipeRequest.builder()
                .name(null) // Null tên
                .outputMaterialId(20L)
                .standardOutputQuantity(BigDecimal.valueOf(5000.0))
                .active(true)
                .inputItems(List.of(
                        AdminDto.PrepRecipeItemDto.builder()
                                .inputMaterialId(10L)
                                .standardQuantity(BigDecimal.valueOf(1.0))
                                .build()
                ))
                .build();

        AdminDto.PreparationRecipeResponse response = adminService.createPreparationRecipe(request);

        // Kiểm tra lưu entity có tên tự sinh "Định mức Nước Cốt Trà Đen"
        verify(preparationRecipeRepository).save(argThat(r -> 
                "Định mức Nước Cốt Trà Đen".equals(r.getName()) &&
                r.getStandardOutputQuantity().compareTo(BigDecimal.valueOf(5000.0)) == 0 &&
                Boolean.TRUE.equals(r.getActive())
        ));

        // Kiểm tra phát WebSocket RECIPE_CHANGED
        verify(realtimeEventPublisher).publishAfterCommit(eq("/topic/stock"), eq("RECIPE_CHANGED"), eq("101"), any(), any(), any());
    }

    @Test
    @DisplayName("3. Tạo định mức: Báo lỗi nếu đã có định mức active cho thành phẩm này")
    void testCreatePreparationRecipeDuplicateActiveThrows() {
        when(preparationRecipeRepository.existsByOutputMaterialIdAndActiveTrue(20L)).thenReturn(true);

        AdminDto.CreatePreparationRecipeRequest request = AdminDto.CreatePreparationRecipeRequest.builder()
                .outputMaterialId(20L)
                .standardOutputQuantity(BigDecimal.valueOf(5000.0))
                .active(true)
                .inputItems(List.of(
                        AdminDto.PrepRecipeItemDto.builder()
                                .inputMaterialId(10L)
                                .standardQuantity(BigDecimal.valueOf(1.0))
                                .build()
                ))
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminService.createPreparationRecipe(request)
        );
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("đã có định mức đang áp dụng"));
    }

    @Test
    @DisplayName("3. Chỉnh sửa định mức: Giữ nguyên ID, tăng version, cập nhật items, phát WebSocket")
    void testUpdatePreparationRecipeKeepsIdAndIncrementsVersion() {
        when(preparationRecipeRepository.findById(100L)).thenReturn(Optional.of(existingRecipe));
        when(preparationRecipeRepository.existsByOutputMaterialIdAndActiveTrueAndIdNot(20L, 100L)).thenReturn(false);

        PreparationRecipeItem updatedItem = PreparationRecipeItem.builder()
                .id(new PreparationRecipeItemId(100L, 10L))
                .recipe(existingRecipe)
                .inputMaterial(rawTea)
                .standardInputQuantity(BigDecimal.valueOf(1.2)) // Thay đổi lượng đầu vào
                .build();
        when(preparationRecipeItemRepository.findByIdRecipeId(100L)).thenReturn(List.of(updatedItem));

        AdminDto.CreatePreparationRecipeRequest request = AdminDto.CreatePreparationRecipeRequest.builder()
                .name("Định mức Nước Cốt Trà Đen Đậm Vị")
                .outputMaterialId(20L)
                .standardOutputQuantity(BigDecimal.valueOf(5000.0))
                .active(true)
                .inputItems(List.of(
                        AdminDto.PrepRecipeItemDto.builder()
                                .inputMaterialId(10L)
                                .standardQuantity(BigDecimal.valueOf(1.2))
                                .build()
                ))
                .build();

        AdminDto.PreparationRecipeResponse resp = adminService.updatePreparationRecipe(100L, request);

        // Đảm bảo ID được giữ nguyên là 100L, version tăng từ 1 lên 2
        assertEquals(100L, existingRecipe.getId());
        assertEquals(2L, existingRecipe.getVersion());
        assertEquals("Định mức Nước Cốt Trà Đen Đậm Vị", existingRecipe.getName());

        // Kiểm tra xóa items cũ và lưu items mới
        verify(preparationRecipeItemRepository).deleteByIdRecipeId(100L);
        verify(preparationRecipeItemRepository).saveAll(any());

        // Kiểm tra phát WebSocket RECIPE_CHANGED với version 2
        verify(realtimeEventPublisher).publishAfterCommit(eq("/topic/stock"), eq("RECIPE_CHANGED"), eq("100"), any(), eq("2"), any());
    }

    @Test
    @DisplayName("2. Quản lý nhân viên: Thêm, sửa, khóa/mở khóa tài khoản phát WebSocket STAFF_CHANGED")
    void testStaffAccountRealtimeSynchronization() {
        Account staff = Account.builder()
                .id(50L)
                .email("cashier1@milktea.vn")
                .fullName("Thu ngân 1")
                .role(Role.CASHIER)
                .active(true)
                .tokenVersion(1L)
                .build();

        when(accountRepository.findById(50L)).thenReturn(Optional.of(staff));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        // Khóa tài khoản
        adminService.toggleStaffStatus(50L, false, 1L);

        // Kiểm tra tài khoản bị khóa và tokenVersion tăng
        assertFalse(staff.getActive());
        assertEquals(2L, staff.getTokenVersion());

        // Kiểm tra phát WebSocket STAFF_CHANGED after commit tới /topic/admin
        verify(realtimeEventPublisher).publishAfterCommit(eq("/topic/admin"), eq("STAFF_CHANGED"), eq("50"), any(), any(), any());
    }
}
