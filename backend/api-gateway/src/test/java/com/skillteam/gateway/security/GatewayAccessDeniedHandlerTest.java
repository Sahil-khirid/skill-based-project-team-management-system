package com.skillteam.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAccessDeniedHandlerTest {

    private final GatewayJsonErrorWriter errorWriter = new GatewayJsonErrorWriter(new ObjectMapper().findAndRegisterModules());
    private final GatewayAccessDeniedHandler accessDeniedHandler = new GatewayAccessDeniedHandler(errorWriter);

    @Test
    void handleWritesJson403WithoutExposingExceptionDetails() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/me"));

        accessDeniedHandler.handle(exchange, new AccessDeniedException("insufficient authority"))
                .block();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(403);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

        String body = exchange.getResponse().getBodyAsString().block();

        assertThat(body).contains("\"status\":403");
        assertThat(body).contains("\"message\":\"Access is denied.\"");
        assertThat(body).contains("\"path\":\"/api/v1/auth/me\"");
        assertThat(body).doesNotContain("AccessDeniedException");
        assertThat(body).doesNotContain("insufficient authority");
    }
}
