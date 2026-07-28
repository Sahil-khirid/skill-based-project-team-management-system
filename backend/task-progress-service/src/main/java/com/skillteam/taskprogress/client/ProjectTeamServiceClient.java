package com.skillteam.taskprogress.client;

import com.skillteam.taskprogress.exception.ProjectTeamServiceUnavailableException;
import com.skillteam.taskprogress.security.IdentityHeaderResolver;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Consumes the Project &amp; Team Service's member listing endpoint
 * ({@code GET /api/v1/projects/{projectId}/members}) via Eureka-resolved, load-balanced calls.
 */
@Component
public class ProjectTeamServiceClient {

    private static final String BASE_URL = "http://PROJECT-TEAM-SERVICE";

    private final RestClient restClient;

    public ProjectTeamServiceClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
    }

    public List<RemoteProjectMemberResponse> fetchMembers(Long projectId, Long callerUserId, String callerRole) {
        try {
            return restClient.get()
                    .uri("/api/v1/projects/{projectId}/members", projectId)
                    .header(IdentityHeaderResolver.USER_ID_HEADER, String.valueOf(callerUserId))
                    .header(IdentityHeaderResolver.USER_ROLE_HEADER, callerRole)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<RemoteProjectMemberResponse>>() {
                    });
        } catch (RestClientException ex) {
            throw new ProjectTeamServiceUnavailableException(
                    "Unable to retrieve project members from Project & Team Service for project " + projectId + ".", ex);
        }
    }
}
