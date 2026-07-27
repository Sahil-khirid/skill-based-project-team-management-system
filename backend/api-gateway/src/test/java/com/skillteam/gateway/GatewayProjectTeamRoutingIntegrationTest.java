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
 * Full-stack Gateway routing/security test for the Project & Team Service routes, and for the
 * Gateway-level containment of the User & Skill Service's internal 7E-A lookup endpoint. Routes
 * to lightweight local mocks standing in for PROJECT-TEAM-SERVICE and USER-SKILL-SERVICE (via
 * route URI overrides), so no running Eureka Server or backend service is required.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayProjectTeamRoutingIntegrationTest {

    private static HttpServer mockProjectTeamServer;
    private static HttpServer mockUserSkillServer;

    private static final Map<String, String> lastProjectTeamRequestHeaders = new ConcurrentHashMap<>();
    private static volatile String lastProjectTeamRequestPath;
    private static final AtomicInteger projectTeamRequestCount = new AtomicInteger();
    private static final AtomicInteger meSkillsRequestCount = new AtomicInteger();
    private static final AtomicInteger internalUserSkillLookupRequestCount = new AtomicInteger();

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void registerRoutes(DynamicPropertyRegistry registry) throws IOException {
        mockProjectTeamServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        mockProjectTeamServer.createContext("/api/v1/projects", exchange -> {
            recordProjectTeamRequest(exchange);
            projectTeamRequestCount.incrementAndGet();
            respond(exchange, 200, "{}");
        });
        mockProjectTeamServer.setExecutor(null);
        mockProjectTeamServer.start();

        mockUserSkillServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        // Registered before the broader "/api/v1/users" context: com.sun.net.httpserver.HttpServer
        // dispatches on the longest matching context path, so this one wins for exactly this path.
        mockUserSkillServer.createContext("/api/v1/users/me/skills", exchange -> {
            consumeRequestBody(exchange);
            meSkillsRequestCount.incrementAndGet();
            respond(exchange, 200, "[]");
        });
        mockUserSkillServer.createContext("/api/v1/users", exchange -> {
            consumeRequestBody(exchange);
            internalUserSkillLookupRequestCount.incrementAndGet();
            respond(exchange, 200, "[]");
        });
        mockUserSkillServer.setExecutor(null);
        mockUserSkillServer.start();

        int projectTeamPort = mockProjectTeamServer.getAddress().getPort();
        int userSkillPort = mockUserSkillServer.getAddress().getPort();

        // Spring Boot binds a list-typed @ConfigurationProperties entirely from a single
        // property source; it does not merge individual indices across sources. Only the two
        // routes this test exercises are redeclared here, pointed at the local mocks.
        registry.add("spring.cloud.gateway.server.webflux.routes[0].id", () -> "project-team-service");
        registry.add("spring.cloud.gateway.server.webflux.routes[0].uri", () -> "http://localhost:" + projectTeamPort);
        registry.add("spring.cloud.gateway.server.webflux.routes[0].predicates[0]", () -> "Path=/api/v1/projects/**");

        registry.add("spring.cloud.gateway.server.webflux.routes[1].id", () -> "user-skill-users");
        registry.add("spring.cloud.gateway.server.webflux.routes[1].uri", () -> "http://localhost:" + userSkillPort);
        registry.add("spring.cloud.gateway.server.webflux.routes[1].predicates[0]", () -> "Path=/api/v1/users/**");
    }

    @AfterAll
    static void stopMockServers() {
        if (mockProjectTeamServer != null) {
            mockProjectTeamServer.stop(0);
        }
        if (mockUserSkillServer != null) {
            mockUserSkillServer.stop(0);
        }
    }

    private static void recordProjectTeamRequest(HttpExchange exchange) throws IOException {
        consumeRequestBody(exchange);
        lastProjectTeamRequestPath = exchange.getRequestURI().getPath();
        lastProjectTeamRequestHeaders.clear();
        exchange.getRequestHeaders().forEach((name, values) -> {
            if (!values.isEmpty()) {
                lastProjectTeamRequestHeaders.put(name.toLowerCase(Locale.ROOT), values.get(0));
            }
        });
    }

    private static void consumeRequestBody(HttpExchange exchange) throws IOException {
        exchange.getRequestBody().readAllBytes();
    }

    private static String projectTeamHeader(String name) {
        return lastProjectTeamRequestHeaders.get(name.toLowerCase(Locale.ROOT));
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String userToken() {
        return TestTokens.builder().subject("1").email("user@example.com").role("USER").build();
    }

    private String managerToken() {
        return TestTokens.builder().subject("2").email("manager@example.com").role("PROJECT_MANAGER").build();
    }

    // ---- A: missing JWT on a protected Project & Team endpoint ----

    @Test
    void missingJwtOnProjectCreateReturns401() {
        webTestClient.post().uri("/api/v1/projects")
                .bodyValue("{\"name\":\"Apollo\"}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ---- B/C: project creation ----

    @Test
    void userCannotCreateProjectAndBackendIsNotCalled() {
        int before = projectTeamRequestCount.get();

        webTestClient.post().uri("/api/v1/projects")
                .header("Authorization", "Bearer " + userToken())
                .bodyValue("{\"name\":\"Apollo\"}")
                .exchange()
                .expectStatus().isForbidden();

        assertThat(projectTeamRequestCount.get()).isEqualTo(before);
    }

    @Test
    void managerCanCreateProjectAndRequestReachesBackend() {
        int before = projectTeamRequestCount.get();

        webTestClient.post().uri("/api/v1/projects")
                .header("Authorization", "Bearer " + managerToken())
                .bodyValue("{\"name\":\"Apollo\"}")
                .exchange()
                .expectStatus().isOk();

        assertThat(projectTeamRequestCount.get()).isEqualTo(before + 1);
        assertThat(lastProjectTeamRequestPath).isEqualTo("/api/v1/projects");
    }

    // ---- project update ----

    @Test
    void userCannotUpdateProjectAndBackendIsNotCalled() {
        int before = projectTeamRequestCount.get();

        webTestClient.put().uri("/api/v1/projects/5")
                .header("Authorization", "Bearer " + userToken())
                .bodyValue("{\"name\":\"Apollo\",\"status\":\"PLANNING\"}")
                .exchange()
                .expectStatus().isForbidden();

        assertThat(projectTeamRequestCount.get()).isEqualTo(before);
    }

    @Test
    void managerCanUpdateProjectAndRequestReachesBackend() {
        int before = projectTeamRequestCount.get();

        webTestClient.put().uri("/api/v1/projects/5")
                .header("Authorization", "Bearer " + managerToken())
                .bodyValue("{\"name\":\"Apollo\",\"status\":\"PLANNING\"}")
                .exchange()
                .expectStatus().isOk();

        assertThat(projectTeamRequestCount.get()).isEqualTo(before + 1);
    }

    // ---- project delete ----

    @Test
    void userCannotDeleteProjectAndBackendIsNotCalled() {
        int before = projectTeamRequestCount.get();

        webTestClient.delete().uri("/api/v1/projects/5")
                .header("Authorization", "Bearer " + userToken())
                .exchange()
                .expectStatus().isForbidden();

        assertThat(projectTeamRequestCount.get()).isEqualTo(before);
    }

    @Test
    void managerCanDeleteProjectAndRequestReachesBackend() {
        int before = projectTeamRequestCount.get();

        webTestClient.delete().uri("/api/v1/projects/5")
                .header("Authorization", "Bearer " + managerToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(projectTeamRequestCount.get()).isEqualTo(before + 1);
    }

    // ---- D/E: project member mutation ----

    @Test
    void userCannotAddMemberAndBackendIsNotCalled() {
        int before = projectTeamRequestCount.get();

        webTestClient.post().uri("/api/v1/projects/5/members")
                .header("Authorization", "Bearer " + userToken())
                .bodyValue("{\"authUserId\":7,\"role\":\"CONTRIBUTOR\"}")
                .exchange()
                .expectStatus().isForbidden();

        assertThat(projectTeamRequestCount.get()).isEqualTo(before);
    }

    @Test
    void managerCanAddMemberAndRequestReachesBackend() {
        int before = projectTeamRequestCount.get();

        webTestClient.post().uri("/api/v1/projects/5/members")
                .header("Authorization", "Bearer " + managerToken())
                .bodyValue("{\"authUserId\":7,\"role\":\"CONTRIBUTOR\"}")
                .exchange()
                .expectStatus().isOk();

        assertThat(projectTeamRequestCount.get()).isEqualTo(before + 1);
    }

    @Test
    void userCannotRemoveMemberAndBackendIsNotCalled() {
        int before = projectTeamRequestCount.get();

        webTestClient.delete().uri("/api/v1/projects/5/members/7")
                .header("Authorization", "Bearer " + userToken())
                .exchange()
                .expectStatus().isForbidden();

        assertThat(projectTeamRequestCount.get()).isEqualTo(before);
    }

    @Test
    void managerCanRemoveMemberAndRequestReachesBackend() {
        int before = projectTeamRequestCount.get();

        webTestClient.delete().uri("/api/v1/projects/5/members/7")
                .header("Authorization", "Bearer " + managerToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(projectTeamRequestCount.get()).isEqualTo(before + 1);
    }

    // ---- F/G: required-skill mutation ----

    @Test
    void userCannotAddRequiredSkillAndBackendIsNotCalled() {
        int before = projectTeamRequestCount.get();

        webTestClient.post().uri("/api/v1/projects/5/required-skills")
                .header("Authorization", "Bearer " + userToken())
                .bodyValue("{\"skillId\":9,\"proficiencyLevel\":\"INTERMEDIATE\"}")
                .exchange()
                .expectStatus().isForbidden();

        assertThat(projectTeamRequestCount.get()).isEqualTo(before);
    }

    @Test
    void managerCanAddRequiredSkillAndRequestReachesBackend() {
        int before = projectTeamRequestCount.get();

        webTestClient.post().uri("/api/v1/projects/5/required-skills")
                .header("Authorization", "Bearer " + managerToken())
                .bodyValue("{\"skillId\":9,\"proficiencyLevel\":\"INTERMEDIATE\"}")
                .exchange()
                .expectStatus().isOk();

        assertThat(projectTeamRequestCount.get()).isEqualTo(before + 1);
    }

    @Test
    void userCannotRemoveRequiredSkillAndBackendIsNotCalled() {
        int before = projectTeamRequestCount.get();

        webTestClient.delete().uri("/api/v1/projects/5/required-skills/9")
                .header("Authorization", "Bearer " + userToken())
                .exchange()
                .expectStatus().isForbidden();

        assertThat(projectTeamRequestCount.get()).isEqualTo(before);
    }

    @Test
    void managerCanRemoveRequiredSkillAndRequestReachesBackend() {
        int before = projectTeamRequestCount.get();

        webTestClient.delete().uri("/api/v1/projects/5/required-skills/9")
                .header("Authorization", "Bearer " + managerToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(projectTeamRequestCount.get()).isEqualTo(before + 1);
    }

    // ---- H/I: member recommendations ----

    @Test
    void userCannotAccessRecommendationsAndBackendIsNotCalled() {
        int before = projectTeamRequestCount.get();

        webTestClient.get().uri("/api/v1/projects/5/member-recommendations")
                .header("Authorization", "Bearer " + userToken())
                .exchange()
                .expectStatus().isForbidden();

        assertThat(projectTeamRequestCount.get()).isEqualTo(before);
    }

    @Test
    void managerCanAccessRecommendationsAndRequestReachesBackend() {
        int before = projectTeamRequestCount.get();

        webTestClient.get().uri("/api/v1/projects/5/member-recommendations")
                .header("Authorization", "Bearer " + managerToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(projectTeamRequestCount.get()).isEqualTo(before + 1);
    }

    // ---- J: read operations remain open to both roles ----

    @Test
    void userCanListProjects() {
        webTestClient.get().uri("/api/v1/projects")
                .header("Authorization", "Bearer " + userToken())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void userCanGetSingleProject() {
        webTestClient.get().uri("/api/v1/projects/5")
                .header("Authorization", "Bearer " + userToken())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void userCanListProjectMembers() {
        webTestClient.get().uri("/api/v1/projects/5/members")
                .header("Authorization", "Bearer " + userToken())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void userCanListProjectRequiredSkills() {
        webTestClient.get().uri("/api/v1/projects/5/required-skills")
                .header("Authorization", "Bearer " + userToken())
                .exchange()
                .expectStatus().isOk();
    }

    // ---- K: trusted identity propagation ----

    @Test
    void validatedTrustedIdentityHeadersReachDownstreamForProjectRoute() {
        String token = TestTokens.builder().subject("42").email("pm@example.com").role("PROJECT_MANAGER").build();

        webTestClient.post().uri("/api/v1/projects")
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"name\":\"Apollo\"}")
                .exchange()
                .expectStatus().isOk();

        assertThat(projectTeamHeader("X-Auth-User-Id")).isEqualTo("42");
        assertThat(projectTeamHeader("X-Auth-User-Email")).isEqualTo("pm@example.com");
        assertThat(projectTeamHeader("X-Auth-User-Role")).isEqualTo("PROJECT_MANAGER");
    }

    // ---- L: spoofing protection ----

    @Test
    void spoofedInboundIdentityHeadersAreReplacedForProjectRoute() {
        String token = TestTokens.builder().subject("2").email("real-manager@example.com").role("PROJECT_MANAGER").build();

        webTestClient.post().uri("/api/v1/projects")
                .header("Authorization", "Bearer " + token)
                .header("X-Auth-User-Id", "999")
                .header("X-Auth-User-Email", "attacker@example.com")
                .header("X-Auth-User-Role", "PROJECT_MANAGER")
                .bodyValue("{\"name\":\"Apollo\"}")
                .exchange()
                .expectStatus().isOk();

        assertThat(projectTeamHeader("X-Auth-User-Id")).isEqualTo("2");
        assertThat(projectTeamHeader("X-Auth-User-Email")).isEqualTo("real-manager@example.com");
        assertThat(projectTeamHeader("X-Auth-User-Role")).isEqualTo("PROJECT_MANAGER");
    }

    // ---- M/N/O: internal 7E-A lookup must never be publicly reachable ----

    @Test
    void userCallingInternalUserSkillLookupIsForbidden() {
        webTestClient.get().uri("/api/v1/users/5/skills")
                .header("Authorization", "Bearer " + userToken())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void managerCallingInternalUserSkillLookupIsForbidden() {
        webTestClient.get().uri("/api/v1/users/5/skills")
                .header("Authorization", "Bearer " + managerToken())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void internalUserSkillLookupBackendIsNeverContactedForEitherRole() {
        int before = internalUserSkillLookupRequestCount.get();

        webTestClient.get().uri("/api/v1/users/5/skills")
                .header("Authorization", "Bearer " + userToken())
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.get().uri("/api/v1/users/5/skills")
                .header("Authorization", "Bearer " + managerToken())
                .exchange()
                .expectStatus().isForbidden();

        assertThat(internalUserSkillLookupRequestCount.get()).isEqualTo(before);
    }

    // ---- P/Q: /api/v1/users/me/skills regression ----

    @Test
    void userMeSkillsRouteStillWorksAfterInternalLookupIsBlocked() {
        int before = meSkillsRequestCount.get();

        webTestClient.get().uri("/api/v1/users/me/skills")
                .header("Authorization", "Bearer " + userToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(meSkillsRequestCount.get()).isEqualTo(before + 1);
    }

    @Test
    void managerMeSkillsRouteStillWorksAfterInternalLookupIsBlocked() {
        int before = meSkillsRequestCount.get();

        webTestClient.get().uri("/api/v1/users/me/skills")
                .header("Authorization", "Bearer " + managerToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(meSkillsRequestCount.get()).isEqualTo(before + 1);
    }
}
