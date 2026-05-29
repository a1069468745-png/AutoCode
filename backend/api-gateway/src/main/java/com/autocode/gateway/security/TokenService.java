package com.autocode.gateway.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
public class TokenService {

    private final AuthProperties properties;

    public TokenService(AuthProperties properties) {
        this.properties = properties;
    }

    public String generateToken(String userId, String username, String roles) {
        long now = Instant.now().getEpochSecond();
        long expiry = now + properties.jwtExpirationSeconds();
        String payload = userId + ":" + username + ":" + roles + ":" + now + ":" + expiry;
        String signature = hmacSha256(payload, properties.jwtSecret());
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + signature;
        return token;
    }

    public TokenPayload validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            int dotIndex = token.lastIndexOf('.');
            if (dotIndex < 0) {
                return null;
            }
            String payloadB64 = token.substring(0, dotIndex);
            String signature = token.substring(dotIndex + 1);
            String payload = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);

            String expectedSig = hmacSha256(payload, properties.jwtSecret());
            if (!signature.equals(expectedSig)) {
                return null;
            }

            String[] parts = payload.split(":");
            if (parts.length != 5) {
                return null;
            }
            long expiry = Long.parseLong(parts[4]);
            if (Instant.now().getEpochSecond() > expiry) {
                return null;
            }

            return new TokenPayload(parts[0], parts[1], parts[2]);
        } catch (Exception e) {
            return null;
        }
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 failed", e);
        }
    }

    public record TokenPayload(String userId, String username, String roles) {
    }
}
