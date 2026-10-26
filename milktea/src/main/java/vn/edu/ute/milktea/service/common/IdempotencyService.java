package vn.edu.ute.milktea.service.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.entity.common.RequestDedupRecord;
import vn.edu.ute.milktea.repository.common.RequestDedupRecordRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RequestDedupRecordRepository dedupRecordRepository;

    /**
     * Tạo mã băm SHA-256 làm fingerprint cho payload và actor.
     */
    public String computeFingerprint(String payloadJson, Long actorId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = (actorId != null ? actorId.toString() : "GUEST") + ":" + (payloadJson != null ? payloadJson : "");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Lỗi băm fingerprint payload idempotency: {}", e.getMessage());
            return String.valueOf(combinedString(payloadJson, actorId).hashCode());
        }
    }

    private String combinedString(String payload, Long actorId) {
        return (actorId != null ? actorId.toString() : "GUEST") + ":" + (payload != null ? payload : "");
    }

    /**
     * Kiểm tra xem request với key này đã từng xử lý chưa.
     * - Nếu chưa có: trả về Optional.empty().
     * - Nếu đã có và fingerprint khớp: trả về Optional chứa MaTaiNguyen (resourceId) để tái sử dụng.
     * - Nếu đã có nhưng fingerprint KHÁC (cùng key khác nội dung): ném lỗi IDEMPOTENCY_CONFLICT (409 Conflict).
     */
    @Transactional(readOnly = true)
    public Optional<Long> checkExisting(String scope, String idempotencyKey, String currentFingerprint) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        var recordOpt = dedupRecordRepository.findByScopeAndIdempotencyKey(scope, idempotencyKey);
        if (recordOpt.isEmpty()) {
            return Optional.empty();
        }

        RequestDedupRecord record = recordOpt.get();

        // Kiểm tra hết hạn
        if (record.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }

        // Kiểm tra dấu vân payload
        if (currentFingerprint != null && record.getPayloadFingerprint() != null) {
            if (!currentFingerprint.equals(record.getPayloadFingerprint())) {
                log.warn("Xung đột Idempotency: scope={}, key={}, fingerprint cũ={}, mới={}",
                        scope, idempotencyKey, record.getPayloadFingerprint(), currentFingerprint);
                throw BusinessException.conflict(
                        ErrorCode.IDEMPOTENCY_CONFLICT,
                        "Khóa yêu cầu (Idempotency-Key) đã được sử dụng trước đó cho một thao tác khác nội dung."
                );
            }
        }

        return Optional.ofNullable(record.getResourceId());
    }

    /**
     * Ghi nhận bản ghi chống lặp sau khi thao tác nghiệp vụ thành công.
     */
    @Transactional
    public void record(String scope, String idempotencyKey, String payloadFingerprint, String resourceType, Long resourceId, int ttlHours) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        Instant now = Instant.now();
        RequestDedupRecord record = RequestDedupRecord.builder()
                .scope(scope)
                .idempotencyKey(idempotencyKey)
                .payloadFingerprint(payloadFingerprint)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .createdAt(now)
                .expiresAt(now.plus(ttlHours, ChronoUnit.HOURS))
                .build();

        dedupRecordRepository.save(record);
        log.debug("Đã lưu bản ghi chống lặp: scope={}, key={}, resourceId={}", scope, idempotencyKey, resourceId);
    }
}
