package vn.edu.ute.milktea.service.branch;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.BranchDto;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.branch.BranchProduct;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.repository.branch.BranchProductRepository;
import vn.edu.ute.milktea.repository.branch.BranchRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.security.CurrentActor;

import java.time.Instant;
import java.util.List;

@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final BranchProductRepository branchProductRepository;
    private final ProductRepository productRepository;
    private final vn.edu.ute.milktea.repository.table.TableSessionRepository tableSessionRepository;
    private final vn.edu.ute.milktea.repository.order.OrderRepository orderRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public BranchService(BranchRepository branchRepository,
                         BranchProductRepository branchProductRepository,
                         ProductRepository productRepository,
                         vn.edu.ute.milktea.repository.table.TableSessionRepository tableSessionRepository,
                         vn.edu.ute.milktea.repository.order.OrderRepository orderRepository) {
        this.branchRepository = branchRepository;
        this.branchProductRepository = branchProductRepository;
        this.productRepository = productRepository;
        this.tableSessionRepository = tableSessionRepository;
        this.orderRepository = orderRepository;
    }

    public BranchService(BranchRepository branchRepository, BranchProductRepository branchProductRepository, ProductRepository productRepository) {
        this(branchRepository, branchProductRepository, productRepository, null, null);
    }

    public static final String CENTRAL_WAREHOUSE_CODE = "KHOTONG";

    @Transactional
    public Branch getOrCreateCentralWarehouse() {
        return branchRepository.findByCode(CENTRAL_WAREHOUSE_CODE)
                .orElseGet(() -> {
                    Branch branch = Branch.builder()
                            .code(CENTRAL_WAREHOUSE_CODE)
                            .name("Kho Hàng Tổng (Toàn Chuỗi)")
                            .address("Tổng Kho Trung Tâm MilkTea Chain, TP.HCM")
                            .phone("19001234")
                            .status("HOAT_DONG")
                            .createdAt(Instant.now())
                            .build();
                    return branchRepository.save(branch);
                });
    }

    @Transactional(readOnly = true)
    public boolean isCentralWarehouse(Long branchId) {
        if (branchId == null) return false;
        return branchRepository.findById(branchId)
                .map(b -> CENTRAL_WAREHOUSE_CODE.equalsIgnoreCase(b.getCode()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<BranchDto.BranchResponse> getActiveBranches() {
        return branchRepository.findByActiveTrueOrderByIdAsc().stream()
                .filter(b -> !CENTRAL_WAREHOUSE_CODE.equalsIgnoreCase(b.getCode()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchDto.BranchResponse getBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy chi nhánh"));
        return toResponse(branch);
    }

    @Transactional
    public void updateProductAvailability(Long branchId, Long productId, boolean available, CurrentActor actor) {
        if (actor == null) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Yêu cầu đăng nhập");
        }
        boolean isAuthorized = actor.hasRole(Role.ADMIN) || (actor.hasRole(Role.MANAGER) && actor.canAccessBranch(branchId));
        if (!isAuthorized) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Không có quyền quản lý thực đơn chi nhánh này");
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy chi nhánh"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.PRODUCT_UNAVAILABLE, "Không tìm thấy món"));

        BranchProduct bp = branchProductRepository.findByBranchIdAndProductId(branchId, productId)
                .orElseGet(() -> BranchProduct.builder()
                        .branch(branch)
                        .product(product)
                        .active(true)
                        .available(available)
                        .build());
        bp.setAvailable(available);
        bp.setUpdatedAt(Instant.now());
        branchProductRepository.save(bp);
    }

    @Transactional
    public BranchDto.BranchResponse createBranch(BranchDto.CreateBranchRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (branchRepository.findByCode(code).isPresent()) {
            throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED, "Mã chi nhánh " + code + " đã tồn tại");
        }
        Branch branch = Branch.builder()
                .code(code)
                .name(request.getName().trim())
                .address(request.getAddress().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .status("HOAT_DONG")
                .createdAt(Instant.now())
                .build();
        branch = branchRepository.save(branch);
        return toResponse(branch);
    }

    @Transactional
    public BranchDto.BranchResponse updateBranch(Long id, BranchDto.UpdateBranchRequest request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy chi nhánh"));
        branch.setName(request.getName().trim());
        branch.setAddress(request.getAddress().trim());
        if (request.getPhone() != null) {
            branch.setPhone(request.getPhone().trim());
        }
        branch = branchRepository.save(branch);
        return toResponse(branch);
    }

    @Transactional
    public BranchDto.BranchResponse toggleBranchStatus(Long branchId, boolean active) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy chi nhánh"));

        if (!active) {
            // F26: Kiểm tra phiên bàn mở
            long openSessions = tableSessionRepository.countByBranchIdAndStatus(branchId, vn.edu.ute.milktea.entity.table.TableSessionStatus.OPEN);
            if (openSessions > 0) {
                throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT,
                        "Không thể vô hiệu hóa chi nhánh vì đang có " + openSessions + " bàn có phiên sử dụng mở");
            }
            // F26: Kiểm tra đơn hàng đang xử lý
            List<vn.edu.ute.milktea.entity.order.OrderStatus> activeOrderStatuses = List.of(
                    vn.edu.ute.milktea.entity.order.OrderStatus.CHO_THANH_TOAN,
                    vn.edu.ute.milktea.entity.order.OrderStatus.CHO_XAC_NHAN,
                    vn.edu.ute.milktea.entity.order.OrderStatus.CHO_CHE_BIEN,
                    vn.edu.ute.milktea.entity.order.OrderStatus.DANG_CHE_BIEN
            );
            long activeOrders = orderRepository.countByBranchIdAndStatusIn(branchId, activeOrderStatuses);
            if (activeOrders > 0) {
                throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT,
                        "Không thể vô hiệu hóa chi nhánh vì đang có " + activeOrders + " đơn hàng đang xử lý");
            }
            branch.setStatus("DONG_CUA");
        } else {
            branch.setStatus("HOAT_DONG");
        }
        branchRepository.save(branch);
        return toResponse(branch);
    }

    private BranchDto.BranchResponse toResponse(Branch branch) {
        return BranchDto.BranchResponse.builder()
                .id(branch.getId())
                .code(branch.getCode())
                .name(branch.getName())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .status(branch.isActive() ? "HOAT_DONG" : "DONG_CUA")
                .build();
    }
}
