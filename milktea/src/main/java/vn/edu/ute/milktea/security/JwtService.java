package vn.edu.ute.milktea.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.edu.ute.milktea.entity.account.Account;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JwtService {

    private final byte[] secretKey;
    private final String issuer;
    private final String audience;
    private final long ttlMinutes;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${app.jwt.secret-base64}") String secretBase64,
            @Value("${app.jwt.issuer:milktea}") String issuer,
            @Value("${app.jwt.audience:milktea-web}") String audience,
            @Value("${app.jwt.ttl-minutes:60}") long ttlMinutes) {
        this.secretKey = Base64.getDecoder().decode(secretBase64);
        this.issuer = issuer;
        this.audience = audience;
        this.ttlMinutes = ttlMinutes;
        this.objectMapper = new ObjectMapper();
    }

    public String generateToken(Account account) {
        try {
            Instant now = Instant.now();
            Instant exp = now.plus(ttlMinutes, ChronoUnit.MINUTES);

            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", String.valueOf(account.getId()));
            claims.put("email", account.getEmail());
            claims.put("role", account.getRole().name());
            claims.put("tokenVersion", account.getTokenVersion());
            claims.put("iss", issuer);
            claims.put("aud", audience);
            claims.put("iat", now.getEpochSecond());
            claims.put("exp", exp.getEpochSecond());

            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsBytes(claims));
            String dataToSign = encodedHeader + "." + encodedPayload;

            byte[] signature = sign(dataToSign.getBytes(StandardCharsets.UTF_8));
            String encodedSignature = base64UrlEncode(signature);

            return dataToSign + "." + encodedSignature;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi sinh JWT token", e);
        }
    }

    public Map<String, Object> validateAndExtractClaims(String token) {
        if (token == null || token.isBlank()) return null;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;

        try {
            String dataToSign = parts[0] + "." + parts[1];
            byte[] expectedSig = sign(dataToSign.getBytes(StandardCharsets.UTF_8));
            byte[] actualSig = base64UrlDecode(parts[2]);

            if (!MessageDigest.isEqual(expectedSig, actualSig)) {
                return null;
            }

            byte[] payloadBytes = base64UrlDecode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, new TypeReference<>() {});

            long exp = ((Number) claims.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > exp) {
                return null;
            }

            if (!issuer.equals(claims.get("iss")) || !audience.equals(claims.get("aud"))) {
                return null;
            }

            return claims;
        } catch (Exception e) {
            log.debug("Lỗi xác thực JWT: {}", e.getMessage());
            return null;
        }
    }

    private byte[] sign(byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] base64UrlDecode(String str) {
        return Base64.getUrlDecoder().decode(str);
    }
}
