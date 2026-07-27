package com.skillteam.projectteam;

import com.skillteam.projectteam.client.RemoteUserSkillResponse;
import com.skillteam.projectteam.client.UserSkillServiceClient;
import com.skillteam.projectteam.entity.ProficiencyLevel;
import com.skillteam.projectteam.entity.Project;
import com.skillteam.projectteam.entity.ProjectMember;
import com.skillteam.projectteam.entity.ProjectMemberRole;
import com.skillteam.projectteam.entity.ProjectRequiredSkill;
import com.skillteam.projectteam.entity.ProjectStatus;
import com.skillteam.projectteam.exception.UserSkillServiceUnavailableException;
import com.skillteam.projectteam.repository.ProjectMemberRepository;
import com.skillteam.projectteam.repository.ProjectRepository;
import com.skillteam.projectteam.repository.ProjectRequiredSkillRepository;
import com.skillteam.projectteam.security.IdentityHeaderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectRecommendationControllerTest {

    private static final String ID_HEADER = IdentityHeaderResolver.USER_ID_HEADER;
    private static final String ROLE_HEADER = IdentityHeaderResolver.USER_ROLE_HEADER;
    private static final String MANAGER = "PROJECT_MANAGER";
    private static final String USER = "USER";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectRequiredSkillRepository projectRequiredSkillRepository;

    @MockitoBean
    private UserSkillServiceClient userSkillServiceClient;

    @AfterEach
    void cleanUp() {
        projectMemberRepository.deleteAll();
        projectRequiredSkillRepository.deleteAll();
        projectRepository.deleteAll();
    }

    private Project createProject(String name) {
        return projectRepository.saveAndFlush(
                new Project(name, name.toLowerCase(Locale.ROOT), null, ProjectStatus.PLANNING, 1L));
    }

    private void addMember(Long projectId, Long authUserId) {
        projectMemberRepository.saveAndFlush(new ProjectMember(projectId, authUserId, ProjectMemberRole.MEMBER));
    }

    private void addRequiredSkill(Long projectId, Long skillId, ProficiencyLevel level) {
        projectRequiredSkillRepository.saveAndFlush(new ProjectRequiredSkill(projectId, skillId, level));
    }

    private RemoteUserSkillResponse remoteSkill(Long skillId, String skillName, ProficiencyLevel level) {
        return new RemoteUserSkillResponse(skillId, skillId, skillName, level, true, Instant.now(), Instant.now());
    }

    private String recommendationsUrl(Long projectId) {
        return "/api/v1/projects/" + projectId + "/member-recommendations";
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder builder, long authUserId, String role) {
        return builder.header(ID_HEADER, String.valueOf(authUserId)).header(ROLE_HEADER, role);
    }

    // --- happy path ---

    @Test
    void projectManagerGetsRankedRecommendations() throws Exception {
        Project project = createProject("Apollo");
        addRequiredSkill(project.getId(), 100L, ProficiencyLevel.INTERMEDIATE);
        addRequiredSkill(project.getId(), 101L, ProficiencyLevel.BEGINNER);
        addMember(project.getId(), 10L);
        addMember(project.getId(), 20L);

        when(userSkillServiceClient.fetchSkills(eq(10L), anyLong(), anyString())).thenReturn(List.of(
                remoteSkill(100L, "Java", ProficiencyLevel.EXPERT),
                remoteSkill(101L, "SQL", ProficiencyLevel.BEGINNER)));
        when(userSkillServiceClient.fetchSkills(eq(20L), anyLong(), anyString())).thenReturn(List.of(
                remoteSkill(100L, "Java", ProficiencyLevel.BEGINNER)));

        mockMvc.perform(as(get(recommendationsUrl(project.getId())), 1L, MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(project.getId().intValue()))
                .andExpect(jsonPath("$.recommendations.length()").value(2))
                .andExpect(jsonPath("$.recommendations[0].authUserId").value(10))
                .andExpect(jsonPath("$.recommendations[0].matchedSkillCount").value(2))
                .andExpect(jsonPath("$.recommendations[0].requiredSkillCount").value(2))
                .andExpect(jsonPath("$.recommendations[0].missingSkillCount").value(0))
                .andExpect(jsonPath("$.recommendations[0].matchPercentage").value(100.0))
                .andExpect(jsonPath("$.recommendations[1].authUserId").value(20))
                .andExpect(jsonPath("$.recommendations[1].matchedSkillCount").value(0))
                .andExpect(jsonPath("$.recommendations[1].missingSkillCount").value(2))
                .andExpect(jsonPath("$.recommendations[1].matchPercentage").value(0.0));
    }

    // --- empty members ---

    @Test
    void projectWithNoMembersReturnsEmptyRecommendationsArray() throws Exception {
        Project project = createProject("Gemini");
        addRequiredSkill(project.getId(), 100L, ProficiencyLevel.BEGINNER);

        mockMvc.perform(as(get(recommendationsUrl(project.getId())), 1L, MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations.length()").value(0));
    }

    // --- nonexistent project ---

    @Test
    void recommendationsForNonexistentProjectReturnsNotFound() throws Exception {
        mockMvc.perform(as(get(recommendationsUrl(999999L)), 1L, MANAGER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- downstream failure ---

    @Test
    void downstreamUserSkillServiceFailureReturnsServiceUnavailable() throws Exception {
        Project project = createProject("Voyager");
        addRequiredSkill(project.getId(), 100L, ProficiencyLevel.BEGINNER);
        addMember(project.getId(), 10L);

        when(userSkillServiceClient.fetchSkills(any(), any(), any()))
                .thenThrow(new UserSkillServiceUnavailableException("downstream unavailable", new RuntimeException()));

        mockMvc.perform(as(get(recommendationsUrl(project.getId())), 1L, MANAGER))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    // --- authorization ---

    @Test
    void userRoleCannotViewRecommendations() throws Exception {
        Project project = createProject("Kepler");

        mockMvc.perform(as(get(recommendationsUrl(project.getId())), 2L, USER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access is denied."));
    }

    @Test
    void missingUserIdHeaderReturnsUnauthorized() throws Exception {
        Project project = createProject("Artemis");

        mockMvc.perform(get(recommendationsUrl(project.getId())).header(ROLE_HEADER, MANAGER))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void missingRoleHeaderReturnsUnauthorized() throws Exception {
        Project project = createProject("Cassini");

        mockMvc.perform(get(recommendationsUrl(project.getId())).header(ID_HEADER, "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unsupportedRoleHeaderReturnsUnauthorized() throws Exception {
        Project project = createProject("Juno");

        mockMvc.perform(get(recommendationsUrl(project.getId())).header(ID_HEADER, "1").header(ROLE_HEADER, "ADMIN"))
                .andExpect(status().isUnauthorized());
    }
}
