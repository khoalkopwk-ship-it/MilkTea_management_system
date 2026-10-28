package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.BranchDto;
import vn.edu.ute.milktea.dto.ReportDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.cancellation.RefundRequest;
import vn.edu.ute.milktea.entity.cancellation.RefundStatus;
import vn.edu.ute.milktea.entity.inventory.Material;
import vn.edu.ute.milktea.entity.inventory.Stock;
import vn.edu.ute.milktea.entity.inventory.StockId;
import vn.edu.ute.milktea.entity.inventory.StockLocation;
import vn.edu.ute.milktea.entity.order.*;
import vn.edu.ute.milktea.entity.payment.Payment;
import vn.edu.ute.milktea.entity.payment.PaymentMethod;
import vn.edu.ute.milktea.repository.branch.BranchProductRepository;
import vn.edu.ute.milktea.repository.branch.BranchRepository;
import vn.edu.ute.milktea.repository.cancellation.RefundRequestRepository;
import vn.edu.ute.milktea.repository.inventory.ShiftSettlementItemRepository;
import vn.edu.ute.milktea.repository.inventory.ShiftSettlementRepository;
import vn.edu.ute.milktea.repository.inventory.StockMovementRepository;
import vn.edu.ute.milktea.repository.inventory.StockRepository;
import vn.edu.ute.milktea.repository.order.InvoiceRepository;
import vn.edu.ute.milktea.repository.order.OrderItemRepository;
import vn.edu.ute.milktea.repository.order.OrderRepository;
import vn.edu.ute.milktea.repository.payment.PaymentRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.branch.BranchService;
import vn.edu.ute.milktea.service.report.ReportService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Phase6AcceptanceAndReconciliationTest {

    @Mock private OrderRepository orderRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RefundRequestRepository refundRepository;
    @Mock private ShiftSettlementRepository shiftSettlementRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private ShiftSettlementItemRepository shiftSettlementItemRepository;
    @Mock private StockRepository stockRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private BranchProductRepository branchProductRepository;
    @Mock private vn.edu.ute.milktea.repository.catalog.ProductRepository productRepository;

    private ReportService reportService;
    private BranchService branchService;

    private Branch branch1;
    private Branch branch2;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                orderRepository,
                invoiceRepository,
                paymentRepository,
                refundRepository,
                shiftSettlementRepository,
                orderItemRepository,
                stockMovementRepository,
                shiftSettlementItemRepository,
                stockRepository,
                branchRepository
        );

        branchService = new BranchService(
                branchRepository,
                branchProductRepository,
                productRepository
        );

        branch1 = Branch.builder().id(1L).code("CN01").name("MilkTea Quán 01 - Võ Văn Ngân").status("HOAT_DONG").build();
        branch2 = Branch.builder().id(2L).code("CN02").name("MilkTea Quán 02 - D2 Bình Thạnh").status("HOAT_DONG").build();

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch1));
        when(branchRepository.findById(2L)).thenReturn(Optional.of(branch2));
        when(productRepository.findById(100L)).thenReturn(Optional.of(
                vn.edu.ute.milktea.entity.catalog.Product.builder().id(100L).name("Trà Sữa Trân Châu").build()
        ));
    }

    @Test
    @DisplayName("AT-01: Phân quyền & Cô lập đa chi nhánh - Manager CN01 bị chặn can thiệp CN02, ADMIN có toàn quyền")
    void testAT01_BranchScopedAuthorization_AndIsolation() {
        CurrentActor managerActor = CurrentActor.authenticated(2L, Role.MANAGER, 1L, List.of(1L));
        CurrentActor adminActor = CurrentActor.authenticated(1L, Role.ADMIN, 1L, List.of(1L, 2L));

        // 1. Manager CN01 cố tình gọi API chỉnh sửa trạng thái món tại CN02 -> Bị chặn 403 Forbidden
        BusinessException ex = assertThrows(BusinessException.class, () ->
                branchService.updateProductAvailability(2L, 100L, false, managerActor)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());

        // 2. Manager CN01 can thiệp tại đúng CN01 -> Cho phép (không ném lỗi quyền)
        when(branchProductRepository.findByBranchIdAndProductId(1L, 100L)).thenReturn(Optional.empty());
        assertDoesNotThrow(() ->
                branchService.updateProductAvailability(1L, 100L, false, managerActor)
        );

        // 3. Admin toàn quyền can thiệp cả CN01 và CN02
        when(branchProductRepository.findByBranchIdAndProductId(2L, 100L)).thenReturn(Optional.empty());
        assertDoesNotThrow(() ->
                branchService.updateProductAvailability(2L, 100L, false, adminActor)
        );
    }

    @Test
    @DisplayName("AT-02: Đơn lịch sử ONLINE & Đơn tại quán - Tương thích dữ liệu và gắn đúng ngữ cảnh chi nhánh cơ sở")
    void testAT02_MultiBranchDiningTables_AndOnlineOrders() {
        Order onlineOrderCn1 = Order.builder()
                .id(101L)
                .branch(branch1)
                .source(OrderSource.ONLINE)
                .status(OrderStatus.CHO_XAC_NHAN)
                .build();

        Order onlineOrderCn2 = Order.builder()
                .id(102L)
                .branch(branch2)
                .source(OrderSource.ONLINE)
                .status(OrderStatus.CHO_XAC_NHAN)
                .build();

        assertEquals(1L, onlineOrderCn1.getBranch().getId());
        assertEquals(2L, onlineOrderCn2.getBranch().getId());
        assertEquals(OrderSource.ONLINE, onlineOrderCn1.getSource());
        assertNull(onlineOrderCn1.getSession(), "Đơn lịch sử ONLINE không cần và không bị buộc phải có TableSession");
    }

    @Test
    @DisplayName("AT-03: Kho đa chi nhánh độc lập - Khóa PK (MaCN, MaNL, ViTri) tách biệt giữa các cơ sở")
    void testAT03_BranchInventoryIsolation() {
        StockId stockIdCn1 = new StockId(1L, 10L, StockLocation.BEP);
        StockId stockIdCn2 = new StockId(2L, 10L, StockLocation.BEP);

        assertNotEquals(stockIdCn1, stockIdCn2, "Tồn kho nguyên liệu 10 tại Bếp CN01 và CN02 phải có ID khác nhau hoàn toàn");
        assertEquals(1L, stockIdCn1.getBranchId());
        assertEquals(2L, stockIdCn2.getBranchId());
    }

    @Test
    @DisplayName("AT-04: Trải nghiệm khách hàng - Top sản phẩm bán chạy lọc theo chi nhánh")
    void testAT04_TopSellingProducts_FilteredByBranch() {
        LocalDate fromDate = LocalDate.now().minusDays(7);
        LocalDate toDate = LocalDate.now();

        List<Object[]> rowsCn1 = Collections.singletonList(new Object[]{1L, "Trà sữa Trân châu Đường đen (CN01)", 45L});
        when(orderItemRepository.findTopSellingProducts(any(), any(), eq(OrderStatus.DA_HUY), eq(1L), any(PageRequest.class)))
                .thenReturn(rowsCn1);

        List<Object[]> rowsCn2 = Collections.singletonList(new Object[]{2L, "Trà sữa Oolong Nướng (CN02)", 30L});
        when(orderItemRepository.findTopSellingProducts(any(), any(), eq(OrderStatus.DA_HUY), eq(2L), any(PageRequest.class)))
                .thenReturn(rowsCn2);

        List<ReportDto.TopSellingProduct> topCn1 = reportService.getDashboardTopProducts(1L, fromDate, toDate);
        assertEquals(1, topCn1.size());
        assertEquals("Trà sữa Trân châu Đường đen (CN01)", topCn1.get(0).getProductName());
        assertEquals(45L, topCn1.get(0).getQuantitySold());

        List<ReportDto.TopSellingProduct> topCn2 = reportService.getDashboardTopProducts(2L, fromDate, toDate);
        assertEquals(1, topCn2.size());
        assertEquals("Trà sữa Oolong Nướng (CN02)", topCn2.get(0).getProductName());
        assertEquals(30L, topCn2.get(0).getQuantitySold());
    }

    @Test
    @DisplayName("AT-05: Cảnh báo tồn kho theo chi nhánh - Cơ sở này hết hàng không làm sai lệch cảnh báo cơ sở khác")
    void testAT05_StockAlerts_ScopedToBranch() {
        Material matTraDen = Material.builder().id(10L).name("Trà Đen").active(true).unit("g").build();

        Stock stockCn1 = Stock.builder()
                .id(new StockId(1L, 10L, StockLocation.BEP))
                .material(matTraDen)
                .quantity(BigDecimal.ZERO) // Hết hàng tại CN01
                .threshold(BigDecimal.valueOf(500))
                .build();

        Stock stockCn2 = Stock.builder()
                .id(new StockId(2L, 10L, StockLocation.BEP))
                .material(matTraDen)
                .quantity(BigDecimal.valueOf(2000)) // Đầy đủ tại CN02
                .threshold(BigDecimal.valueOf(500))
                .build();

        when(stockRepository.findAll()).thenReturn(List.of(stockCn1, stockCn2));
        when(refundRepository.findByStatusAndBranchId(eq(RefundStatus.CHO_DUYET), any())).thenReturn(Collections.emptyList());

        // Lọc cảnh báo cho CN01 -> 1 món hết hàng
        ReportDto.DashboardAlerts alertsCn1 = reportService.getDashboardAlerts(1L);
        assertEquals(1, alertsCn1.getOutOfStockCount());
        assertEquals(1, alertsCn1.getStockItems().size());
        assertEquals(10L, alertsCn1.getStockItems().get(0).getMaterialId());

        // Lọc cảnh báo cho CN02 -> 0 món hết hàng
        ReportDto.DashboardAlerts alertsCn2 = reportService.getDashboardAlerts(2L);
        assertEquals(0, alertsCn2.getOutOfStockCount());
        assertEquals(0, alertsCn2.getStockItems().size());
    }

    @Test
    @DisplayName("AT-06: Đối soát số học tài chính chuỗi vs từng cơ sở - Revenue(Chain) = Revenue(CN01) + Revenue(CN02)")
    void testAT06_FinancialReconciliation_ChainVsBranches() {
        LocalDate fromDate = LocalDate.now().minusDays(30);
        LocalDate toDate = LocalDate.now();

        // Đơn hàng & Hóa đơn CN01
        Order ord1 = Order.builder().id(1L).branch(branch1).status(OrderStatus.HOAN_THANH).completedAt(Instant.now()).build();
        Invoice inv1 = Invoice.builder().id(1L).order(ord1).totalAmount(BigDecimal.valueOf(150000)).status(InvoiceStatus.HIEU_LUC).build();
        Payment pay1 = Payment.builder().id(1L).invoice(inv1).amount(BigDecimal.valueOf(150000)).paidAt(Instant.now()).method(PaymentMethod.CASH).build();

        // Đơn hàng & Hóa đơn CN02
        Order ord2 = Order.builder().id(2L).branch(branch2).status(OrderStatus.HOAN_THANH).completedAt(Instant.now()).build();
        Invoice inv2 = Invoice.builder().id(2L).order(ord2).totalAmount(BigDecimal.valueOf(250000)).status(InvoiceStatus.HIEU_LUC).build();
        Payment pay2 = Payment.builder().id(2L).invoice(inv2).amount(BigDecimal.valueOf(250000)).paidAt(Instant.now()).method(PaymentMethod.BANK_TRANSFER).build();

        // Hoàn tiền CN01 (20.000đ)
        RefundRequest ref1 = RefundRequest.builder().id(1L).order(ord1).amount(BigDecimal.valueOf(20000)).status(RefundStatus.DA_HOAN).refundedAt(Instant.now()).build();

        // Mock dữ liệu Toàn chuỗi (branchId == null)
        when(paymentRepository.findByPaidAtBetween(any(), any())).thenReturn(List.of(pay1, pay2));
        when(refundRepository.findCompletedRefundsBetween(any(), any())).thenReturn(List.of(ref1));
        when(orderRepository.findByStatusInOrderByCreatedAtAsc(any())).thenReturn(List.of(ord1, ord2));
        when(invoiceRepository.findAll()).thenReturn(List.of(inv1, inv2));
        when(paymentRepository.existsByInvoiceId(1L)).thenReturn(true);
        when(paymentRepository.existsByInvoiceId(2L)).thenReturn(true);

        // Mock dữ liệu CN01 (branchId == 1)
        when(paymentRepository.findByPaidAtBetweenAndBranchId(any(), any(), eq(1L))).thenReturn(List.of(pay1));
        when(refundRepository.findCompletedRefundsBetweenAndBranchId(any(), any(), eq(1L))).thenReturn(List.of(ref1));
        when(orderRepository.findByBranchIdAndStatusInOrderByCreatedAtAsc(eq(1L), any())).thenReturn(List.of(ord1));

        // Mock dữ liệu CN02 (branchId == 2)
        when(paymentRepository.findByPaidAtBetweenAndBranchId(any(), any(), eq(2L))).thenReturn(List.of(pay2));
        when(refundRepository.findCompletedRefundsBetweenAndBranchId(any(), any(), eq(2L))).thenReturn(Collections.emptyList());
        when(orderRepository.findByBranchIdAndStatusInOrderByCreatedAtAsc(eq(2L), any())).thenReturn(List.of(ord2));

        // 1. Tính toán báo cáo tài chính từng cấp
        ReportDto.RevenueReportView chainReport = reportService.getRevenueReport(null, fromDate, toDate);
        ReportDto.RevenueReportView cn1Report = reportService.getRevenueReport(1L, fromDate, toDate);
        ReportDto.RevenueReportView cn2Report = reportService.getRevenueReport(2L, fromDate, toDate);

        // 2. Đối soát Số thực thu: Chain = CN01 + CN02
        assertEquals(0, BigDecimal.valueOf(400000).compareTo(chainReport.getReceived()), "Thực thu toàn chuỗi: 150k + 250k = 400k");
        assertEquals(0, BigDecimal.valueOf(150000).compareTo(cn1Report.getReceived()), "Thực thu CN01: 150k");
        assertEquals(0, BigDecimal.valueOf(250000).compareTo(cn2Report.getReceived()), "Thực thu CN02: 250k");
        assertEquals(0, chainReport.getReceived().compareTo(cn1Report.getReceived().add(cn2Report.getReceived())),
                "Đối soát hoàn hảo: Received(Chain) == Received(CN01) + Received(CN02)");

        // 3. Đối soát Số thực hoàn: Chain = CN01 + CN02
        assertEquals(0, BigDecimal.valueOf(20000).compareTo(chainReport.getRefunded()));
        assertEquals(0, BigDecimal.valueOf(20000).compareTo(cn1Report.getRefunded()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cn2Report.getRefunded()));
        assertEquals(0, chainReport.getRefunded().compareTo(cn1Report.getRefunded().add(cn2Report.getRefunded())),
                "Đối soát hoàn hảo: Refunded(Chain) == Refunded(CN01) + Refunded(CN02)");

        // 4. Đối soát Thu ròng (Net revenue)
        assertEquals(0, BigDecimal.valueOf(380000).compareTo(chainReport.getNetReceived()), "Thu ròng chuỗi: 400k - 20k = 380k");
        assertEquals(0, BigDecimal.valueOf(130000).compareTo(cn1Report.getNetReceived()), "Thu ròng CN01: 150k - 20k = 130k");
        assertEquals(0, BigDecimal.valueOf(250000).compareTo(cn2Report.getNetReceived()), "Thu ròng CN02: 250k - 0 = 250k");
        assertEquals(0, chainReport.getNetReceived().compareTo(cn1Report.getNetReceived().add(cn2Report.getNetReceived())),
                "Đối soát hoàn hảo: NetReceived(Chain) == NetReceived(CN01) + NetReceived(CN02)");

        // 5. Đối soát Doanh số đơn hoàn thành (Completed Order Value)
        when(invoiceRepository.findByOrderId(1L)).thenReturn(Optional.of(inv1));
        when(invoiceRepository.findByOrderId(2L)).thenReturn(Optional.of(inv2));
        ReportDto.RevenueReportView chainReportWithInv = reportService.getRevenueReport(null, fromDate, toDate);
        ReportDto.RevenueReportView cn1ReportWithInv = reportService.getRevenueReport(1L, fromDate, toDate);
        ReportDto.RevenueReportView cn2ReportWithInv = reportService.getRevenueReport(2L, fromDate, toDate);

        assertEquals(0, chainReportWithInv.getCompletedOrderValue().compareTo(
                cn1ReportWithInv.getCompletedOrderValue().add(cn2ReportWithInv.getCompletedOrderValue())
        ), "Đối soát hoàn hảo: CompletedOrderValue(Chain) == CN01 + CN02");
    }

    @Test
    @DisplayName("AT-06: Dashboard tổng quan - Nhận diện đúng phạm vi chi nhánh và tên hiển thị")
    void testAT06_DashboardResponse_ContainsCorrectBranchMeta() {
        LocalDate fromDate = LocalDate.now().minusDays(6);
        LocalDate toDate = LocalDate.now();

        // 1. Toàn chuỗi
        ReportDto.DashboardResponse chainDashboard = reportService.getDashboard(null, fromDate, toDate);
        assertNull(chainDashboard.getBranchId());
        assertEquals("Toàn chuỗi hệ thống", chainDashboard.getBranchName());

        // 2. Chi nhánh 1
        ReportDto.DashboardResponse cn1Dashboard = reportService.getDashboard(1L, fromDate, toDate);
        assertEquals(1L, cn1Dashboard.getBranchId());
        assertEquals("MilkTea Quán 01 - Võ Văn Ngân", cn1Dashboard.getBranchName());

        // 3. Chi nhánh 2
        ReportDto.DashboardResponse cn2Dashboard = reportService.getDashboard(2L, fromDate, toDate);
        assertEquals(2L, cn2Dashboard.getBranchId());
        assertEquals("MilkTea Quán 02 - D2 Bình Thạnh", cn2Dashboard.getBranchName());
    }
}
