package com.skillteam.projectteam.client;

import com.skillteam.projectteam.exception.UserSkillServiceUnavailableException;
import com.skillteam.projectteam.security.IdentityHeaderResolver;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Consumes the User &amp; Skill Service's internal lookup endpoint
 * ({@code GET /api/v1/users/{authUserId}/skills}) via Eureka-resolved, load-balanced calls.
 */
@Component
public class UserSkillServiceClient {

    private static final String BASE_URL = "http://USER-SKILL-SERVICE";

    private final RestClient restClient;

    public UserSkillServiceClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
    }

    public List<RemoteUserSkillResponse> fetchSkills(Long targetAuthUserId, Long callerUserId, String callerRole) {
        try {
            return restClient.get()
                    .uri("/api/v1/users/{authUserId}/skills", targetAuthUserId)
                    .header(IdentityHeaderResolver.USER_ID_HEADER, String.valueOf(callerUserId))
                    .header(IdentityHeaderResolver.USER_ROLE_HEADER, callerRole)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<RemoteUserSkillResponse>>() {
                    });
        } catch (RestClientException | IllegalStateException ex) {
            // IllegalStateException is thrown by Spring Cloud LoadBalancer (e.g. "No instances
            // available for USER-SKILL-SERVICE") when the downstream service isn't registered in
            // Eureka; RestClientException covers connection failures and non-2xx responses.
            // Both mean the downstream is unreachable and must surface as 503, not an unhandled 500.
            throw new UserSkillServiceUnavailableException(
                    "Unable to retrieve skills from User & Skill Service for user " + targetAuthUserId + ".", ex);
        }
    }
}
