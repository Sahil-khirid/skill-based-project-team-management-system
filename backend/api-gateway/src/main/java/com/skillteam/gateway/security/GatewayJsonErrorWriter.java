package com.skillteam.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillteam.gateway.dto.GatewayErrorResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Shared JSON error writer for the 401/403 handlers. Emits only the fixed, safe error shape —
 * never an exception class name, JWT parsing detail, or token/secret content.
 */
@Component
public class GatewayJsonErrorWriter {

    private final ObjectMapper objectMapper;

    public GatewayJsonErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        GatewayErrorResponse body = new GatewayErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value(),
                List.of());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception serializationFailure) {
            bytes = ("{\"status\":" + status.value() + ",\"error\":\"" + status.getReasonPhrase() + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
