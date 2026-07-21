package com.skillteam.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TrustedIdentityHeaderFilterTest {

    private final TrustedIdentityHeaderFilter filter = new TrustedIdentityHeaderFilter();

    @Test
    void unauthenticatedRequestHasSpoofedHeadersRemovedAndNoneAdded() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/register")
                        .header("X-Auth-User-Id", "999")
                        .header("X-Auth-User-Email", "attacker@example.com")
                        .header("X-Auth-User-Role", "PROJECT_MANAGER"));

        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            captured.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        ServerWebExchange forwarded = captured.get();
        assertThat(forwarded.getRequest().getHeaders().get("X-Auth-User-Id")).isNull();
        assertThat(forwarded.getRequest().getHeaders().get("X-Auth-User-Email")).isNull();
        assertThat(forwarded.getRequest().getHeaders().get("X-Auth-User-Role")).isNull();
    }

    @Test
    void authenticatedRequestReplacesSpoofedHeadersWithTrustedValues() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/me")
                        .header("X-Auth-User-Id", "999")
                        .header("X-Auth-User-Email", "attacker@example.com")
                        .header("X-Auth-User-Role", "PROJECT_MANAGER"));

        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            captured.set(ex);
            return Mono.empty();
        };

        GatewayUserPrincipal principal = new GatewayUserPrincipal(7L, "trusted@example.com", "USER");
        GatewayBearerAuthenticationToken authentication =
                new GatewayBearerAuthenticationToken(principal, principal.authorities());

        filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(new SecurityContextImpl(authentication))))
                .block();

        ServerWebExchange forwarded = captured.get();
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-Auth-User-Id")).isEqualTo("7");
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-Auth-User-Email")).isEqualTo("trusted@example.com");
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-Auth-User-Role")).isEqualTo("USER");
    }
}
