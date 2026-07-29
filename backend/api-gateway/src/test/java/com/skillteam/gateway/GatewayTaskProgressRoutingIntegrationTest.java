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
 * Full-stack Gateway routing/security test for the Task & Progress Service routes
 * ({@code /api/v1/tasks/**} and {@code /api/v1/projects/*&#47;task-progress-summary}), and for
 * the continued correctness of the broader {@code /api/v1/projects/**} route to the Project &
 * Team Service. Routes to lightweight local mocks standing in for TASK-PROGRESS-SERVICE and
 * PROJECT-TEAM-SERVICE (via route URI overrides), so no running Eureka Server or backend service
 * is required.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayTaskProgressRoutingIntegrationTest {

    private static HttpServer mockTaskProgressServer;
    private static HttpServer mockProjectTeamServer;

    private static final Map<String, String> lastTaskProgressRequestHeaders = new ConcurrentHashMap<>();
    private static volatile String lastTaskProgressRequestPath;
    private static final AtomicInteger tasksBackendRequestCount = new AtomicInteger();
    private static final AtomicInteger summaryBackendRequestCount = new AtomicInteger();
    private static final AtomicInteger projectTeamBackendRequestCount = new AtomicInteger();

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void registerRoutes(DynamicPropertyRegistry registry) throws IOException {
        mockTaskProgressServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        mockTaskProgressServer.createContext("/api/v1/tasks", exchange -> {
            recordTaskProgressRequest(exchange);
            tasksBackendRequestCount.incrementAndGet();
            respond(exchange, 200, "{}");
        });
        mockTaskProgressServer.createContext("/api/v1/projects", exchange -> {
            recordTaskProgressRequest(exchange);
            summaryBackendRequestCount.incrementAndGet();
            respond(exchange, 200, "{}");
        });
        mockTaskProgressServer.setExecutor(null);
        mockTaskProgressServer.start();

        mockProjectTeamServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        mockProjectTeamServer.createContext("/api/v1/projects", exchange -> {
            consumeRequestBody(exchange);
            projectTeamBackendRequestCount.incrementAndGet();
            respond(exchange, 200, "{}");
        });
        mockProjectTeamServer.setExecutor(null);
        mockProjectTeamServer.start();

        int taskProgressPort = mockTaskProgressServer.getAddress().getPort();
        int projectTeamPort = mockProjectTeamServer.getAddress().getPort();

        // Spring Boot binds a list-typed @ConfigurationProperties entirely from a single
        // property source; it does not merge individual indices across sources. So the whole
        // route list relevant to this test is redeclared here, preserving the production
        // ordering: the specific task-progress-summary route must precede the broader
        // project-team-service route, since the latter's predicate is also a match for it.
        registry.add("spring.cloud.gateway.server.webflux.routes[0].id", () -> "task-progress-summary");
        registry.add("spring.cloud.gateway.server.webflux.routes[0].uri", () -> "http://localhost:" + taskProgressPort);
        registry.add("spring.cloud.gateway.server.webflux.routes[0].predicates[0]",
                () -> "Path=/api/v1/projects/*/task-progress-summary");

        registry.add("spring.cloud.gateway.server.webflux.routes[1].id", () -> "project-team-service");
        registry.add("spring.cloud.gateway.server.webflux.routes[1].uri", () -> "http://localhost:" + projectTeamPort);
        registry.add("spring.cloud.gateway.server.webflux.routes[1].predicates[0]", () -> "Path=/api/v1/projects/**");

        registry.add("spring.cloud.gateway.server.webflux.routes[2].id", () -> "task-progress-tasks");
        registry.add("spring.cloud.gateway.server.webflux.routes[2].uri", () -> "http://localhost:" + taskProgressPort);
        registry.add("spring.cloud.gateway.server.webflux.routes[2].predicates[0]", () -> "Path=/api/v1/tasks/**");
    }

    @AfterAll
    static void stopMockServers() {
        if (mockTaskProgressServer != null) {
            mockTaskProgressServer.stop(0);
        }
        if (mockProjectTeamServer != null) {
            mockProjectTeamServer.stop(0);
        }
    }

    private static void recordTaskProgressRequest(HttpExchange exchange) throws IOException {
        consumeRequestBody(exchange);
        lastTaskProgressRequestPath = exchange.getRequestURI().getPath();
        lastTaskProgressRequestHeaders.clear();
        exchange.getRequestHeaders().forEach((name, values) -> {
            if (!values.isEmpty()) {
                lastTaskProgressRequestHeaders.put(name.toLowerCase(Locale.ROOT), values.get(0));
            }
        });
    }

    private static void consumeRequestBody(HttpExchange exchange) throws IOException {
        exchange.getRequestBody().readAllBytes();
    }

    private static String taskProgressHeader(String name) {
        return lastTaskProgressRequestHeaders.get(name.toLowerCase(Locale.ROOT));
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

    // ---- routing: /api/v1/tasks/** reaches TASK-PROGRESS-SERVICE ----

    @Test
    void userCanListTasksAndRequestReachesTaskProgressBackend() {
        int before = tasksBackendRequestCount.get();

        webTestClient.get().uri("/api/v1/tasks")
                .header("Authorization", "Bearer " + userToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before + 1);
        assertThat(lastTaskProgressRequestPath).isEqualTo("/api/v1/tasks");
    }

    @Test
    void userCanGetTaskByIdAndRequestReachesTaskProgressBackend() {
        int before = tasksBackendRequestCount.get();

        webTestClient.get().uri("/api/v1/tasks/5")
                .header("Authorization", "Bearer " + userToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before + 1);
        assertThat(lastTaskProgressRequestPath).isEqualTo("/api/v1/tasks/5");
    }

    // ---- routing: progress-summary reaches TASK-PROGRESS-SERVICE, not PROJECT-TEAM-SERVICE ----

    @Test
    void userCanViewProgressSummaryAndRequestReachesTaskProgressBackend() {
        int summaryBefore = summaryBackendRequestCount.get();
        int projectTeamBefore = projectTeamBackendRequestCount.get();

        webTestClient.get().uri("/api/v1/projects/5/task-progress-summary")
                .header("Authorization", "Bearer " + userToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(summaryBackendRequestCount.get()).isEqualTo(summaryBefore + 1);
        assertThat(projectTeamBackendRequestCount.get()).isEqualTo(projectTeamBefore);
        assertThat(lastTaskProgressRequestPath).isEqualTo("/api/v1/projects/5/task-progress-summary");
    }

    // ---- regression: ordinary /api/v1/projects/{id} still routes to PROJECT-TEAM-SERVICE ----

    @Test
    void userCanGetSingleProjectAndRequestReachesProjectTeamBackendNotTaskProgress() {
        int projectTeamBefore = projectTeamBackendRequestCount.get();
        int summaryBefore = summaryBackendRequestCount.get();

        webTestClient.get().uri("/api/v1/projects/5")
                .header("Authorization", "Bearer " + userToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(projectTeamBackendRequestCount.get()).isEqualTo(projectTeamBefore + 1);
        assertThat(summaryBackendRequestCount.get()).isEqualTo(summaryBefore);
    }

    // ---- authentication: missing/invalid JWT ----

    @Test
    void missingJwtOnTaskCreateReturns401AndBackendIsNotContacted() {
        int before = tasksBackendRequestCount.get();

        webTestClient.post().uri("/api/v1/tasks")
                .bodyValue("{\"projectId\":1,\"title\":\"Design\",\"priority\":\"LOW\"}")
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before);
    }

    @Test
    void malformedJwtOnTaskCreateReturns401AndBackendIsNotContacted() {
        int before = tasksBackendRequestCount.get();

        webTestClient.post().uri("/api/v1/tasks")
                .header("Authorization", "Bearer not-a-valid-jwt")
                .bodyValue("{\"projectId\":1,\"title\":\"Design\",\"priority\":\"LOW\"}")
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before);
    }

    // ---- USER forbidden on manager-only task mutations, backend never contacted ----

    @Test
    void userCannotCreateTaskAndBackendIsNotCalled() {
        int before = tasksBackendRequestCount.get();

        webTestClient.post().uri("/api/v1/tasks")
                .header("Authorization", "Bearer " + userToken())
                .bodyValue("{\"projectId\":1,\"title\":\"Design\",\"priority\":\"LOW\"}")
                .exchange()
                .expectStatus().isForbidden();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before);
    }

    @Test
    void userCannotUpdateTaskAndBackendIsNotCalled() {
        int before = tasksBackendRequestCount.get();

        webTestClient.put().uri("/api/v1/tasks/5")
                .header("Authorization", "Bearer " + userToken())
                .bodyValue("{\"title\":\"Design\",\"status\":\"TODO\",\"priority\":\"LOW\"}")
                .exchange()
                .expectStatus().isForbidden();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before);
    }

    @Test
    void userCannotDeleteTaskAndBackendIsNotCalled() {
        int before = tasksBackendRequestCount.get();

        webTestClient.delete().uri("/api/v1/tasks/5")
                .header("Authorization", "Bearer " + userToken())
                .exchange()
                .expectStatus().isForbidden();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before);
    }

    @Test
    void userCannotAssignTaskAndBackendIsNotCalled() {
        int before = tasksBackendRequestCount.get();

        webTestClient.patch().uri("/api/v1/tasks/5/assign")
                .header("Authorization", "Bearer " + userToken())
                .bodyValue("{\"assignedAuthUserId\":9}")
                .exchange()
                .expectStatus().isForbidden();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before);
    }

    // ---- PROJECT_MANAGER can perform every task operation, all reach the backend ----

    @Test
    void managerCanCreateTaskAndRequestReachesBackend() {
        int before = tasksBackendRequestCount.get();

        webTestClient.post().uri("/api/v1/tasks")
                .header("Authorization", "Bearer " + managerToken())
                .bodyValue("{\"projectId\":1,\"title\":\"Design\",\"priority\":\"LOW\"}")
                .exchange()
                .expectStatus().isOk();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before + 1);
    }

    @Test
    void managerCanUpdateTaskAndRequestReachesBackend() {
        int before = tasksBackendRequestCount.get();

        webTestClient.put().uri("/api/v1/tasks/5")
                .header("Authorization", "Bearer " + managerToken())
                .bodyValue("{\"title\":\"Design\",\"status\":\"TODO\",\"priority\":\"LOW\"}")
                .exchange()
                .expectStatus().isOk();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before + 1);
    }

    @Test
    void managerCanDeleteTaskAndRequestReachesBackend() {
        int before = tasksBackendRequestCount.get();

        webTestClient.delete().uri("/api/v1/tasks/5")
                .header("Authorization", "Bearer " + managerToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before + 1);
    }

    @Test
    void managerCanAssignTaskAndRequestReachesBackend() {
        int before = tasksBackendRequestCount.get();

        webTestClient.patch().uri("/api/v1/tasks/5/assign")
                .header("Authorization", "Bearer " + managerToken())
                .bodyValue("{\"assignedAuthUserId\":9}")
                .exchange()
                .expectStatus().isOk();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before + 1);
    }

    @Test
    void managerCanUpdateTaskStatusAndRequestReachesBackend() {
        int before = tasksBackendRequestCount.get();

        webTestClient.patch().uri("/api/v1/tasks/5/status")
                .header("Authorization", "Bearer " + managerToken())
                .bodyValue("{\"status\":\"IN_PROGRESS\"}")
                .exchange()
                .expectStatus().isOk();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before + 1);
    }

    @Test
    void managerCanUpdateTaskProgressAndRequestReachesBackend() {
        int before = tasksBackendRequestCount.get();

        webTestClient.patch().uri("/api/v1/tasks/5/progress")
                .header("Authorization", "Bearer " + managerToken())
                .bodyValue("{\"progressPercentage\":50}")
                .exchange()
                .expectStatus().isOk();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before + 1);
    }

    @Test
    void managerCanListTasksAndRequestReachesTaskProgressBackend() {
        int before = tasksBackendRequestCount.get();

        webTestClient.get().uri("/api/v1/tasks")
                .header("Authorization", "Bearer " + managerToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before + 1);
    }

    @Test
    void managerCanGetTaskById() {
        webTestClient.get().uri("/api/v1/tasks/5")
                .header("Authorization", "Bearer " + managerToken())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void managerCanViewProgressSummary() {
        webTestClient.get().uri("/api/v1/projects/5/task-progress-summary")
                .header("Authorization", "Bearer " + managerToken())
                .exchange()
                .expectStatus().isOk();
    }

    // ---- USER is forwarded (NOT blocked) for status/progress: Gateway must not enforce
    // ---- manager-only here, since the Task & Progress Service allows the assignee too. ----

    @Test
    void userStatusUpdateIsForwardedNotBlockedByGateway() {
        int before = tasksBackendRequestCount.get();

        webTestClient.patch().uri("/api/v1/tasks/5/status")
                .header("Authorization", "Bearer " + userToken())
                .bodyValue("{\"status\":\"IN_PROGRESS\"}")
                .exchange()
                .expectStatus().isOk();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before + 1);
    }

    @Test
    void userProgressUpdateIsForwardedNotBlockedByGateway() {
        int before = tasksBackendRequestCount.get();

        webTestClient.patch().uri("/api/v1/tasks/5/progress")
                .header("Authorization", "Bearer " + userToken())
                .bodyValue("{\"progressPercentage\":50}")
                .exchange()
                .expectStatus().isOk();

        assertThat(tasksBackendRequestCount.get()).isEqualTo(before + 1);
    }

    // ---- trusted identity propagation ----

    @Test
    void validatedTrustedIdentityHeadersReachDownstreamForTaskRoute() {
        String token = TestTokens.builder().subject("42").email("pm@example.com").role("PROJECT_MANAGER").build();

        webTestClient.get().uri("/api/v1/tasks/5")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        assertThat(taskProgressHeader("X-Auth-User-Id")).isEqualTo("42");
        assertThat(taskProgressHeader("X-Auth-User-Email")).isEqualTo("pm@example.com");
        assertThat(taskProgressHeader("X-Auth-User-Role")).isEqualTo("PROJECT_MANAGER");
    }

    // ---- spoofing protection: client-supplied identity headers are replaced ----

    @Test
    void spoofedInboundIdentityHeadersAreReplacedForTaskRoute() {
        String token = TestTokens.builder().subject("1").email("real-user@example.com").role("USER").build();

        webTestClient.get().uri("/api/v1/tasks/5")
                .header("Authorization", "Bearer " + token)
                .header("X-Auth-User-Id", "999")
                .header("X-Auth-User-Email", "attacker@example.com")
                .header("X-Auth-User-Role", "PROJECT_MANAGER")
                .exchange()
                .expectStatus().isOk();

        assertThat(taskProgressHeader("X-Auth-User-Id")).isEqualTo("1");
        assertThat(taskProgressHeader("X-Auth-User-Email")).isEqualTo("real-user@example.com");
        assertThat(taskProgressHeader("X-Auth-User-Role")).isEqualTo("USER");
    }
}
