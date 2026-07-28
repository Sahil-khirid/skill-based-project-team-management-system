package com.skillteam.taskprogress.client;

import com.skillteam.taskprogress.exception.ProjectTeamServiceUnavailableException;
import com.skillteam.taskprogress.security.IdentityHeaderResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProjectTeamServiceClientTest {

    private MockRestServiceServer mockServer;
    private ProjectTeamServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new ProjectTeamServiceClient(builder);
    }

    @Test
    void fetchMembersReturnsMembersAndForwardsIdentityHeaders() {
        mockServer.expect(requestTo("http://PROJECT-TEAM-SERVICE/api/v1/projects/9/members"))
                .andExpect(method(GET))
                .andExpect(header(IdentityHeaderResolver.USER_ID_HEADER, "7"))
                .andExpect(header(IdentityHeaderResolver.USER_ROLE_HEADER, "PROJECT_MANAGER"))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": 1,
                            "projectId": 9,
                            "authUserId": 5,
                            "role": "MEMBER",
                            "createdAt": "2026-01-01T00:00:00Z",
                            "updatedAt": "2026-01-01T00:00:00Z"
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<RemoteProjectMemberResponse> result = client.fetchMembers(9L, 7L, "PROJECT_MANAGER");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).authUserId()).isEqualTo(5L);
        assertThat(result.get(0).role()).isEqualTo("MEMBER");
        mockServer.verify();
    }

    @Test
    void fetchMembersWrapsDownstreamErrorAsProjectTeamServiceUnavailableException() {
        mockServer.expect(requestTo("http://PROJECT-TEAM-SERVICE/api/v1/projects/9/members"))
                .andExpect(method(GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchMembers(9L, 7L, "PROJECT_MANAGER"))
                .isInstanceOf(ProjectTeamServiceUnavailableException.class);

        mockServer.verify();
    }
}
