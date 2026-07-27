package com.skillteam.projectteam.client;

import com.skillteam.projectteam.entity.ProficiencyLevel;
import com.skillteam.projectteam.exception.UserSkillServiceUnavailableException;
import com.skillteam.projectteam.security.IdentityHeaderResolver;
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

class UserSkillServiceClientTest {

    private MockRestServiceServer mockServer;
    private UserSkillServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new UserSkillServiceClient(builder);
    }

    @Test
    void fetchSkillsReturnsSkillsAndForwardsIdentityHeaders() {
        mockServer.expect(requestTo("http://USER-SKILL-SERVICE/api/v1/users/42/skills"))
                .andExpect(method(GET))
                .andExpect(header(IdentityHeaderResolver.USER_ID_HEADER, "7"))
                .andExpect(header(IdentityHeaderResolver.USER_ROLE_HEADER, "PROJECT_MANAGER"))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": 1,
                            "skillId": 5,
                            "skillName": "Java",
                            "proficiencyLevel": "EXPERT",
                            "skillActive": true,
                            "createdAt": "2026-01-01T00:00:00Z",
                            "updatedAt": "2026-01-01T00:00:00Z"
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<RemoteUserSkillResponse> result = client.fetchSkills(42L, 7L, "PROJECT_MANAGER");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).skillName()).isEqualTo("Java");
        assertThat(result.get(0).proficiencyLevel()).isEqualTo(ProficiencyLevel.EXPERT);
        mockServer.verify();
    }

    @Test
    void fetchSkillsWrapsDownstreamErrorAsUserSkillServiceUnavailableException() {
        mockServer.expect(requestTo("http://USER-SKILL-SERVICE/api/v1/users/42/skills"))
                .andExpect(method(GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchSkills(42L, 7L, "PROJECT_MANAGER"))
                .isInstanceOf(UserSkillServiceUnavailableException.class);

        mockServer.verify();
    }
}
