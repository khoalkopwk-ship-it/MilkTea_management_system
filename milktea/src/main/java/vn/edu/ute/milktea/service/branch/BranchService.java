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
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final BranchProductRepository branchProductRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<BranchDto.BranchResponse> getActiveBranches() {
        return branchRepository.findByActiveTrueOrderByIdAsc().stream()
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
