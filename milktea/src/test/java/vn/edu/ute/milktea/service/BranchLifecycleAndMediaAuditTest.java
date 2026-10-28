package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.BranchDto;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.table.TableSessionStatus;
import vn.edu.ute.milktea.repository.branch.BranchProductRepository;
import vn.edu.ute.milktea.repository.branch.BranchRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.order.OrderRepository;
import vn.edu.ute.milktea.repository.review.ProductReviewRepository;
import vn.edu.ute.milktea.repository.review.ReviewMediaRepository;
import vn.edu.ute.milktea.repository.table.TableSessionRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.branch.BranchService;
import vn.edu.ute.milktea.service.media.ProductImageService;
import vn.edu.ute.milktea.service.review.ProductReviewService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BranchLifecycleAndMediaAuditTest {

    @Mock private BranchRepository branchRepository;
    @Mock private BranchProductRepository branchProductRepository;
    @Mock private ProductRepository productRepository;
    @Mock private TableSessionRepository tableSessionRepository;
    @Mock private OrderRepository orderRepository;

    private BranchService branchService;
    private ProductImageService productImageService;

    @BeforeEach
    void setUp() {
        branchService = new BranchService(
                branchRepository, branchProductRepository, productRepository,
                tableSessionRepository, orderRepository
        );

        productImageService = new ProductImageService(
                null, productRepository, null, null, null
        );
    }

    @Test
    @DisplayName("F26: Từ chối vô hiệu hóa chi nhánh nếu còn bàn đang có phiên mở")
    void testCannotDeactivateBranchWithOpenSessions() {
        Branch branch = Branch.builder().id(10L).name("CN Tân Bình").status("HOAT_DONG").build();
        when(branchRepository.findById(10L)).thenReturn(Optional.of(branch));
        when(tableSessionRepository.countByBranchIdAndStatus(10L, TableSessionStatus.OPEN)).thenReturn(2L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                branchService.toggleBranchStatus(10L, false)
        );

        assertEquals(ErrorCode.ORDER_STATE_CONFLICT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("đang có 2 bàn có phiên"));
        verify(branchRepository, never()).save(any());
    }

    @Test
    @DisplayName("F26: Từ chối vô hiệu hóa chi nhánh nếu còn đơn hàng đang hoạt động")
    void testCannotDeactivateBranchWithActiveOrders() {
        Branch branch = Branch.builder().id(11L).name("CN Bình Thạnh").status("HOAT_DONG").build();
        when(branchRepository.findById(11L)).thenReturn(Optional.of(branch));
        when(tableSessionRepository.countByBranchIdAndStatus(11L, TableSessionStatus.OPEN)).thenReturn(0L);
        when(orderRepository.countByBranchIdAndStatusIn(eq(11L), anyList())).thenReturn(3L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                branchService.toggleBranchStatus(11L, false)
        );

        assertEquals(ErrorCode.ORDER_STATE_CONFLICT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("đang có 3 đơn hàng đang xử lý"));
        verify(branchRepository, never()).save(any());
    }

    @Test
    @DisplayName("F23: Từ chối upload file review media vượt quá 5MB")
    void testRejectReviewMediaExceeding5MB() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", largeBytes);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productImageService.uploadReviewImage(file)
        );

        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("5MB"));
    }

    @Test
    @DisplayName("F23: Từ chối upload file review media không đúng định dạng ảnh cho phép")
    void testRejectReviewMediaInvalidExtension() {
        byte[] content = "malicious script".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "payload.exe", "application/octet-stream", content);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productImageService.uploadReviewImage(file)
        );

        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("JPEG, PNG"));
    }
}
