package com.skillteam.gateway.security;

import com.skillteam.gateway.config.GatewayJwtProperties;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayJwtValidatorTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-22T10:00:00Z");

    private final GatewayJwtProperties properties =
            new GatewayJwtProperties(TestTokens.SECRET_BASE64, TestTokens.ISSUER, TestTokens.AUDIENCE);
    private final Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final GatewayJwtValidator validator = new GatewayJwtValidator(properties, fixedClock);

    private TestTokens.Builder validToken() {
        return TestTokens.builder()
                .issuedAt(FIXED_NOW)
                .expiration(FIXED_NOW.plus(Duration.ofMinutes(15)));
    }

    @Test
    void validTokenProducesExpectedPrincipal() {
        String token = validToken().subject("42").email("user@example.com").role("USER").build();

        GatewayUserPrincipal principal = validator.validate(token);

        assertThat(principal.getUserId()).isEqualTo(42L);
        assertThat(principal.getEmail()).isEqualTo("user@example.com");
        assertThat(principal.getRole()).isEqualTo("USER");
    }

    @Test
    void expiredTokenIsRejected() {
        String token = validToken()
                .issuedAt(FIXED_NOW.minus(Duration.ofMinutes(30)))
                .expiration(FIXED_NOW.minus(Duration.ofMinutes(15)))
                .build();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(InvalidGatewayTokenException.class);
    }

    @Test
    void invalidSignatureTokenIsRejected() {
        String token = validToken().secretBase64(TestTokens.OTHER_SECRET_BASE64).build();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(InvalidGatewayTokenException.class);
    }

    @Test
    void malformedTokenIsRejected() {
        assertThatThrownBy(() -> validator.validate("not-a-jwt"))
                .isInstanceOf(InvalidGatewayTokenException.class);
    }

    @Test
    void wrongIssuerTokenIsRejected() {
        String token = validToken().issuer("someone-else").build();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(InvalidGatewayTokenException.class);
    }

    @Test
    void wrongAudienceTokenIsRejected() {
        String token = validToken().audience("someone-else-api").build();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(InvalidGatewayTokenException.class);
    }

    @Test
    void missingSubjectIsRejected() {
        String token = validToken().omitSubject().build();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(InvalidGatewayTokenException.class);
    }

    @Test
    void nonNumericSubjectIsRejected() {
        String token = validToken().subject("not-a-number").build();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(InvalidGatewayTokenException.class);
    }

    @Test
    void missingEmailIsRejected() {
        String token = validToken().omitEmail().build();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(InvalidGatewayTokenException.class);
    }

    @Test
    void invalidRoleIsRejected() {
        String token = validToken().role("ADMIN").build();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(InvalidGatewayTokenException.class);
    }

    @Test
    void missingJwtIdIsRejected() {
        String token = validToken().omitJwtId().build();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(InvalidGatewayTokenException.class);
    }

    @Test
    void missingIssuedAtIsRejected() {
        String token = validToken().omitIssuedAt().build();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(InvalidGatewayTokenException.class);
    }

    @Test
    void missingExpirationIsRejected() {
        String token = validToken().omitExpiration().build();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(InvalidGatewayTokenException.class);
    }

    @Test
    void expirationNotAfterIssuedAtIsRejected() {
        String token = validToken()
                .issuedAt(FIXED_NOW)
                .expiration(FIXED_NOW)
                .build();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(InvalidGatewayTokenException.class);
    }

    @Test
    void nonHs256TokenIsRejectedEvenWhenValidlySigned() {
        String token = validToken().algorithm(Jwts.SIG.HS512).build();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(InvalidGatewayTokenException.class);
    }

    @Test
    void uppercaseEmailClaimIsRejected() {
        String token = validToken().email("User@Example.com").build();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(InvalidGatewayTokenException.class);
    }

    @Test
    void whitespacePaddedEmailClaimIsRejected() {
        String token = validToken().email(" user@example.com ").build();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(InvalidGatewayTokenException.class);
    }
}
