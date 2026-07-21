package com.skillteam.gateway.security;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Shared JWT construction helper for gateway security tests. Not a production class — builds
 * tokens shaped like Auth-Service output, including deliberately broken variants for negative
 * validation tests.
 */
public final class TestTokens {

    public static final String SECRET_BASE64 =
            "dGVzdC1vbmx5LWp3dC1zaWduaW5nLXNlY3JldC1mb3ItYXV0aC1zZXJ2aWNlLXVuaXQtdGVzdHMtMzJieXRlcw==";
    public static final String OTHER_SECRET_BASE64 =
            Base64.getEncoder().encodeToString("a-completely-different-32-byte-plus-secret-key!!".getBytes());
    public static final String ISSUER = "skillteam-auth-service";
    public static final String AUDIENCE = "skillteam-api";

    private TestTokens() {
    }

    public static SecretKey keyFor(String secretBase64) {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretBase64));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String secretBase64 = SECRET_BASE64;
        private String issuer = ISSUER;
        private String audience = AUDIENCE;
        private String subject = "42";
        private String email = "user@example.com";
        private String role = "USER";
        private String jwtId = UUID.randomUUID().toString();
        private Instant issuedAt = Instant.now();
        private Instant expiration = Instant.now().plus(Duration.ofMinutes(15));
        private MacAlgorithm algorithm = Jwts.SIG.HS256;
        private boolean omitSubject;
        private boolean omitEmail;
        private boolean omitRole;
        private boolean omitJwtId;
        private boolean omitIssuedAt;
        private boolean omitExpiration;

        public Builder secretBase64(String value) {
            this.secretBase64 = value;
            return this;
        }

        public Builder issuer(String value) {
            this.issuer = value;
            return this;
        }

        public Builder audience(String value) {
            this.audience = value;
            return this;
        }

        public Builder subject(String value) {
            this.subject = value;
            return this;
        }

        public Builder email(String value) {
            this.email = value;
            return this;
        }

        public Builder role(String value) {
            this.role = value;
            return this;
        }

        public Builder jwtId(String value) {
            this.jwtId = value;
            return this;
        }

        public Builder issuedAt(Instant value) {
            this.issuedAt = value;
            return this;
        }

        public Builder expiration(Instant value) {
            this.expiration = value;
            return this;
        }

        public Builder algorithm(MacAlgorithm value) {
            this.algorithm = value;
            return this;
        }

        public Builder omitIssuedAt() {
            this.omitIssuedAt = true;
            return this;
        }

        public Builder omitExpiration() {
            this.omitExpiration = true;
            return this;
        }

        public Builder omitSubject() {
            this.omitSubject = true;
            return this;
        }

        public Builder omitEmail() {
            this.omitEmail = true;
            return this;
        }

        public Builder omitRole() {
            this.omitRole = true;
            return this;
        }

        public Builder omitJwtId() {
            this.omitJwtId = true;
            return this;
        }

        public String build() {
            JwtBuilder jwtBuilder = Jwts.builder()
                    .issuer(issuer)
                    .audience().add(audience).and();

            if (!omitIssuedAt) {
                jwtBuilder.issuedAt(Date.from(issuedAt));
            }
            if (!omitExpiration) {
                jwtBuilder.expiration(Date.from(expiration));
            }
            if (!omitSubject) {
                jwtBuilder.subject(subject);
            }
            if (!omitEmail) {
                jwtBuilder.claim("email", email);
            }
            if (!omitRole) {
                jwtBuilder.claim("role", role);
            }
            if (!omitJwtId) {
                jwtBuilder.id(jwtId);
            }

            return jwtBuilder.signWith(keyFor(secretBase64), algorithm).compact();
        }
    }
}
