package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.branch.BranchProduct;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.inventory.StockId;
import vn.edu.ute.milktea.entity.inventory.StockLocation;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.order.OrderSource;
import vn.edu.ute.milktea.entity.table.DiningTable;
import vn.edu.ute.milktea.security.CurrentActor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Phase2MultiBranchDataTest {

    @Test
    @DisplayName("Actor Scoping: ADMIN có quyền truy cập mọi chi nhánh")
    void testAdminCanAccessAnyBranch() {
        CurrentActor admin = CurrentActor.authenticated(1L, Role.ADMIN);

        assertTrue(admin.canAccessBranch(1L));
        assertTrue(admin.canAccessBranch(2L));
        assertTrue(admin.canAccessBranch(999L));
    }

    @Test
    @DisplayName("Actor Scoping: Nhân viên chi nhánh chỉ có quyền truy cập chi nhánh được phân công")
    void testStaffCanOnlyAccessAssignedBranches() {
        CurrentActor cashierBranch1 = CurrentActor.authenticated(2L, Role.CASHIER, 1L, List.of(1L));

        assertTrue(cashierBranch1.canAccessBranch(1L), "Thu ngân CN01 phải truy cập được CN01");
        assertFalse(cashierBranch1.canAccessBranch(2L), "Thu ngân CN01 không được truy cập CN02");
        assertFalse(cashierBranch1.canAccessBranch(null), "Null branchId phải bị từ chối");
    }

    @Test
    @DisplayName("Actor Scoping: Khách bàn TABLE_GUEST bị giới hạn trong chi nhánh của bàn đó")
    void testTableGuestBranchScoping() {
        CurrentActor guestBranch2 = CurrentActor.tableGuest(50L, 10L, 2L);

        assertEquals(2L, guestBranch2.getBranchId());
        assertTrue(guestBranch2.canAccessBranch(2L));
        assertFalse(guestBranch2.canAccessBranch(1L));
    }

    @Test
    @DisplayName("StockId: Tách biệt khóa chính kho giữa các chi nhánh")
    void testStockIdBranchSegregation() {
        StockId stockBranch1 = new StockId(1L, 10L, StockLocation.BEP);
        StockId stockBranch2 = new StockId(2L, 10L, StockLocation.BEP);
        StockId stockBranch1Duplicate = new StockId(1L, 10L, StockLocation.BEP);

        assertEquals(stockBranch1, stockBranch1Duplicate);
        assertEquals(stockBranch1.hashCode(), stockBranch1Duplicate.hashCode());

        assertNotEquals(stockBranch1, stockBranch2, "Cùng nguyên liệu và vị trí nhưng khác chi nhánh phải là 2 bản ghi khác biệt");
        assertNotEquals(stockBranch1.hashCode(), stockBranch2.hashCode());
    }

    @Test
    @DisplayName("DiningTable: Bàn gắn liền chi nhánh và hỗ trợ trùng tên giữa các cơ sở")
    void testTableBranchAssociation() {
        Branch branch1 = Branch.builder().id(1L).code("CN01").name("Cơ sở 1").build();
        Branch branch2 = Branch.builder().id(2L).code("CN02").name("Cơ sở 2").build();

        DiningTable table1 = DiningTable.builder()
                .id(1L)
                .name("Bàn 01")
                .branch(branch1)
                .build();

        DiningTable table2 = DiningTable.builder()
                .id(2L)
                .name("Bàn 01")
                .branch(branch2)
                .build();

        assertEquals(table1.getName(), table2.getName());
        assertNotEquals(table1.getBranch().getId(), table2.getBranch().getId());
    }

    @Test
    @DisplayName("Order: Hỗ trợ nguồn đơn ONLINE và gắn liền chi nhánh tiếp nhận")
    void testOnlineOrderBranchAssociation() {
        Branch branch2 = Branch.builder().id(2L).code("CN02").name("Cơ sở 2").build();

        Order onlineOrder = Order.builder()
                .id(100L)
                .branch(branch2)
                .source(OrderSource.ONLINE)
                .build();

        assertEquals(OrderSource.ONLINE, onlineOrder.getSource());
        assertEquals(2L, onlineOrder.getBranch().getId());
    }

    @Test
    @DisplayName("BranchProduct: Cho phép chi nhánh tạm ngưng phục vụ món cụ thể")
    void testBranchProductAvailability() {
        Branch branch1 = Branch.builder().id(1L).code("CN01").build();
        Product tea = Product.builder().id(10L).name("Trà sữa Oolong").build();

        BranchProduct bp = BranchProduct.builder()
                .branch(branch1)
                .product(tea)
                .active(true)
                .available(false) // Tạm hết tại cơ sở 1
                .build();

        assertTrue(bp.getActive(), "Món vẫn thuộc danh mục kinh doanh");
        assertFalse(bp.getAvailable(), "Món tạm ngưng phục vụ tại chi nhánh");
    }
}
