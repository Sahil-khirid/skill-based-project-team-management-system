package com.skillteam.auth.security;

import com.skillteam.auth.config.JwtProperties;
import com.skillteam.auth.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class JwtServiceTest {

    private static final String SECRET_BASE64 =
            "dGVzdC1vbmx5LWp3dC1zaWduaW5nLXNlY3JldC1mb3ItYXV0aC1zZXJ2aWNlLXVuaXQtdGVzdHMtMzJieXRlcw==";
    private static final String OTHER_SECRET_BASE64 =
            Base64.getEncoder().encodeToString("a-completely-different-32-byte-plus-secret-key!!".getBytes());
    private static final String ISSUER = "skillteam-auth-service";
    private static final String AUDIENCE = "skillteam-api";
    private static final Duration TTL = Duration.ofMinutes(15);
    private static final Instant FIXED_NOW = Instant.parse("2026-07-22T10:00:00Z");

    private final JwtProperties jwtProperties = new JwtProperties(SECRET_BASE64, ISSUER, AUDIENCE, TTL);
    private final Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final JwtService jwtService = new JwtService(jwtProperties, fixedClock);

    @Test
    void generatedTokenHasValidHs256SignatureAndExpectedClaims() {
        String token = jwtService.generateAccessToken(42L, "user@example.com", Role.USER);

        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_BASE64));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .clock(() -> java.util.Date.from(FIXED_NOW))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getAudience()).contains(AUDIENCE);
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    void tokenExpirationIsApproximatelyFifteenMinutesAfterIssuance() {
        String token = jwtService.generateAccessToken(1L, "ttl@example.com", Role.USER);

        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_BASE64));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .clock(() -> java.util.Date.from(FIXED_NOW))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Instant expiresAt = claims.getExpiration().toInstant();
        Instant issuedAt = claims.getIssuedAt().toInstant();

        assertThat(issuedAt).isEqualTo(FIXED_NOW);
        assertThat(expiresAt).isCloseTo(FIXED_NOW.plus(TTL), within(1, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void validTokenParsesSuccessfully() {
        String token = jwtService.generateAccessToken(7L, "parse@example.com", Role.PROJECT_MANAGER);

        JwtService.ParsedAccessToken parsed = jwtService.parseAndValidate(token);

        assertThat(parsed.userId()).isEqualTo(7L);
        assertThat(parsed.email()).isEqualTo("parse@example.com");
        assertThat(parsed.role()).isEqualTo("PROJECT_MANAGER");
    }

    @Test
    void expiredTokenIsRejected() {
        String token = jwtService.generateAccessToken(1L, "expired@example.com", Role.USER);

        Clock laterClock = Clock.fixed(FIXED_NOW.plus(TTL).plusSeconds(60), ZoneOffset.UTC);
        JwtService laterJwtService = new JwtService(jwtProperties, laterClock);

        assertThatThrownBy(() -> laterJwtService.parseAndValidate(token))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void invalidSignatureTokenIsRejected() {
        JwtProperties otherProperties = new JwtProperties(OTHER_SECRET_BASE64, ISSUER, AUDIENCE, TTL);
        JwtService otherJwtService = new JwtService(otherProperties, fixedClock);
        String token = otherJwtService.generateAccessToken(1L, "wrong-key@example.com", Role.USER);

        assertThatThrownBy(() -> jwtService.parseAndValidate(token))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void malformedTokenIsRejected() {
        assertThatThrownBy(() -> jwtService.parseAndValidate("not-a-jwt"))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void wrongIssuerTokenIsRejected() {
        JwtProperties otherIssuerProperties = new JwtProperties(SECRET_BASE64, "someone-else", AUDIENCE, TTL);
        JwtService otherJwtService = new JwtService(otherIssuerProperties, fixedClock);
        String token = otherJwtService.generateAccessToken(1L, "wrong-issuer@example.com", Role.USER);

        assertThatThrownBy(() -> jwtService.parseAndValidate(token))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void wrongAudienceTokenIsRejected() {
        JwtProperties otherAudienceProperties = new JwtProperties(SECRET_BASE64, ISSUER, "someone-else-api", TTL);
        JwtService otherJwtService = new JwtService(otherAudienceProperties, fixedClock);
        String token = otherJwtService.generateAccessToken(1L, "wrong-audience@example.com", Role.USER);

        assertThatThrownBy(() -> jwtService.parseAndValidate(token))
                .isInstanceOf(InvalidAccessTokenException.class);
    }
}
