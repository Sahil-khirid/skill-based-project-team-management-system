package com.skillteam.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Runs on every request before it is proxied downstream. Client-supplied identity headers are
 * never trusted: they are always stripped first, and for authenticated requests replaced with
 * values derived solely from the validated {@link GatewayUserPrincipal}. Unauthenticated (public)
 * requests are forwarded with the headers removed and nothing added back.
 */
@Component
public class TrustedIdentityHeaderFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-Auth-User-Id";
    public static final String USER_EMAIL_HEADER = "X-Auth-User-Email";
    public static final String USER_ROLE_HEADER = "X-Auth-User-Role";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> extractPrincipal(context.getAuthentication()))
                .defaultIfEmpty(Optional.empty())
                .flatMap(principal -> forward(exchange, chain, principal));
    }

    private Optional<GatewayUserPrincipal> extractPrincipal(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof GatewayUserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    private Mono<Void> forward(ServerWebExchange exchange, GatewayFilterChain chain,
                                Optional<GatewayUserPrincipal> principal) {
        ServerHttpRequest.Builder mutatedRequest = exchange.getRequest().mutate();
        mutatedRequest.headers(headers -> {
            headers.remove(USER_ID_HEADER);
            headers.remove(USER_EMAIL_HEADER);
            headers.remove(USER_ROLE_HEADER);
        });

        principal.ifPresent(user -> mutatedRequest.headers(headers -> {
            headers.set(USER_ID_HEADER, String.valueOf(user.getUserId()));
            headers.set(USER_EMAIL_HEADER, user.getEmail());
            headers.set(USER_ROLE_HEADER, user.getRole());
        }));

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest.build()).build();
        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
