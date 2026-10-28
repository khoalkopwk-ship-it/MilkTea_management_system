package vn.edu.ute.milktea.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.entity.order.CounterOrderAccess;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.table.TableSession;
import vn.edu.ute.milktea.entity.table.TableSessionAccess;
import vn.edu.ute.milktea.repository.order.CounterOrderAccessRepository;
import vn.edu.ute.milktea.repository.table.TableSessionAccessRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GuestAccessService {

    private final TableSessionAccessRepository sessionAccessRepository;
    private final CounterOrderAccessRepository counterOrderAccessRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @org.springframework.beans.factory.annotation.Value("${app.jwt.secret-base64:c29tZXNlY3JldGtleWZvcm1pbGt0ZWFhcHBsaWNhdGlvbjEyMzQ1Njc4OQ==}")
    private String jwtSecretBase64;

    public record IssuedToken(String rawToken, String tokenHash, Instant expiresAt) {}

    public String issueTableQrCapabilityToken(Long tableId, Long branchId, int ttlHours) {
        Instant exp = Instant.now().plus(ttlHours, ChronoUnit.HOURS);
        String payload = "TQR:" + tableId + ":" + (branchId != null ? branchId : 0) + ":" + exp.toEpochMilli() + ":" + generateRandomOpaqueToken();
        String sig = signPayload(payload);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + sig;
    }

    public boolean validateTableQrCapabilityToken(String token, Long expectedTableId) {
        if (token == null || token.isBlank() || expectedTableId == null) return false;
        String[] parts = token.split("\\.");
        if (parts.length != 2) return false;
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String sig = parts[1];
            String expectedSig = signPayload(payload);
            if (!MessageDigest.isEqual(sig.getBytes(StandardCharsets.UTF_8), expectedSig.getBytes(StandardCharsets.UTF_8))) {
                return false;
            }
            String[] tokens = payload.split(":");
            if (tokens.length < 5 || !"TQR".equals(tokens[0])) return false;
            Long tableId = Long.parseLong(tokens[1]);
            long expMillis = Long.parseLong(tokens[3]);
            if (!tableId.equals(expectedTableId)) return false;
            if (Instant.now().toEpochMilli() > expMillis) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String signPayload(String payload) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            byte[] keyBytes = (jwtSecretBase64 != null && !jwtSecretBase64.isBlank())
                    ? Base64.getDecoder().decode(jwtSecretBase64)
                    : "milktea-secret-fallback-key-256bit!".getBytes(StandardCharsets.UTF_8);
            mac.init(new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi ký QR token", e);
        }
    }

    public IssuedToken issueTableSessionToken(TableSession session, int ttlHours) {
        String rawToken = generateRandomOpaqueToken();
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(ttlHours, ChronoUnit.HOURS);

        TableSessionAccess access = TableSessionAccess.builder()
                .session(session)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();
        sessionAccessRepository.save(access);

        return new IssuedToken(rawToken, tokenHash, expiresAt);
    }

    public IssuedToken issueCounterOrderToken(Order order, int ttlHours) {
        String rawToken = generateRandomOpaqueToken();
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(ttlHours, ChronoUnit.HOURS);

        CounterOrderAccess access = CounterOrderAccess.builder()
                .order(order)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();
        counterOrderAccessRepository.save(access);

        return new IssuedToken(rawToken, tokenHash, expiresAt);
    }

    @Transactional(readOnly = true)
    public Optional<TableSessionAccess> validateTableToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();
        String hash = hashToken(rawToken);
        return sessionAccessRepository.findByTokenHashAndRevokedAtIsNull(hash)
                .filter(a -> a.getExpiresAt().isAfter(Instant.now()))
                .filter(a -> a.getSession().getStatus() == vn.edu.ute.milktea.entity.table.TableSessionStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public Optional<CounterOrderAccess> validateCounterToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();
        String hash = hashToken(rawToken);
        return counterOrderAccessRepository.findByTokenHashAndRevokedAtIsNull(hash)
                .filter(a -> a.getExpiresAt().isAfter(Instant.now()));
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi hash token", e);
        }
    }

    private String generateRandomOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
