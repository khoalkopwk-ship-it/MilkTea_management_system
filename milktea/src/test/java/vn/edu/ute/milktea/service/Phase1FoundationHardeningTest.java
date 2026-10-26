package vn.edu.ute.milktea.service;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.controller.CartController;
import vn.edu.ute.milktea.entity.account.EmailVerificationOtp;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.common.RequestDedupRecord;
import vn.edu.ute.milktea.repository.account.EmailVerificationOtpRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.common.RequestDedupRecordRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.auth.OtpAttemptRecorder;
import vn.edu.ute.milktea.service.common.IdempotencyService;
import vn.edu.ute.milktea.service.media.ProductImageService;
import vn.edu.ute.milktea.service.table.SessionCartService;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Phase1FoundationHardeningTest {

    // --- SEC-02.2: OTP Attempt Recorder ---
    @Mock
    private EmailVerificationOtpRepository otpRepository;

    @InjectMocks
    private OtpAttemptRecorder otpAttemptRecorder;

    // --- SEC-02.3: Idempotency Service ---
    @Mock
    private RequestDedupRecordRepository dedupRecordRepository;

    @InjectMocks
    private IdempotencyService idempotencyService;

    // --- MEDIA-01: ProductImageService ---
    @Mock
    private ProductRepository productRepository;

    @Mock
    private Cloudinary cloudinary;

    @InjectMocks
    private ProductImageService productImageService;

    // --- SEC-02.4: Cart Ownership ---
    @Mock
    private SessionCartService sessionCartService;

    @InjectMocks
    private CartController cartController;

    @Test
    @DisplayName("OTP: Ghi nhận số lần thử sai tăng dần và thu hồi khi đạt ngưỡng tối đa")
    void testOtpAttemptRecorderIncrementsAndRevokes() {
        EmailVerificationOtp otp = EmailVerificationOtp.builder()
                .id(100L)
                .otpHash("hashed_otp")
                .attempts(4)
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .build();

        when(otpRepository.findById(100L)).thenReturn(Optional.of(otp));
        when(otpRepository.saveAndFlush(any(EmailVerificationOtp.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int newAttempts = otpAttemptRecorder.recordFailedAttempt(100L, 5);

        assertEquals(5, newAttempts);
        assertEquals(5, otp.getAttempts());
        assertNotNull(otp.getRevokedAt(), "OTP phải bị thu hồi khi đạt maxAttempts=5");
        verify(otpRepository).saveAndFlush(otp);
    }

    @Test
    @DisplayName("Idempotency: Cùng Key và Fingerprint trả về resourceId trước đó mà không xử lý lại")
    void testIdempotencyReturnsExistingWhenFingerprintMatches() {
        String scope = "ORDER_CREATE";
        String key = "test-idem-key-1";
        String fingerprint = idempotencyService.computeFingerprint("{\"total\": 50000}", 123L);

        RequestDedupRecord record = RequestDedupRecord.builder()
                .id(1L)
                .scope(scope)
                .idempotencyKey(key)
                .payloadFingerprint(fingerprint)
                .resourceType("DON_HANG")
                .resourceId(999L)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        when(dedupRecordRepository.findByScopeAndIdempotencyKey(scope, key))
                .thenReturn(Optional.of(record));

        Optional<Long> existingId = idempotencyService.checkExisting(scope, key, fingerprint);

        assertTrue(existingId.isPresent());
        assertEquals(999L, existingId.get());
    }

    @Test
    @DisplayName("Idempotency: Cùng Key nhưng Payload khác nhau phải ném IDEMPOTENCY_CONFLICT (409)")
    void testIdempotencyThrowsConflictOnMismatchedFingerprint() {
        String scope = "ORDER_CREATE";
        String key = "test-idem-key-conflict";
        String oldFingerprint = idempotencyService.computeFingerprint("{\"total\": 50000}", 123L);
        String newFingerprint = idempotencyService.computeFingerprint("{\"total\": 100000}", 123L);

        RequestDedupRecord record = RequestDedupRecord.builder()
                .id(1L)
                .scope(scope)
                .idempotencyKey(key)
                .payloadFingerprint(oldFingerprint)
                .resourceType("DON_HANG")
                .resourceId(999L)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        when(dedupRecordRepository.findByScopeAndIdempotencyKey(scope, key))
                .thenReturn(Optional.of(record));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                idempotencyService.checkExisting(scope, key, newFingerprint)
        );

        assertEquals(ErrorCode.IDEMPOTENCY_CONFLICT, exception.getErrorCode());
    }

    @Test
    @DisplayName("Idempotency: Lưu bản ghi mới thành công với TTL")
    void testIdempotencyRecordCreation() {
        String scope = "PAYMENT_RECEIPT";
        String key = "receipt-key-01";
        String fingerprint = idempotencyService.computeFingerprint("{\"invoiceId\":10}", 5L);

        idempotencyService.record(scope, key, fingerprint, "HOA_DON", 888L, 24);

        ArgumentCaptor<RequestDedupRecord> captor = ArgumentCaptor.forClass(RequestDedupRecord.class);
        verify(dedupRecordRepository).save(captor.capture());

        RequestDedupRecord saved = captor.getValue();
        assertEquals(scope, saved.getScope());
        assertEquals(key, saved.getIdempotencyKey());
        assertEquals(fingerprint, saved.getPayloadFingerprint());
        assertEquals(888L, saved.getResourceId());
        assertTrue(saved.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Cart Ownership: Khách bàn TABLE_GUEST không được truy cập giỏ hàng của phiên khác")
    void testTableGuestCannotAccessOtherSessionCart() throws Exception {
        CurrentActor guestActor = CurrentActor.tableGuest(10L, 1L);

        Method method = CartController.class.getDeclaredMethod("resolveAndVerifySessionId", Long.class, Long.class, CurrentActor.class);
        method.setAccessible(true);

        // Guest session is 10L, but requests 20L
        Exception exception = null;
        try {
            method.invoke(cartController, 20L, null, guestActor);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            if (ite.getCause() instanceof BusinessException be) {
                exception = be;
            }
        }

        assertNotNull(exception);
        assertTrue(exception instanceof BusinessException);
        assertEquals(ErrorCode.ACCESS_DENIED, ((BusinessException) exception).getErrorCode());
    }

    @Test
    @DisplayName("Cart Ownership: Khách bàn TABLE_GUEST truy cập đúng giỏ hàng phiên của mình thành công")
    void testTableGuestCanAccessOwnSessionCart() throws Exception {
        CurrentActor guestActor = CurrentActor.tableGuest(10L, 1L);

        Method method = CartController.class.getDeclaredMethod("resolveAndVerifySessionId", Long.class, Long.class, CurrentActor.class);
        method.setAccessible(true);

        Long resolved = (Long) method.invoke(cartController, 10L, null, guestActor);
        assertEquals(10L, resolved);
    }
}
