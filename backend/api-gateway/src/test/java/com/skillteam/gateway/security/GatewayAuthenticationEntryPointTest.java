package com.skillteam.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAuthenticationEntryPointTest {

    private final GatewayJsonErrorWriter errorWriter = new GatewayJsonErrorWriter(new ObjectMapper().findAndRegisterModules());
    private final GatewayAuthenticationEntryPoint entryPoint = new GatewayAuthenticationEntryPoint(errorWriter);

    @Test
    void commenceWritesJson401WithoutExposingExceptionDetails() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/me"));

        entryPoint.commence(exchange, new BadCredentialsException("Invalid access token: eyJhbGciOi.secret.stuff"))
                .block();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

        String body = exchange.getResponse().getBodyAsString().block();

        assertThat(body).contains("\"status\":401");
        assertThat(body).contains("\"message\":\"Authentication is required.\"");
        assertThat(body).contains("\"path\":\"/api/v1/auth/me\"");
        assertThat(body).doesNotContain("BadCredentialsException");
        assertThat(body).doesNotContain("eyJhbGciOi");
        assertThat(body).doesNotContain("secret");
    }
}
