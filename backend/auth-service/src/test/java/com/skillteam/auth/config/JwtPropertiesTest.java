package com.skillteam.auth.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    private static final String VALID_SECRET_BASE64 =
            Base64.getEncoder().encodeToString("a-32-byte-or-longer-test-secret!".getBytes());

    @Test
    void blankSecretFailsWithoutRevealingSecretContent() {
        assertThatThrownBy(() -> new JwtProperties("", "issuer", "audience", Duration.ofMinutes(15)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("a-32-byte-or-longer-test-secret!");
    }

    @Test
    void secretShorterThanThirtyTwoBytesFails() {
        String shortSecret = Base64.getEncoder().encodeToString("too-short".getBytes());

        assertThatThrownBy(() -> new JwtProperties(shortSecret, "issuer", "audience", Duration.ofMinutes(15)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("too-short")
                .hasMessageNotContaining(shortSecret);
    }

    @Test
    void nonBase64SecretFails() {
        assertThatThrownBy(() -> new JwtProperties("not base64!!", "issuer", "audience", Duration.ofMinutes(15)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void blankIssuerFails() {
        assertThatThrownBy(() -> new JwtProperties(VALID_SECRET_BASE64, " ", "audience", Duration.ofMinutes(15)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void blankAudienceFails() {
        assertThatThrownBy(() -> new JwtProperties(VALID_SECRET_BASE64, "issuer", "", Duration.ofMinutes(15)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nonPositiveTtlFails() {
        assertThatThrownBy(() -> new JwtProperties(VALID_SECRET_BASE64, "issuer", "audience", Duration.ZERO))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtProperties(VALID_SECRET_BASE64, "issuer", "audience", Duration.ofMinutes(-1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void toStringNeverContainsSecretBytes() {
        JwtProperties properties = new JwtProperties(VALID_SECRET_BASE64, "issuer", "audience", Duration.ofMinutes(15));

        assertThat(properties.toString())
                .doesNotContain(VALID_SECRET_BASE64)
                .contains("REDACTED");
    }
}
