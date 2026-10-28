package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.branch.BranchProduct;
import vn.edu.ute.milktea.entity.branch.StaffBranch;
import vn.edu.ute.milktea.entity.catalog.Category;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.table.DiningTable;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.branch.BranchProductRepository;
import vn.edu.ute.milktea.repository.branch.BranchRepository;
import vn.edu.ute.milktea.repository.branch.StaffBranchRepository;
import vn.edu.ute.milktea.repository.catalog.CategoryRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.inventory.ImportReceiptItemRepository;
import vn.edu.ute.milktea.repository.inventory.MaterialRepository;
import vn.edu.ute.milktea.repository.inventory.StockRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeItemRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeRepository;
import vn.edu.ute.milktea.repository.recipe.ProductRecipeRepository;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;
import vn.edu.ute.milktea.security.CurrentActor;
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
public class ManagerRolePermissionsTest {

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
    @Mock BranchRepository branchRepository;
    @Mock StaffBranchRepository staffBranchRepository;
    @Mock BranchProductRepository branchProductRepository;
    @Mock vn.edu.ute.milktea.service.branch.BranchService branchService;

    private AdminService adminService;

    private Branch branch1;
    private Branch branch2;
    private Account managerUser;
    private CurrentActor managerActor;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                accountRepository,
                categoryRepository,
                productRepository,
                productRecipeRepository,
                tableRepository,
                materialRepository,
                preparationRecipeRepository,
                preparationRecipeItemRepository,
                importReceiptItemRepository,
                stockRepository,
                auditRepository,
                realtimeEventPublisher,
                passwordEncoder,
                branchRepository,
                staffBranchRepository,
                branchProductRepository,
                branchService
        );

        branch1 = Branch.builder().id(1L).code("CN1").name("Chi nhánh 1").status("HOAT_DONG").build();
        branch2 = Branch.builder().id(2L).code("CN2").name("Chi nhánh 2").status("HOAT_DONG").build();

        managerUser = Account.builder()
                .id(10L)
                .email("manager1@milktea.vn")
                .fullName("Quản lý Chi nhánh 1")
                .role(Role.MANAGER)
                .active(true)
                .build();

        managerActor = CurrentActor.builder()
                .actorType(CurrentActor.ActorType.AUTHENTICATED)
                .accountId(10L)
                .email("manager1@milktea.vn")
                .role(Role.MANAGER)
                .branchId(1L)
                .build();
    }

    @Test
    @DisplayName("Manager cannot create ADMIN staff account")
    void managerCannotCreateAdminAccount() {
        AdminDto.CreateStaffRequest req = AdminDto.CreateStaffRequest.builder()
                .fullName("Admin Fake")
                .email("fakeadmin@milktea.vn")
                .password("Password123")
                .role(Role.ADMIN)
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminService.createStaffAccount(req, managerActor.getBranchId())
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Thu ngân hoặc Bếp"));
    }

    @Test
    @DisplayName("Manager can create CASHIER staff account bound to their branch")
    void managerCanCreateCashierAccountForOwnBranch() {
        AdminDto.CreateStaffRequest req = AdminDto.CreateStaffRequest.builder()
                .fullName("Cashier CN1")
                .email("cashier1@milktea.vn")
                .password("Password123")
                .role(Role.CASHIER)
                .build();

        when(accountRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setId(99L);
            return a;
        });
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch1));

        AdminDto.StaffResponse resp = adminService.createStaffAccount(req, managerActor.getBranchId());

        assertNotNull(resp);
        assertEquals(Role.CASHIER, resp.getRole());
        assertEquals(1L, resp.getBranchId());
        verify(staffBranchRepository).save(argThat(sb ->
                sb.getBranch().getId().equals(1L) && sb.getAccount().getId().equals(99L)
        ));
    }

    @Test
    @DisplayName("Manager cannot view or edit staff from another branch")
    void managerCannotAccessStaffFromOtherBranch() {
        Account otherStaff = Account.builder().id(20L).email("other@milktea.vn").role(Role.CASHIER).active(true).build();
        when(accountRepository.findById(20L)).thenReturn(Optional.of(otherStaff));
        when(staffBranchRepository.findByAccountId(20L)).thenReturn(List.of(
                StaffBranch.builder().account(otherStaff).branch(branch2).status("HOAT_DONG").build()
        ));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminService.getStaffAccount(20L, managerActor.getBranchId())
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Manager can list tables only for their branch")
    void managerListTablesScopedToBranch() {
        DiningTable table1 = DiningTable.builder().id(101L).name("Bàn 01").branch(branch1).active(true).build();
        when(tableRepository.findByBranchIdOrderByIdAsc(1L)).thenReturn(List.of(table1));

        List<AdminDto.TableResponse> tables = adminService.listTables(managerActor.getBranchId());

        assertEquals(1, tables.size());
        assertEquals("Bàn 01", tables.get(0).getName());
        assertEquals(1L, tables.get(0).getBranchId());
        verify(tableRepository).findByBranchIdOrderByIdAsc(1L);
        verify(tableRepository, never()).findAll();
    }

    @Test
    @DisplayName("Manager cannot view table belonging to another branch")
    void managerCannotViewTableFromOtherBranch() {
        DiningTable table2 = DiningTable.builder().id(202L).name("Bàn 02").branch(branch2).active(true).build();
        when(tableRepository.findById(202L)).thenReturn(Optional.of(table2));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminService.getTable(202L, managerActor.getBranchId())
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Product search for manager reflects branch availability")
    void productSearchReflectsBranchAvailability() {
        Category cat = Category.builder().id(1L).name("Trà sữa").active(true).build();
        Product p1 = Product.builder().id(10L).name("Trà Oolong").category(cat).price(BigDecimal.valueOf(30000)).active(true).build();
        Product p2 = Product.builder().id(11L).name("Trà Đào").category(cat).price(BigDecimal.valueOf(32000)).active(true).build();

        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        BranchProduct bp1 = BranchProduct.builder().product(p1).branch(branch1).available(false).build();
        when(branchProductRepository.findByBranchId(1L)).thenReturn(List.of(bp1));

        AdminDto.ProductPageResponse res = adminService.searchProducts(null, null, null, 1, 10, managerActor.getBranchId());

        assertEquals(2, res.getItems().size());
        AdminDto.ProductResponse respP1 = res.getItems().stream().filter(p -> p.getId().equals(10L)).findFirst().orElseThrow();
        AdminDto.ProductResponse respP2 = res.getItems().stream().filter(p -> p.getId().equals(11L)).findFirst().orElseThrow();

        assertFalse(respP1.getActive(), "p1 should be marked inactive/unavailable for branch1");
        assertTrue(respP2.getActive(), "p2 should default to globally active (true) for branch1");
    }
}
