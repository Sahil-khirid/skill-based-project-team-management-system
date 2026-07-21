package com.skillteam.gateway.config;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayJwtPropertiesTest {

    private static final String VALID_SECRET_BASE64 =
            Base64.getEncoder().encodeToString("a-32-byte-or-longer-test-secret!".getBytes());

    @Test
    void blankSecretFailsWithoutRevealingSecretContent() {
        assertThatThrownBy(() -> new GatewayJwtProperties("", "issuer", "audience"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("a-32-byte-or-longer-test-secret!");
    }

    @Test
    void secretShorterThanThirtyTwoBytesFails() {
        String shortSecret = Base64.getEncoder().encodeToString("too-short".getBytes());

        assertThatThrownBy(() -> new GatewayJwtProperties(shortSecret, "issuer", "audience"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("too-short")
                .hasMessageNotContaining(shortSecret);
    }

    @Test
    void nonBase64SecretFails() {
        assertThatThrownBy(() -> new GatewayJwtProperties("not base64!!", "issuer", "audience"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void blankIssuerFails() {
        assertThatThrownBy(() -> new GatewayJwtProperties(VALID_SECRET_BASE64, " ", "audience"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void blankAudienceFails() {
        assertThatThrownBy(() -> new GatewayJwtProperties(VALID_SECRET_BASE64, "issuer", ""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void toStringNeverContainsSecretBytes() {
        GatewayJwtProperties properties = new GatewayJwtProperties(VALID_SECRET_BASE64, "issuer", "audience");

        assertThat(properties.toString())
                .doesNotContain(VALID_SECRET_BASE64)
                .contains("REDACTED");
    }
}
