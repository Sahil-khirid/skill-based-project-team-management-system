package com.skillteam.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.Base64;

@ConfigurationProperties(prefix = "gateway.security.jwt")
public class GatewayJwtProperties {

    private static final int MIN_SECRET_BYTES = 32;

    private final byte[] secretKeyBytes;
    private final String issuer;
    private final String audience;

    @ConstructorBinding
    public GatewayJwtProperties(String secretBase64, String issuer, String audience) {
        this.secretKeyBytes = decodeAndValidateSecret(secretBase64);
        this.issuer = requireNonBlank(issuer, "gateway.security.jwt.issuer must not be blank.");
        this.audience = requireNonBlank(audience, "gateway.security.jwt.audience must not be blank.");
    }

    private static byte[] decodeAndValidateSecret(String secretBase64) {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException(
                    "gateway.security.jwt.secret-base64 is required. Set it via the JWT_SECRET_BASE64 "
                            + "environment variable.");
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secretBase64.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("gateway.security.jwt.secret-base64 must be valid Base64.");
        }

        if (decoded.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "gateway.security.jwt.secret-base64 must decode to at least " + MIN_SECRET_BYTES + " bytes.");
        }

        return decoded;
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    // Defensive copy of the decoded signing secret; callers must never log or surface these bytes.
    public byte[] secretKeyBytes() {
        return secretKeyBytes.clone();
    }

    public String getIssuer() {
        return issuer;
    }

    public String getAudience() {
        return audience;
    }

    @Override
    public String toString() {
        return "GatewayJwtProperties{issuer='" + issuer + "', audience='" + audience + "', secretKeyBytes=[REDACTED]}";
    }
}
