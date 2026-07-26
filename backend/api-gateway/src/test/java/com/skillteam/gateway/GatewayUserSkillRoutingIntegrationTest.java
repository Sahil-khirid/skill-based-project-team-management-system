package com.skillteam.gateway;

import com.skillteam.gateway.security.TestTokens;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Full-stack Gateway routing test for the User & Skill Service routes. Routes to a lightweight
 * local mock standing in for USER-SKILL-SERVICE (via a route URI override), so no running
 * Eureka Server or User & Skill Service is required.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayUserSkillRoutingIntegrationTest {

    private static HttpServer mockUserSkillServer;
    private static final Map<String, String> lastRequestHeaders = new ConcurrentHashMap<>();
    private static volatile String lastRequestPath;
    private static final AtomicInteger skillsBackendRequestCount = new AtomicInteger();

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void registerUserSkillServiceRoute(DynamicPropertyRegistry registry) throws IOException {
        mockUserSkillServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        mockUserSkillServer.createContext("/api/v1/users/me", exchange -> {
            recordRequest(exchange);
            respond(exchange, 200, "{\"id\":1,\"displayName\":\"Test User\"}");
        });
        mockUserSkillServer.createContext("/api/v1/skills", exchange -> {
            recordRequest(exchange);
            skillsBackendRequestCount.incrementAndGet();
            respond(exchange, 200, "[]");
        });
        mockUserSkillServer.setExecutor(null);
        mockUserSkillServer.start();

        int port = mockUserSkillServer.getAddress().getPort();
        String uri = "http://localhost:" + port;

        // Spring Boot binds a list-typed @ConfigurationProperties entirely from a single
        // property source; it does not merge individual indices across sources. So the whole
        // three-route list is redeclared here, mirroring application.yml's auth-service entry
        // unchanged and pointing the two new routes at the local mock.
        registry.add("spring.cloud.gateway.server.webflux.routes[0].id", () -> "auth-service");
        registry.add("spring.cloud.gateway.server.webflux.routes[0].uri", () -> "lb://AUTH-SERVICE");
        registry.add("spring.cloud.gateway.server.webflux.routes[0].predicates[0]", () -> "Path=/api/v1/auth/**");

        registry.add("spring.cloud.gateway.server.webflux.routes[1].id", () -> "user-skill-users");
        registry.add("spring.cloud.gateway.server.webflux.routes[1].uri", () -> uri);
        registry.add("spring.cloud.gateway.server.webflux.routes[1].predicates[0]", () -> "Path=/api/v1/users/**");

        registry.add("spring.cloud.gateway.server.webflux.routes[2].id", () -> "user-skill-skills");
        registry.add("spring.cloud.gateway.server.webflux.routes[2].uri", () -> uri);
        registry.add("spring.cloud.gateway.server.webflux.routes[2].predicates[0]", () -> "Path=/api/v1/skills/**");
    }

    @AfterAll
    static void stopMockUserSkillServer() {
        if (mockUserSkillServer != null) {
            mockUserSkillServer.stop(0);
        }
    }

    private static void recordRequest(HttpExchange exchange) throws IOException {
        exchange.getRequestBody().readAllBytes();
        lastRequestPath = exchange.getRequestURI().getPath();
        lastRequestHeaders.clear();
        exchange.getRequestHeaders().forEach((name, values) -> {
            if (!values.isEmpty()) {
                lastRequestHeaders.put(name.toLowerCase(Locale.ROOT), values.get(0));
            }
        });
    }

    private static String header(String name) {
        return lastRequestHeaders.get(name.toLowerCase(Locale.ROOT));
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

    private String managerToken() {
        return TestTokens.builder().subject("2").email("manager@example.com").role("PROJECT_MANAGER").build();
    }

    @Test
    void usersRouteWithoutJwtReturns401() {
        webTestClient.get().uri("/api/v1/users/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void skillsRouteWithoutJwtReturns401() {
        webTestClient.get().uri("/api/v1/skills")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void authenticatedRequestIsRoutedToUserSkillServiceForUsersPath() {
        webTestClient.get().uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + validToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(lastRequestPath).isEqualTo("/api/v1/users/me");
    }

    @Test
    void authenticatedRequestIsRoutedToUserSkillServiceForSkillsPath() {
        webTestClient.get().uri("/api/v1/skills")
                .header("Authorization", "Bearer " + validToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(lastRequestPath).isEqualTo("/api/v1/skills");
    }

    @Test
    void originalPathIsPreservedWithoutRewriteForNestedUsersSubPath() {
        webTestClient.get().uri("/api/v1/users/me/skills")
                .header("Authorization", "Bearer " + validToken())
                .exchange()
                .expectStatus().isOk();

        // No StripPrefix/RewritePath/SetPath filter is applied: the mock receives the exact
        // nested path the client sent, proving the gateway forwards it unmodified.
        assertThat(lastRequestPath).isEqualTo("/api/v1/users/me/skills");
    }

    @Test
    void authorizationHeaderIsPreservedDownstreamForUsersRoute() {
        String token = validToken();

        webTestClient.get().uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        assertThat(header("Authorization")).isEqualTo("Bearer " + token);
    }

    @Test
    void validatedTrustedIdentityHeadersReachDownstreamForUsersRoute() {
        String token = TestTokens.builder().subject("77").email("trusted@example.com").role("PROJECT_MANAGER").build();

        webTestClient.get().uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        assertThat(header("X-Auth-User-Id")).isEqualTo("77");
        assertThat(header("X-Auth-User-Email")).isEqualTo("trusted@example.com");
        assertThat(header("X-Auth-User-Role")).isEqualTo("PROJECT_MANAGER");
    }

    @Test
    void spoofedInboundIdentityHeadersAreReplacedByValidatedJwtIdentityForSkillsRoute() {
        String token = TestTokens.builder().subject("1").email("real@example.com").role("USER").build();

        webTestClient.get().uri("/api/v1/skills")
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
    void userCannotCreateSkillAndBackendIsNotCalled() {
        int before = skillsBackendRequestCount.get();

        webTestClient.post().uri("/api/v1/skills")
                .header("Authorization", "Bearer " + validToken())
                .bodyValue("{\"name\":\"Java\"}")
                .exchange()
                .expectStatus().isForbidden();

        assertThat(skillsBackendRequestCount.get()).isEqualTo(before);
    }

    @Test
    void managerCanCreateSkillAndRequestReachesBackend() {
        int before = skillsBackendRequestCount.get();

        webTestClient.post().uri("/api/v1/skills")
                .header("Authorization", "Bearer " + managerToken())
                .bodyValue("{\"name\":\"Java\"}")
                .exchange()
                .expectStatus().isOk();

        assertThat(skillsBackendRequestCount.get()).isEqualTo(before + 1);
    }

    @Test
    void userCannotUpdateSkillAndBackendIsNotCalled() {
        int before = skillsBackendRequestCount.get();

        webTestClient.put().uri("/api/v1/skills/5")
                .header("Authorization", "Bearer " + validToken())
                .bodyValue("{\"name\":\"Java\"}")
                .exchange()
                .expectStatus().isForbidden();

        assertThat(skillsBackendRequestCount.get()).isEqualTo(before);
    }

    @Test
    void managerCanUpdateSkillAndRequestReachesBackend() {
        int before = skillsBackendRequestCount.get();

        webTestClient.put().uri("/api/v1/skills/5")
                .header("Authorization", "Bearer " + managerToken())
                .bodyValue("{\"name\":\"Java\"}")
                .exchange()
                .expectStatus().isOk();

        assertThat(skillsBackendRequestCount.get()).isEqualTo(before + 1);
    }

    @Test
    void userCannotDeleteSkillAndBackendIsNotCalled() {
        int before = skillsBackendRequestCount.get();

        webTestClient.delete().uri("/api/v1/skills/5")
                .header("Authorization", "Bearer " + validToken())
                .exchange()
                .expectStatus().isForbidden();

        assertThat(skillsBackendRequestCount.get()).isEqualTo(before);
    }

    @Test
    void managerCanDeleteSkillAndRequestReachesBackend() {
        int before = skillsBackendRequestCount.get();

        webTestClient.delete().uri("/api/v1/skills/5")
                .header("Authorization", "Bearer " + managerToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(skillsBackendRequestCount.get()).isEqualTo(before + 1);
    }

    @Test
    void userCanListSkillCatalog() {
        webTestClient.get().uri("/api/v1/skills")
                .header("Authorization", "Bearer " + validToken())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void userCanViewSingleSkill() {
        webTestClient.get().uri("/api/v1/skills/5")
                .header("Authorization", "Bearer " + validToken())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void missingJwtOnSkillMutationReturns401NotForbidden() {
        webTestClient.post().uri("/api/v1/skills")
                .bodyValue("{\"name\":\"Java\"}")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
