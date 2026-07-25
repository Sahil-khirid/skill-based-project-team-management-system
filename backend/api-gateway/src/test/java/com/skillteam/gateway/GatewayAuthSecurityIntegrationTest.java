package com.skillteam.gateway;

import com.skillteam.gateway.security.TestTokens;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Full-stack Gateway security test. Routes to a lightweight local mock standing in for
 * AUTH-SERVICE (via a route URI override), so no running Eureka Server or Auth Service is
 * required.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayAuthSecurityIntegrationTest {

    private static HttpServer mockAuthServer;
    private static final Map<String, String> lastRequestHeaders = new ConcurrentHashMap<>();
    private static volatile String lastRequestPath;

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void registerAuthServiceRoute(DynamicPropertyRegistry registry) throws IOException {
        mockAuthServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        mockAuthServer.createContext("/api/v1/auth/register", exchange -> {
            recordRequest(exchange);
            respond(exchange, 201, "{\"id\":1,\"email\":\"user@example.com\",\"role\":\"USER\",\"enabled\":true}");
        });
        mockAuthServer.createContext("/api/v1/auth/login", exchange -> {
            recordRequest(exchange);
            respond(exchange, 200, "{\"accessToken\":\"stub\",\"tokenType\":\"Bearer\",\"expiresIn\":900}");
        });
        mockAuthServer.createContext("/api/v1/auth/me", exchange -> {
            recordRequest(exchange);
            respond(exchange, 200, "{\"id\":1,\"email\":\"user@example.com\",\"role\":\"USER\"}");
        });
        mockAuthServer.createContext("/api/v1/auth/refresh", exchange -> {
            recordRequest(exchange);
            respond(exchange, 200, "{\"accessToken\":\"stub\",\"tokenType\":\"Bearer\",\"expiresIn\":900,\"refreshToken\":\"stub-refresh\"}");
        });
        mockAuthServer.createContext("/api/v1/auth/logout", exchange -> {
            recordRequest(exchange);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        mockAuthServer.setExecutor(null);
        mockAuthServer.start();

        int port = mockAuthServer.getAddress().getPort();
        registry.add("spring.cloud.gateway.server.webflux.routes[0].id", () -> "auth-service");
        registry.add("spring.cloud.gateway.server.webflux.routes[0].uri", () -> "http://localhost:" + port);
        registry.add("spring.cloud.gateway.server.webflux.routes[0].predicates[0]", () -> "Path=/api/v1/auth/**");
    }

    @AfterAll
    static void stopMockAuthServer() {
        if (mockAuthServer != null) {
            mockAuthServer.stop(0);
        }
    }

    private static void recordRequest(HttpExchange exchange) throws IOException {
        exchange.getRequestBody().readAllBytes();
        lastRequestPath = exchange.getRequestURI().getPath();
        lastRequestHeaders.clear();
        exchange.getRequestHeaders().forEach((name, values) -> {
            if (!values.isEmpty()) {
                lastRequestHeaders.put(name.toLowerCase(java.util.Locale.ROOT), values.get(0));
            }
        });
    }

    private static String header(String name) {
        return lastRequestHeaders.get(name.toLowerCase(java.util.Locale.ROOT));
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String validToken() {
        return TestTokens.builder().subject("1").email("user@example.com").role("USER").build();
    }

    @Test
    void healthEndpointIsPubliclyAccessible() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void registerIsAccessibleWithoutJwt() {
        webTestClient.post().uri("/api/v1/auth/register")
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void loginIsAccessibleWithoutJwt() {
        webTestClient.post().uri("/api/v1/auth/login")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void refreshIsAccessibleWithoutJwt() {
        webTestClient.post().uri("/api/v1/auth/refresh")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("{\"refreshToken\":\"some-refresh-token\"}")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void logoutIsAccessibleWithoutJwt() {
        webTestClient.post().uri("/api/v1/auth/logout")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("{\"refreshToken\":\"some-refresh-token\"}")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void meWithoutJwtReturnsJson401() {
        webTestClient.get().uri("/api/v1/auth/me")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("Authentication is required.")
                .jsonPath("$.path").isEqualTo("/api/v1/auth/me");
    }

    @Test
    void validJwtGrantsAccessToProtectedRouteAndIsRoutedWithPathPreserved() {
        webTestClient.get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + validToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(lastRequestPath).isEqualTo("/api/v1/auth/me");
    }

    @Test
    void authorizationHeaderIsPreservedDownstream() {
        String token = validToken();

        webTestClient.get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        assertThat(header("Authorization")).isEqualTo("Bearer " + token);
    }

    @Test
    void validJwtProducesUserIdHeader() {
        String token = TestTokens.builder().subject("55").email("user@example.com").role("USER").build();

        webTestClient.get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        assertThat(header("X-Auth-User-Id")).isEqualTo("55");
    }

    @Test
    void validNormalizedEmailProducesMatchingTrustedHeader() {
        String token = TestTokens.builder().subject("1").email("user@example.com").role("USER").build();

        webTestClient.get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        assertThat(header("X-Auth-User-Email")).isEqualTo("user@example.com");
    }

    @Test
    void validJwtProducesRoleHeader() {
        String token = TestTokens.builder().subject("1").email("pm@example.com").role("PROJECT_MANAGER").build();

        webTestClient.get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        assertThat(header("X-Auth-User-Role")).isEqualTo("PROJECT_MANAGER");
    }

    @Test
    void clientSuppliedIdentityHeadersAreRemovedAndReplacedWithTrustedValues() {
        String token = TestTokens.builder().subject("1").email("real@example.com").role("USER").build();

        webTestClient.get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token)
                .header("X-Auth-User-Id", "999")
                .header("X-Auth-User-Email", "attacker@example.com")
                .header("X-Auth-User-Role", "PROJECT_MANAGER")
                .exchange()
                .expectStatus().isOk();

        assertThat(header("X-Auth-User-Id")).isEqualTo("1");
        assertThat(header("X-Auth-User-Email")).isEqualTo("real@example.com");
        assertThat(header("X-Auth-User-Role")).isEqualTo("USER");
    }

    @Test
    void publicRequestSpoofedHeadersAreRemoved() {
        webTestClient.post().uri("/api/v1/auth/register")
                .header("X-Auth-User-Id", "999")
                .header("X-Auth-User-Email", "attacker@example.com")
                .header("X-Auth-User-Role", "PROJECT_MANAGER")
                .exchange()
                .expectStatus().isCreated();

        assertThat(header("X-Auth-User-Id")).isNull();
        assertThat(header("X-Auth-User-Email")).isNull();
        assertThat(header("X-Auth-User-Role")).isNull();
    }

    @Test
    void invalidSignatureTokenReturns401() {
        String token = TestTokens.builder().secretBase64(TestTokens.OTHER_SECRET_BASE64).build();

        assertUnauthorized(token);
    }

    @Test
    void malformedTokenReturns401() {
        assertUnauthorized("not-a-jwt-at-all");
    }

    @Test
    void expiredTokenReturns401() {
        Instant past = Instant.now().minus(Duration.ofMinutes(30));
        String token = TestTokens.builder()
                .issuedAt(past)
                .expiration(past.plus(Duration.ofMinutes(15)))
                .build();

        assertUnauthorized(token);
    }

    @Test
    void wrongIssuerTokenReturns401() {
        String token = TestTokens.builder().issuer("someone-else").build();

        assertUnauthorized(token);
    }

    @Test
    void wrongAudienceTokenReturns401() {
        String token = TestTokens.builder().audience("someone-else-api").build();

        assertUnauthorized(token);
    }

    @Test
    void missingSubjectTokenReturns401() {
        String token = TestTokens.builder().omitSubject().build();

        assertUnauthorized(token);
    }

    @Test
    void nonNumericSubjectTokenReturns401() {
        String token = TestTokens.builder().subject("not-a-number").build();

        assertUnauthorized(token);
    }

    @Test
    void missingEmailTokenReturns401() {
        String token = TestTokens.builder().omitEmail().build();

        assertUnauthorized(token);
    }

    @Test
    void invalidRoleTokenReturns401() {
        String token = TestTokens.builder().role("ADMIN").build();

        assertUnauthorized(token);
    }

    @Test
    void missingJwtIdTokenReturns401() {
        String token = TestTokens.builder().omitJwtId().build();

        assertUnauthorized(token);
    }

    @Test
    void missingIssuedAtTokenReturns401() {
        String token = TestTokens.builder().omitIssuedAt().build();

        assertUnauthorized(token);
    }

    @Test
    void missingExpirationTokenReturns401() {
        String token = TestTokens.builder().omitExpiration().build();

        assertUnauthorized(token);
    }

    @Test
    void nonHs256TokenReturns401() {
        String token = TestTokens.builder().algorithm(Jwts.SIG.HS512).build();

        assertUnauthorized(token);
    }

    @Test
    void uppercaseEmailTokenReturns401() {
        String token = TestTokens.builder().email("User@Example.com").build();

        assertUnauthorized(token);
    }

    @Test
    void whitespacePaddedEmailTokenReturns401() {
        String token = TestTokens.builder().email(" user@example.com ").build();

        assertUnauthorized(token);
    }

    @Test
    void unauthorizedResponseNeverExposesTokenOrExceptionDetails() {
        String token = "not-a-jwt-at-all";

        byte[] body = webTestClient.get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().returnResult().getResponseBody();

        String responseBody = new String(body, StandardCharsets.UTF_8);
        assertThat(responseBody).doesNotContain(token);
        assertThat(responseBody).doesNotContain("JwtException");
        assertThat(responseBody).doesNotContain("io.jsonwebtoken");
        assertThat(responseBody).doesNotContain("InvalidGatewayTokenException");
    }

    @Test
    void noSessionOrAuthenticationCookieIsCreated() {
        webTestClient.get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + validToken())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist("Set-Cookie");
    }

    private void assertUnauthorized(String token) {
        webTestClient.get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("Authentication is required.");
    }
}
