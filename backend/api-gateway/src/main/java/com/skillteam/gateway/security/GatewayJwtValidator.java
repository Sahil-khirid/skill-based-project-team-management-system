package com.skillteam.gateway.security;

import com.skillteam.gateway.config.GatewayJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validates Auth-Service-issued JWTs at the Gateway: signature, expiration, issuer, audience,
 * and the shape of the required identity claims. This component never touches the Auth
 * database — it only validates what the token itself asserts.
 */
@Component
public class GatewayJwtValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final String ROLE_USER = "USER";
    private static final String ROLE_PROJECT_MANAGER = "PROJECT_MANAGER";
    private static final String REQUIRED_ALGORITHM = "HS256";

    private final GatewayJwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public GatewayJwtValidator(GatewayJwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(properties.secretKeyBytes());
    }

    public GatewayUserPrincipal validate(String token) {
        Jws<Claims> jws;
        try {
            jws = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.getIssuer())
                    .requireAudience(properties.getAudience())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidGatewayTokenException("Access token failed validation.");
        }

        requireExactAlgorithm(jws.getHeader());

        Claims claims = jws.getPayload();
        requireTemporalClaims(claims);

        Long userId = parseUserId(claims.getSubject());
        String email = parseEmail(claims.get("email", String.class));
        String role = parseRole(claims.get("role", String.class));
        requireNonBlank(claims.getId(), "Access token id claim is missing.");

        return new GatewayUserPrincipal(userId, email, role);
    }

    private void requireExactAlgorithm(JwsHeader header) {
        if (!REQUIRED_ALGORITHM.equals(header.getAlgorithm())) {
            throw new InvalidGatewayTokenException("Access token uses an unsupported signing algorithm.");
        }
    }

    private void requireTemporalClaims(Claims claims) {
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        if (issuedAt == null) {
            throw new InvalidGatewayTokenException("Access token issuedAt claim is missing.");
        }
        if (expiration == null) {
            throw new InvalidGatewayTokenException("Access token expiration claim is missing.");
        }
        if (!expiration.after(issuedAt)) {
            throw new InvalidGatewayTokenException("Access token expiration must be after issuedAt.");
        }
    }

    private Long parseUserId(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new InvalidGatewayTokenException("Access token subject claim is missing.");
        }
        long value;
        try {
            value = Long.parseLong(subject.trim());
        } catch (NumberFormatException ex) {
            throw new InvalidGatewayTokenException("Access token subject claim is not numeric.");
        }
        if (value <= 0) {
            throw new InvalidGatewayTokenException("Access token subject claim must be a positive number.");
        }
        return value;
    }

    private String parseEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidGatewayTokenException("Access token email claim is missing.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!email.equals(normalized)) {
            throw new InvalidGatewayTokenException("Access token email claim is not normalized.");
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new InvalidGatewayTokenException("Access token email claim is invalid.");
        }
        return normalized;
    }

    private String parseRole(String role) {
        if (!ROLE_USER.equals(role) && !ROLE_PROJECT_MANAGER.equals(role)) {
            throw new InvalidGatewayTokenException("Access token role claim is invalid.");
        }
        return role;
    }

    private void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidGatewayTokenException(message);
        }
    }
}
