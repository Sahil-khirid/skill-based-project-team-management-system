package com.skillteam.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;

class BearerTokenServerAuthenticationConverterTest {

    private final BearerTokenServerAuthenticationConverter converter = new BearerTokenServerAuthenticationConverter();

    @Test
    void lowercaseBearerSchemeIsAccepted() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/me").header("Authorization", "bearer a-token-value"));

        Authentication authentication = converter.convert(exchange).block();

        assertThat(authentication).isInstanceOf(GatewayBearerAuthenticationToken.class);
        assertThat(authentication.getCredentials()).isEqualTo("a-token-value");
    }

    @Test
    void mixedCaseBearerSchemeIsAccepted() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/me").header("Authorization", "BeArEr a-token-value"));

        Authentication authentication = converter.convert(exchange).block();

        assertThat(authentication).isInstanceOf(GatewayBearerAuthenticationToken.class);
        assertThat(authentication.getCredentials()).isEqualTo("a-token-value");
    }

    @Test
    void missingHeaderIsUnauthenticated() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/me"));

        assertThat(converter.convert(exchange).block()).isNull();
    }

    @Test
    void emptyCredentialAfterSchemeIsUnauthenticated() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/me").header("Authorization", "Bearer "));

        assertThat(converter.convert(exchange).block()).isNull();
    }

    @Test
    void malformedSchemeIsUnauthenticated() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/me").header("Authorization", "Basic dXNlcjpwYXNz"));

        assertThat(converter.convert(exchange).block()).isNull();
    }
}
