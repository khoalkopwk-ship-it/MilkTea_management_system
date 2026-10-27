package vn.edu.ute.milktea.repository.voucher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.voucher.VoucherUsageHistory;
import vn.edu.ute.milktea.entity.voucher.VoucherUsageStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherUsageHistoryRepository extends JpaRepository<VoucherUsageHistory, Long> {

    long countByVoucherIdAndAccountIdAndStatus(Long voucherId, Long accountId, VoucherUsageStatus status);

    Optional<VoucherUsageHistory> findByOrderId(Long orderId);

    List<VoucherUsageHistory> findByVoucherIdOrderByUsedAtDesc(Long voucherId);

    Page<VoucherUsageHistory> findByVoucherIdOrderByUsedAtDesc(Long voucherId, Pageable pageable);

    List<VoucherUsageHistory> findByAccountIdOrderByUsedAtDesc(Long accountId);
}
