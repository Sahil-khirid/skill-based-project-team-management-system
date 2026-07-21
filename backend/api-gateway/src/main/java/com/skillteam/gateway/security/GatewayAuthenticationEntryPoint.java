package com.skillteam.gateway.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private static final String MESSAGE = "Authentication is required.";

    private final GatewayJsonErrorWriter errorWriter;

    public GatewayAuthenticationEntryPoint(GatewayJsonErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        return errorWriter.write(exchange, HttpStatus.UNAUTHORIZED, MESSAGE);
    }
}
