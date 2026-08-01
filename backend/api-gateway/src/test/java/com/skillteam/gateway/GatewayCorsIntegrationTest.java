package com.skillteam.gateway;

import com.skillteam.gateway.security.TestTokens;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Gateway CORS integration test: proves the global CORS policy (allowed local frontend origins,
 * allowed methods/headers, no credentials) is enforced consistently for preflight and actual
 * requests, on both public and JWT-protected routes, without weakening existing JWT/role
 * authorization. Routes to lightweight local mocks standing in for AUTH-SERVICE and
 * TASK-PROGRESS-SERVICE (via route URI overrides), so no running Eureka Server or backend service
 * is required.
 *
 * <p>Uses a manually built, real-socket {@link WebTestClient} (bound to the running server's
 * random port) rather than the framework-injected one: Spring's CORS same-origin check inspects
 * the incoming request's absolute URI (scheme/host/port), which is only populated end-to-end over
 * a real HTTP connection.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
class GatewayCorsIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String ALLOWED_ORIGIN_ALT = "http://127.0.0.1:5173";
    private static final String DISALLOWED_ORIGIN = "http://evil.example";

    private static HttpServer mockAuthServer;
    private static HttpServer mockTaskServer;
    private static final AtomicInteger authBackendRequestCount = new AtomicInteger();
    private static final AtomicInteger taskBackendRequestCount = new AtomicInteger();

    @LocalServerPort
    private int localServerPort;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUpClient() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + localServerPort)
                .build();
    }

    @DynamicPropertySource
    static void registerRoutesAndCors(DynamicPropertyRegistry registry) throws IOException {
        mockAuthServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        mockAuthServer.createContext("/api/v1/auth/login", exchange -> {
            consumeRequestBody(exchange);
            authBackendRequestCount.incrementAndGet();
            respond(exchange, 200, "{\"accessToken\":\"stub\",\"tokenType\":\"Bearer\",\"expiresIn\":900}");
        });
        mockAuthServer.setExecutor(null);
        mockAuthServer.start();

        mockTaskServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        mockTaskServer.createContext("/api/v1/tasks", exchange -> {
            consumeRequestBody(exchange);
            taskBackendRequestCount.incrementAndGet();
            respond(exchange, 200, "{}");
        });
        mockTaskServer.setExecutor(null);
        mockTaskServer.start();

        int authPort = mockAuthServer.getAddress().getPort();
        int taskPort = mockTaskServer.getAddress().getPort();

        // Spring Boot binds a list-typed @ConfigurationProperties entirely from a single
        // property source; it does not merge individual indices across sources. So the whole
        // route list relevant to this test is redeclared here (same pattern used by the other
        // Gateway integration tests).
        registry.add("spring.cloud.gateway.server.webflux.routes[0].id", () -> "auth-service");
        registry.add("spring.cloud.gateway.server.webflux.routes[0].uri", () -> "http://localhost:" + authPort);
        registry.add("spring.cloud.gateway.server.webflux.routes[0].predicates[0]", () -> "Path=/api/v1/auth/**");

        registry.add("spring.cloud.gateway.server.webflux.routes[1].id", () -> "task-progress-tasks");
        registry.add("spring.cloud.gateway.server.webflux.routes[1].uri", () -> "http://localhost:" + taskPort);
        registry.add("spring.cloud.gateway.server.webflux.routes[1].predicates[0]", () -> "Path=/api/v1/tasks/**");

        // src/test/resources/application.yml fully shadows src/main/resources/application.yml on
        // the test classpath (Spring Boot resolves a single "application.yml", and test-classes
        // precedes classes on the classpath) rather than merging with it. So the globalcors policy
        // declared in the main application.yml is otherwise invisible here and must be redeclared,
        // exactly as production defines it.
        registry.add("spring.cloud.gateway.server.webflux.globalcors.add-to-simple-url-handler-mapping", () -> "true");
        registry.add("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedOrigins[0]",
                () -> ALLOWED_ORIGIN);
        registry.add("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedOrigins[1]",
                () -> ALLOWED_ORIGIN_ALT);
        registry.add("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedMethods[0]", () -> "GET");
        registry.add("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedMethods[1]", () -> "POST");
        registry.add("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedMethods[2]", () -> "PUT");
        registry.add("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedMethods[3]", () -> "PATCH");
        registry.add("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedMethods[4]", () -> "DELETE");
        registry.add("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedMethods[5]", () -> "OPTIONS");
        registry.add("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedHeaders[0]", () -> "Authorization");
        registry.add("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedHeaders[1]", () -> "Content-Type");
        registry.add("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedHeaders[2]", () -> "Accept");
        registry.add("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowCredentials", () -> "false");
        registry.add("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].maxAge", () -> "3600");
    }

    @AfterAll
    static void stopMockServers() {
        if (mockAuthServer != null) {
            mockAuthServer.stop(0);
        }
        if (mockTaskServer != null) {
            mockTaskServer.stop(0);
        }
    }

    private static void consumeRequestBody(HttpExchange exchange) throws IOException {
        exchange.getRequestBody().readAllBytes();
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

    private static String allowOrigin(WebTestClient.ResponseSpec response) {
        return response.returnResult(Void.class).getResponseHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    // ---- 1. Login preflight from localhost is allowed, without a JWT. Also proves the full
    // ---- policy: Access-Control-Max-Age, no Access-Control-Allow-Credentials (allowCredentials
    // ---- is false), and Accept honored as a requested header. ----

    @Test
    void loginPreflightFromLocalhostIsAllowedWithoutJwt() {
        int before = authBackendRequestCount.get();

        WebTestClient.ResponseSpec response = webTestClient.method(HttpMethod.OPTIONS).uri("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, Accept")
                .exchange();

        response.expectStatus().is2xxSuccessful();
        HttpHeaders headers = response.returnResult(Void.class).getResponseHeaders();
        assertThat(headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(ALLOWED_ORIGIN);
        assertThat(headers.get(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isNotNull().anySatisfy(
                value -> assertThat(value.toUpperCase(Locale.ROOT)).contains("POST"));
        String allowedHeaders = String.join(",", headers.getOrDefault(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, List.of()))
                .toLowerCase(Locale.ROOT);
        assertThat(allowedHeaders).contains("content-type");
        assertThat(allowedHeaders).contains("accept");
        assertThat(headers.getFirst(HttpHeaders.ACCESS_CONTROL_MAX_AGE)).isEqualTo("3600");
        assertThat(headers.containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isFalse();
        assertThat(authBackendRequestCount.get()).isEqualTo(before);
    }

    // ---- 2. Protected endpoint preflight is allowed without a JWT, and never reaches the backend ----

    @Test
    void protectedEndpointPreflightIsAllowedWithoutJwtAndBackendIsNotContacted() {
        int before = taskBackendRequestCount.get();

        WebTestClient.ResponseSpec response = webTestClient.method(HttpMethod.OPTIONS).uri("/api/v1/tasks/5/progress")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type")
                .exchange();

        response.expectStatus().is2xxSuccessful();
        HttpHeaders headers = response.returnResult(Void.class).getResponseHeaders();
        assertThat(headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(ALLOWED_ORIGIN);
        assertThat(headers.get(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isNotNull().anySatisfy(
                value -> assertThat(value.toUpperCase(Locale.ROOT)).contains("PATCH"));
        String allowedHeaders = String.join(",", headers.getOrDefault(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, List.of()))
                .toLowerCase(Locale.ROOT);
        assertThat(allowedHeaders).contains("authorization");
        assertThat(allowedHeaders).contains("content-type");
        assertThat(taskBackendRequestCount.get()).isEqualTo(before);
    }

    // ---- 3. Alternate local origin (127.0.0.1:5173) is allowed and echoed exactly ----

    @Test
    void alternateLocalOriginIsAllowedAndEchoedExactly() {
        WebTestClient.ResponseSpec response = webTestClient.method(HttpMethod.OPTIONS).uri("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_ALT)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type")
                .exchange();

        response.expectStatus().is2xxSuccessful();
        assertThat(allowOrigin(response)).isEqualTo(ALLOWED_ORIGIN_ALT);
    }

    // ---- 4. Disallowed origin is rejected: no Access-Control-Allow-Origin, backend never contacted ----

    @Test
    void disallowedOriginPreflightIsRejectedAndBackendIsNotContacted() {
        int before = authBackendRequestCount.get();

        WebTestClient.ResponseSpec response = webTestClient.method(HttpMethod.OPTIONS).uri("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type")
                .exchange();

        response.expectStatus().isForbidden();
        assertThat(allowOrigin(response)).isNull();
        assertThat(authBackendRequestCount.get()).isEqualTo(before);
    }

    // ---- 5. An actual public login request from an allowed origin receives CORS headers ----

    @Test
    void actualLoginRequestFromAllowedOriginReceivesCorsHeaders() {
        int before = authBackendRequestCount.get();

        WebTestClient.ResponseSpec response = webTestClient.post().uri("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"user@example.com\",\"password\":\"secret\"}")
                .exchange();

        response.expectStatus().isOk();
        assertThat(allowOrigin(response)).isEqualTo(ALLOWED_ORIGIN);
        assertThat(authBackendRequestCount.get()).isEqualTo(before + 1);
    }

    // ---- 6. An actual authenticated request from an allowed origin receives CORS headers,
    // ---- and existing JWT authentication continues to work. ----

    @Test
    void actualAuthenticatedRequestFromAllowedOriginReceivesCorsHeadersAndAuthenticationStillWorks() {
        int before = taskBackendRequestCount.get();

        WebTestClient.ResponseSpec response = webTestClient.get().uri("/api/v1/tasks")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken())
                .exchange();

        response.expectStatus().isOk();
        assertThat(allowOrigin(response)).isEqualTo(ALLOWED_ORIGIN);
        assertThat(taskBackendRequestCount.get()).isEqualTo(before + 1);
    }

    // ---- 7. A non-CORS request (no Origin header) behaves exactly as before: no CORS headers,
    // ---- existing authentication rules still apply. ----

    @Test
    void requestWithoutOriginHeaderHasNoCorsHeadersAndExistingAuthStillApplies() {
        WebTestClient.ResponseSpec authenticated = webTestClient.get().uri("/api/v1/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken())
                .exchange();

        authenticated.expectStatus().isOk();
        assertThat(allowOrigin(authenticated)).isNull();

        webTestClient.get().uri("/api/v1/tasks")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ---- 8. Preflight permission does not weaken the real request's authorization:
    // ---- POST /api/v1/tasks still requires PROJECT_MANAGER / a valid JWT. ----

    @Test
    void preflightPassingDoesNotMakeProtectedTaskCreationPublic() {
        int before = taskBackendRequestCount.get();

        webTestClient.method(HttpMethod.OPTIONS).uri("/api/v1/tasks")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type")
                .exchange()
                .expectStatus().is2xxSuccessful();

        webTestClient.post().uri("/api/v1/tasks")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .bodyValue("{\"projectId\":1,\"title\":\"Design\",\"priority\":\"LOW\"}")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);

        webTestClient.post().uri("/api/v1/tasks")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken())
                .bodyValue("{\"projectId\":1,\"title\":\"Design\",\"priority\":\"LOW\"}")
                .exchange()
                .expectStatus().isForbidden();

        assertThat(taskBackendRequestCount.get()).isEqualTo(before);
    }

    // ---- 9. An actual unauthenticated request to a protected route from an allowed origin still
    // ---- receives CORS headers on its 401 response, so the browser can read the real error
    // ---- instead of seeing only a CORS/network failure. ----

    @Test
    void actualUnauthenticatedRequestFromAllowedOriginReceivesCorsHeadersOn401() {
        int before = taskBackendRequestCount.get();

        WebTestClient.ResponseSpec response = webTestClient.get().uri("/api/v1/tasks")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .exchange();

        response.expectStatus().isUnauthorized();
        assertThat(allowOrigin(response)).isEqualTo(ALLOWED_ORIGIN);
        assertThat(taskBackendRequestCount.get()).isEqualTo(before);
    }

    // ---- 10. An actual forbidden request (valid JWT, insufficient role) to a protected route
    // ---- from an allowed origin still receives CORS headers on its 403 response. ----

    @Test
    void actualForbiddenRequestFromAllowedOriginReceivesCorsHeadersOn403() {
        int before = taskBackendRequestCount.get();

        WebTestClient.ResponseSpec response = webTestClient.post().uri("/api/v1/tasks")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"projectId\":1,\"title\":\"Design\",\"priority\":\"LOW\"}")
                .exchange();

        response.expectStatus().isForbidden();
        assertThat(allowOrigin(response)).isEqualTo(ALLOWED_ORIGIN);
        assertThat(taskBackendRequestCount.get()).isEqualTo(before);
    }
}
