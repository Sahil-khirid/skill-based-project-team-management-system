package com.skillteam.gateway.security;

import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class GatewaySecurityConfig {

    private final GatewayReactiveAuthenticationManager authenticationManager;
    private final GatewayAuthenticationEntryPoint authenticationEntryPoint;
    private final GatewayAccessDeniedHandler accessDeniedHandler;
    private final GlobalCorsProperties globalCorsProperties;

    public GatewaySecurityConfig(GatewayReactiveAuthenticationManager authenticationManager,
                                  GatewayAuthenticationEntryPoint authenticationEntryPoint,
                                  GatewayAccessDeniedHandler accessDeniedHandler,
                                  GlobalCorsProperties globalCorsProperties) {
        this.authenticationManager = authenticationManager;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.globalCorsProperties = globalCorsProperties;
    }

    @Bean
    public SecurityWebFilterChain gatewaySecurityWebFilterChain(ServerHttpSecurity http) {
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(authenticationManager);
        authenticationWebFilter.setServerAuthenticationConverter(new BearerTokenServerAuthenticationConverter());
        authenticationWebFilter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());
        authenticationWebFilter.setAuthenticationFailureHandler(
                new ServerAuthenticationEntryPointFailureHandler(authenticationEntryPoint));

        // Reuses the single CORS policy bound from spring.cloud.gateway.server.webflux.globalcors
        // (application.yml is the sole source of truth for origins/methods/headers) so that CORS
        // processing runs as a Spring Security WebFilter, ahead of authentication/authorization.
        // Without this, a 401/403 produced by the entry point / access-denied handler never passes
        // through the Gateway routing layer's own CORS handling, and the browser sees a CORS
        // failure instead of the real error response.
        UrlBasedCorsConfigurationSource corsConfigurationSource = new UrlBasedCorsConfigurationSource();
        corsConfigurationSource.setCorsConfigurations(globalCorsProperties.getCorsConfigurations());

        http
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .logout(logout -> logout.disable())
                .requestCache(requestCache -> requestCache.requestCache(NoOpServerRequestCache.getInstance()))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchange -> exchange
                        // CORS preflight: browsers send OPTIONS without credentials before the
                        // real request. This permits only the preflight itself — the actual
                        // request below still runs through the normal JWT/role rules.
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
                        .pathMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/skills").hasRole("PROJECT_MANAGER")
                        .pathMatchers(HttpMethod.PUT, "/api/v1/skills/**").hasRole("PROJECT_MANAGER")
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/skills/**").hasRole("PROJECT_MANAGER")
                        // Must precede the internal user-skill lookup deny rule below: the single-segment
                        // wildcard in "/api/v1/users/*/skills" also matches this literal "me" path, so the
                        // public self-service route needs its own higher-precedence rule to stay reachable.
                        .pathMatchers(HttpMethod.GET, "/api/v1/users/me/skills").authenticated()
                        // Internal service-to-service lookup (User & Skill Service's InternalUserSkillController,
                        // introduced for 7E-A/7E-B). Not a public client API - never expose it through the Gateway.
                        .pathMatchers(HttpMethod.GET, "/api/v1/users/*/skills").denyAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/projects",
                                "/api/v1/projects/*/members",
                                "/api/v1/projects/*/required-skills").hasRole("PROJECT_MANAGER")
                        .pathMatchers(HttpMethod.PUT, "/api/v1/projects/*").hasRole("PROJECT_MANAGER")
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/projects/*",
                                "/api/v1/projects/*/members/*",
                                "/api/v1/projects/*/required-skills/*").hasRole("PROJECT_MANAGER")
                        .pathMatchers(HttpMethod.GET, "/api/v1/projects/*/member-recommendations").hasRole("PROJECT_MANAGER")
                        .pathMatchers(HttpMethod.POST, "/api/v1/tasks").hasRole("PROJECT_MANAGER")
                        .pathMatchers(HttpMethod.PUT, "/api/v1/tasks/*").hasRole("PROJECT_MANAGER")
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/tasks/*").hasRole("PROJECT_MANAGER")
                        .pathMatchers(HttpMethod.PATCH, "/api/v1/tasks/*/assign").hasRole("PROJECT_MANAGER")
                        // Status and progress updates are intentionally NOT manager-only here: the
                        // Task & Progress Service's requireManagerOrAssignee(...) allows the assigned
                        // USER too, so both roles fall through to the authenticated() rule below and
                        // the downstream service performs the assignee ownership check.
                        .anyExchange().authenticated());

        return http.build();
    }
}
