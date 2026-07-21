package com.skillteam.gateway.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayAccessDeniedHandler implements ServerAccessDeniedHandler {

    private static final String MESSAGE = "Access is denied.";

    private final GatewayJsonErrorWriter errorWriter;

    public GatewayAccessDeniedHandler(GatewayJsonErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        return errorWriter.write(exchange, HttpStatus.FORBIDDEN, MESSAGE);
    }
}
