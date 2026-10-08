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

    public record IssuedToken(String rawToken, String tokenHash, Instant expiresAt) {}

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
                .filter(a -> a.getExpiresAt().isAfter(Instant.now()));
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
