package com.skillteam.projectteam;

import com.skillteam.projectteam.entity.Project;
import com.skillteam.projectteam.entity.ProjectStatus;
import com.skillteam.projectteam.repository.ProjectRepository;
import com.skillteam.projectteam.repository.ProjectRequiredSkillRepository;
import com.skillteam.projectteam.security.IdentityHeaderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectRequiredSkillControllerTest {

    private static final String ID_HEADER = IdentityHeaderResolver.USER_ID_HEADER;
    private static final String ROLE_HEADER = IdentityHeaderResolver.USER_ROLE_HEADER;
    private static final String MANAGER = "PROJECT_MANAGER";
    private static final String USER = "USER";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRequiredSkillRepository projectRequiredSkillRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @AfterEach
    void cleanUp() {
        projectRequiredSkillRepository.deleteAll();
        projectRepository.deleteAll();
    }

    private Project createProject(String name) {
        return projectRepository.saveAndFlush(
                new Project(name, name.toLowerCase(Locale.ROOT), null, ProjectStatus.PLANNING, 1L));
    }

    private String requiredSkillsUrl(Long projectId) {
        return "/api/v1/projects/" + projectId + "/required-skills";
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder builder, long authUserId, String role) {
        return builder.header(ID_HEADER, String.valueOf(authUserId)).header(ROLE_HEADER, role);
    }

    private String body(Long skillId, String proficiencyLevel) {
        return "{\"skillId\":" + skillId + ",\"proficiencyLevel\":" + quoteOrNull(proficiencyLevel) + "}";
    }

    private String quoteOrNull(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    // --- successful add ---

    @Test
    void addRequiredSkillReturnsCreated() throws Exception {
        Project project = createProject("Apollo");

        mockMvc.perform(as(post(requiredSkillsUrl(project.getId())), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "INTERMEDIATE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.projectId").value(project.getId().intValue()))
                .andExpect(jsonPath("$.skillId").value(100))
                .andExpect(jsonPath("$.proficiencyLevel").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    // --- add to nonexistent project ---

    @Test
    void addRequiredSkillToNonexistentProjectReturnsNotFound() throws Exception {
        mockMvc.perform(as(post(requiredSkillsUrl(999999L)), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "INTERMEDIATE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- duplicate required skill ---

    @Test
    void addDuplicateRequiredSkillReturnsConflict() throws Exception {
        Project project = createProject("Gemini");

        mockMvc.perform(as(post(requiredSkillsUrl(project.getId())), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "BEGINNER")))
                .andExpect(status().isCreated());

        mockMvc.perform(as(post(requiredSkillsUrl(project.getId())), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "EXPERT")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // --- same skill, different projects ---

    @Test
    void sameSkillCanBeRequiredByDifferentProjects() throws Exception {
        Project first = createProject("Voyager 1");
        Project second = createProject("Voyager 2");

        mockMvc.perform(as(post(requiredSkillsUrl(first.getId())), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "ADVANCED")))
                .andExpect(status().isCreated());

        mockMvc.perform(as(post(requiredSkillsUrl(second.getId())), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "ADVANCED")))
                .andExpect(status().isCreated());
    }

    // --- list empty ---

    @Test
    void listWithNoRequiredSkillsReturnsEmptyArray() throws Exception {
        Project project = createProject("Artemis");

        mockMvc.perform(as(get(requiredSkillsUrl(project.getId())), 1L, USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- list for nonexistent project ---

    @Test
    void listForNonexistentProjectReturnsNotFound() throws Exception {
        mockMvc.perform(as(get(requiredSkillsUrl(999999L)), 1L, USER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- list only returns required skills of the given project ---

    @Test
    void listReturnsOnlyRequiredSkillsOfGivenProject() throws Exception {
        Project first = createProject("Orion");
        Project second = createProject("Skylab");

        mockMvc.perform(as(post(requiredSkillsUrl(first.getId())), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "BEGINNER"))).andExpect(status().isCreated());
        mockMvc.perform(as(post(requiredSkillsUrl(first.getId())), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(101L, "EXPERT"))).andExpect(status().isCreated());
        mockMvc.perform(as(post(requiredSkillsUrl(second.getId())), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(102L, "ADVANCED"))).andExpect(status().isCreated());

        mockMvc.perform(as(get(requiredSkillsUrl(first.getId())), 1L, USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].skillId").value(100))
                .andExpect(jsonPath("$[1].skillId").value(101));
    }

    // --- successful removal ---

    @Test
    void removeRequiredSkillReturnsNoContent() throws Exception {
        Project project = createProject("Hubble");

        mockMvc.perform(as(post(requiredSkillsUrl(project.getId())), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "BEGINNER")))
                .andExpect(status().isCreated());

        mockMvc.perform(as(delete(requiredSkillsUrl(project.getId()) + "/100"), 1L, MANAGER))
                .andExpect(status().isNoContent());
    }

    // --- removal for nonexistent project ---

    @Test
    void removeRequiredSkillForNonexistentProjectReturnsNotFound() throws Exception {
        mockMvc.perform(as(delete(requiredSkillsUrl(999999L) + "/100"), 1L, MANAGER))
                .andExpect(status().isNotFound());
    }

    // --- removal of absent required skill ---

    @Test
    void removeAbsentRequiredSkillReturnsNotFound() throws Exception {
        Project project = createProject("Cassini");

        mockMvc.perform(as(delete(requiredSkillsUrl(project.getId()) + "/999"), 1L, MANAGER))
                .andExpect(status().isNotFound());
    }

    // --- repeated removal ---

    @Test
    void repeatedRemoveReturnsNotFound() throws Exception {
        Project project = createProject("Juno");

        mockMvc.perform(as(post(requiredSkillsUrl(project.getId())), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "BEGINNER")))
                .andExpect(status().isCreated());

        mockMvc.perform(as(delete(requiredSkillsUrl(project.getId()) + "/100"), 1L, MANAGER))
                .andExpect(status().isNoContent());

        mockMvc.perform(as(delete(requiredSkillsUrl(project.getId()) + "/100"), 1L, MANAGER))
                .andExpect(status().isNotFound());
    }

    // --- removed required skill no longer appears in list ---

    @Test
    void removedRequiredSkillIsHiddenFromList() throws Exception {
        Project project = createProject("Galileo");

        mockMvc.perform(as(post(requiredSkillsUrl(project.getId())), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "BEGINNER")))
                .andExpect(status().isCreated());

        mockMvc.perform(as(delete(requiredSkillsUrl(project.getId()) + "/100"), 1L, MANAGER))
                .andExpect(status().isNoContent());

        mockMvc.perform(as(get(requiredSkillsUrl(project.getId())), 1L, USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- validation failures ---

    @Test
    void addWithMissingSkillIdReturnsBadRequest() throws Exception {
        Project project = createProject("New Horizons");

        mockMvc.perform(as(post(requiredSkillsUrl(project.getId())), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(null, "BEGINNER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addWithMissingProficiencyLevelReturnsBadRequest() throws Exception {
        Project project = createProject("Pioneer");

        mockMvc.perform(as(post(requiredSkillsUrl(project.getId())), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addWithInvalidProficiencyLevelReturnsBadRequest() throws Exception {
        Project project = createProject("Rosetta");

        mockMvc.perform(as(post(requiredSkillsUrl(project.getId())), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "MASTER")))
                .andExpect(status().isBadRequest());
    }

    // --- missing/malformed/zero/negative caller identity ---

    @Test
    void addWithoutUserIdHeaderReturnsUnauthorized() throws Exception {
        Project project = createProject("Chandrayaan");

        mockMvc.perform(post(requiredSkillsUrl(project.getId())).header(ROLE_HEADER, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(body(100L, "BEGINNER")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void addWithMalformedUserIdHeaderReturnsUnauthorized() throws Exception {
        Project project = createProject("Tianwen");

        mockMvc.perform(post(requiredSkillsUrl(project.getId())).header(ID_HEADER, "not-a-number")
                        .header(ROLE_HEADER, MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "BEGINNER")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addWithZeroUserIdHeaderReturnsUnauthorized() throws Exception {
        Project project = createProject("Akatsuki");

        mockMvc.perform(post(requiredSkillsUrl(project.getId())).header(ID_HEADER, "0")
                        .header(ROLE_HEADER, MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "BEGINNER")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addWithNegativeUserIdHeaderReturnsUnauthorized() throws Exception {
        Project project = createProject("BepiColombo");

        mockMvc.perform(post(requiredSkillsUrl(project.getId())).header(ID_HEADER, "-3")
                        .header(ROLE_HEADER, MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "BEGINNER")))
                .andExpect(status().isUnauthorized());
    }

    // --- missing/unsupported role header ---

    @Test
    void addWithoutRoleHeaderReturnsUnauthorized() throws Exception {
        Project project = createProject("Parker");

        mockMvc.perform(post(requiredSkillsUrl(project.getId())).header(ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON).content(body(100L, "BEGINNER")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addWithUnsupportedRoleHeaderReturnsUnauthorized() throws Exception {
        Project project = createProject("Lucy");

        mockMvc.perform(post(requiredSkillsUrl(project.getId())).header(ID_HEADER, "1").header(ROLE_HEADER, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(body(100L, "BEGINNER")))
                .andExpect(status().isUnauthorized());
    }

    // --- USER can list but not add/remove ---

    @Test
    void userRoleCanListRequiredSkills() throws Exception {
        Project project = createProject("Dawn");

        mockMvc.perform(as(get(requiredSkillsUrl(project.getId())), 2L, USER))
                .andExpect(status().isOk());
    }

    @Test
    void userRoleCannotAddRequiredSkill() throws Exception {
        Project project = createProject("Kepler");

        mockMvc.perform(as(post(requiredSkillsUrl(project.getId())), 2L, USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "BEGINNER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access is denied."));
    }

    @Test
    void userRoleCannotRemoveRequiredSkill() throws Exception {
        Project project = createProject("Spitzer");

        mockMvc.perform(as(post(requiredSkillsUrl(project.getId())), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "BEGINNER")))
                .andExpect(status().isCreated());

        mockMvc.perform(as(delete(requiredSkillsUrl(project.getId()) + "/100"), 2L, USER))
                .andExpect(status().isForbidden());
    }

    // --- PROJECT_MANAGER full flow ---

    @Test
    void projectManagerCanAddListAndRemoveRequiredSkill() throws Exception {
        Project project = createProject("TESS");

        mockMvc.perform(as(post(requiredSkillsUrl(project.getId())), 1L, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100L, "EXPERT")))
                .andExpect(status().isCreated());

        mockMvc.perform(as(get(requiredSkillsUrl(project.getId())), 1L, MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(as(delete(requiredSkillsUrl(project.getId()) + "/100"), 1L, MANAGER))
                .andExpect(status().isNoContent());
    }
}
