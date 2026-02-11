package com.cloudmedia.service;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.cloudmedia.config.JwtProperties;

@Service
public class SignedUrlService {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String KIND_VIDEO = "VIDEO";
    private static final String KIND_DOC = "DOC";
    private static final long DEFAULT_EXPIRE_SECONDS = 3600L;

    private final JwtProperties jwtProperties;

    public SignedUrlService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateVideoAccessToken(Long userId, Long mediaId) {
        return generate(userId, mediaId, KIND_VIDEO, Instant.now().plusSeconds(DEFAULT_EXPIRE_SECONDS).getEpochSecond());
    }

    public String generateDocAccessToken(Long userId, Long mediaId) {
        return generate(userId, mediaId, KIND_DOC, Instant.now().plusSeconds(DEFAULT_EXPIRE_SECONDS).getEpochSecond());
    }

    public Long verifyAccessToken(String token, HttpServletRequest request) {
        if (!StringUtils.hasText(token) || !token.contains(".")) {
            return null;
        }
        String[] parts = token.split("\\.", 2);
        String payloadEncoded = parts[0];
        String signature = parts[1];
        String expected = hmacHex(payloadEncoded);
        if (!constantTimeEquals(expected, signature)) {
            return null;
        }
        String payload = new String(Base64.getUrlDecoder().decode(payloadEncoded), StandardCharsets.UTF_8);
        String[] fields = payload.split(":", 4);
        if (fields.length != 4) {
            return null;
        }
        Long userId = parseLong(fields[0]);
        Long mediaId = parseLong(fields[1]);
        String kind = fields[2];
        Long expiresAt = parseLong(fields[3]);
        if (userId == null || mediaId == null || expiresAt == null || expiresAt < Instant.now().getEpochSecond()) {
            return null;
        }

        String path = request.getServletPath();
        Long pathMediaId = extractPathMediaId(path);
        if (pathMediaId == null || !mediaId.equals(pathMediaId)) {
            return null;
        }
        if (path.startsWith("/api/media/stream/video/") && !KIND_VIDEO.equals(kind)) {
            return null;
        }
        if (path.startsWith("/api/media/view/doc/") && !KIND_DOC.equals(kind)) {
            return null;
        }
        return userId;
    }

    private String generate(Long userId, Long mediaId, String kind, long expiresAt) {
        String payload = userId + ":" + mediaId + ":" + kind + ":" + expiresAt;
        String payloadEncoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return payloadEncoded + "." + hmacHex(payloadEncoded);
    }

    private String hmacHex(String payloadEncoded) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] digest = mac.doFinal(payloadEncoded.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("sign token failed", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private static Long parseLong(String v) {
        try {
            return Long.parseLong(v);
        } catch (Exception e) {
            return null;
        }
    }

    private static Long extractPathMediaId(String path) {
        int idx = path.lastIndexOf('/');
        if (idx < 0 || idx == path.length() - 1) {
            return null;
        }
        return parseLong(path.substring(idx + 1));
    }
}
