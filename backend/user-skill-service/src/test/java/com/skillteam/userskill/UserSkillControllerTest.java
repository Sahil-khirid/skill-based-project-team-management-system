package com.skillteam.userskill;

import com.skillteam.userskill.entity.Skill;
import com.skillteam.userskill.entity.UserProfile;
import com.skillteam.userskill.repository.SkillRepository;
import com.skillteam.userskill.repository.UserProfileRepository;
import com.skillteam.userskill.repository.UserSkillRepository;
import com.skillteam.userskill.security.IdentityHeaderResolver;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserSkillControllerTest {

    private static final String SKILLS_URL = "/api/v1/users/me/skills";
    private static final String ID_HEADER = IdentityHeaderResolver.USER_ID_HEADER;
    private static final String ROLE_HEADER = IdentityHeaderResolver.USER_ROLE_HEADER;
    private static final String MANAGER = "PROJECT_MANAGER";
    private static final String USER = "USER";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserSkillRepository userSkillRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private SkillRepository skillRepository;

    @AfterEach
    void cleanUp() {
        userSkillRepository.deleteAll();
        skillRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    private UserProfile createProfile(long authUserId) {
        return userProfileRepository.saveAndFlush(new UserProfile(authUserId, "Test User " + authUserId, null, null));
    }

    private Skill createSkill(String name) {
        return skillRepository.saveAndFlush(new Skill(name, name.toLowerCase(Locale.ROOT), null));
    }

    private Skill deactivate(Skill skill) {
        skill.setActive(false);
        return skillRepository.saveAndFlush(skill);
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder builder, long authUserId, String role) {
        return builder.header(ID_HEADER, String.valueOf(authUserId)).header(ROLE_HEADER, role);
    }

    private String createBody(Long skillId, String proficiencyLevel) {
        return "{\"skillId\":" + skillId + ",\"proficiencyLevel\":" + quoteOrNull(proficiencyLevel) + "}";
    }

    private String updateBody(String proficiencyLevel) {
        return "{\"proficiencyLevel\":" + quoteOrNull(proficiencyLevel) + "}";
    }

    private String quoteOrNull(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    // --- successful assignment ---

    @Test
    void createAssignmentReturnsCreated() throws Exception {
        createProfile(1L);
        Skill skill = createSkill("Java");

        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), "BEGINNER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.skillId").value(skill.getId().intValue()))
                .andExpect(jsonPath("$.skillName").value("Java"))
                .andExpect(jsonPath("$.proficiencyLevel").value("BEGINNER"))
                .andExpect(jsonPath("$.skillActive").value(true))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.userProfileId").doesNotExist())
                .andExpect(jsonPath("$.normalizedName").doesNotExist());
    }

    // --- duplicate assignment ---

    @Test
    void createDuplicateAssignmentReturnsConflict() throws Exception {
        createProfile(1L);
        Skill skill = createSkill("Java");

        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), "BEGINNER")))
                .andExpect(status().isCreated());

        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), "ADVANCED")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // --- missing profile ---

    @Test
    void createWithoutExistingProfileReturnsNotFound() throws Exception {
        Skill skill = createSkill("Java");

        mockMvc.perform(as(post(SKILLS_URL), 42L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), "BEGINNER")))
                .andExpect(status().isNotFound());
    }

    // --- missing skill ---

    @Test
    void createWithNonexistentSkillReturnsNotFound() throws Exception {
        createProfile(1L);

        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(999999L, "BEGINNER")))
                .andExpect(status().isNotFound());
    }

    // --- inactive skill cannot be assigned ---

    @Test
    void createWithInactiveSkillReturnsNotFound() throws Exception {
        createProfile(1L);
        Skill skill = deactivate(createSkill("Cobol"));

        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), "BEGINNER")))
                .andExpect(status().isNotFound());
    }

    // --- list empty ---

    @Test
    void listWithNoAssignmentsReturnsEmptyArray() throws Exception {
        createProfile(1L);

        mockMvc.perform(as(get(SKILLS_URL), 1L, USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- list active and inactive assignments ---

    @Test
    void listIncludesActiveAndInactiveAssignedSkills() throws Exception {
        createProfile(1L);
        Skill activeSkill = createSkill("Java");
        Skill inactiveSkill = createSkill("Cobol");

        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(activeSkill.getId(), "BEGINNER")))
                .andExpect(status().isCreated());
        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(inactiveSkill.getId(), "EXPERT")))
                .andExpect(status().isCreated());

        deactivate(inactiveSkill);

        mockMvc.perform(as(get(SKILLS_URL), 1L, USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].skillName").value("Cobol"))
                .andExpect(jsonPath("$[0].skillActive").value(false))
                .andExpect(jsonPath("$[1].skillName").value("Java"))
                .andExpect(jsonPath("$[1].skillActive").value(true));
    }

    // --- alphabetical ordering ---

    @Test
    void listSortsByNormalizedNameAscending() throws Exception {
        createProfile(1L);
        Skill zookeeper = createSkill("Zookeeper");
        Skill java = createSkill("Java");
        Skill docker = createSkill("Docker");

        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(zookeeper.getId(), "BEGINNER"))).andExpect(status().isCreated());
        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(java.getId(), "BEGINNER"))).andExpect(status().isCreated());
        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(docker.getId(), "BEGINNER"))).andExpect(status().isCreated());

        mockMvc.perform(as(get(SKILLS_URL), 1L, USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skillName").value("Docker"))
                .andExpect(jsonPath("$[1].skillName").value("Java"))
                .andExpect(jsonPath("$[2].skillName").value("Zookeeper"));
    }

    // --- successful proficiency update ---

    @Test
    void updateProficiencyReturnsOk() throws Exception {
        createProfile(1L);
        Skill skill = createSkill("Java");
        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), "BEGINNER")))
                .andExpect(status().isCreated());

        mockMvc.perform(as(put(SKILLS_URL + "/" + skill.getId()), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("EXPERT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proficiencyLevel").value("EXPERT"));
    }

    // --- update absent assignment ---

    @Test
    void updateAbsentAssignmentReturnsNotFound() throws Exception {
        createProfile(1L);
        Skill skill = createSkill("Java");

        mockMvc.perform(as(put(SKILLS_URL + "/" + skill.getId()), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("EXPERT")))
                .andExpect(status().isNotFound());
    }

    // --- update inactive skill ---

    @Test
    void updateWhenSkillInactiveReturnsNotFound() throws Exception {
        createProfile(1L);
        Skill skill = createSkill("Java");
        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), "BEGINNER")))
                .andExpect(status().isCreated());

        deactivate(skill);

        mockMvc.perform(as(put(SKILLS_URL + "/" + skill.getId()), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("EXPERT")))
                .andExpect(status().isNotFound());
    }

    // --- successful removal ---

    @Test
    void deleteAssignmentReturnsNoContent() throws Exception {
        createProfile(1L);
        Skill skill = createSkill("Java");
        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), "BEGINNER")))
                .andExpect(status().isCreated());

        mockMvc.perform(as(delete(SKILLS_URL + "/" + skill.getId()), 1L, USER))
                .andExpect(status().isNoContent());
    }

    // --- removal of an inactive assigned skill ---

    @Test
    void deleteAssignmentToInactiveSkillReturnsNoContent() throws Exception {
        createProfile(1L);
        Skill skill = createSkill("Java");
        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), "BEGINNER")))
                .andExpect(status().isCreated());

        deactivate(skill);

        mockMvc.perform(as(delete(SKILLS_URL + "/" + skill.getId()), 1L, USER))
                .andExpect(status().isNoContent());
    }

    // --- removal of absent assignment ---

    @Test
    void deleteAbsentAssignmentReturnsNotFound() throws Exception {
        createProfile(1L);
        Skill skill = createSkill("Java");

        mockMvc.perform(as(delete(SKILLS_URL + "/" + skill.getId()), 1L, USER))
                .andExpect(status().isNotFound());
    }

    // --- every proficiency enum value ---

    @Test
    void createWithBeginnerProficiencySucceeds() throws Exception {
        createProfile(1L);
        Skill skill = createSkill("Java");
        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), "BEGINNER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proficiencyLevel").value("BEGINNER"));
    }

    @Test
    void createWithIntermediateProficiencySucceeds() throws Exception {
        createProfile(1L);
        Skill skill = createSkill("Java");
        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), "INTERMEDIATE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proficiencyLevel").value("INTERMEDIATE"));
    }

    @Test
    void createWithAdvancedProficiencySucceeds() throws Exception {
        createProfile(1L);
        Skill skill = createSkill("Java");
        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), "ADVANCED")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proficiencyLevel").value("ADVANCED"));
    }

    @Test
    void createWithExpertProficiencySucceeds() throws Exception {
        createProfile(1L);
        Skill skill = createSkill("Java");
        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), "EXPERT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proficiencyLevel").value("EXPERT"));
    }

    // --- invalid or missing proficiency ---

    @Test
    void createWithInvalidProficiencyReturnsBadRequest() throws Exception {
        createProfile(1L);
        Skill skill = createSkill("Java");

        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), "MASTER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithMissingProficiencyReturnsBadRequest() throws Exception {
        createProfile(1L);
        Skill skill = createSkill("Java");

        mockMvc.perform(as(post(SKILLS_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), null)))
                .andExpect(status().isBadRequest());
    }

    // --- missing/malformed/zero/negative user ID ---

    @Test
    void createWithoutUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(SKILLS_URL).header(ROLE_HEADER, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "BEGINNER")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void createWithMalformedUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(SKILLS_URL).header(ID_HEADER, "not-a-number").header(ROLE_HEADER, USER)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(1L, "BEGINNER")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithZeroUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(SKILLS_URL).header(ID_HEADER, "0").header(ROLE_HEADER, USER)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(1L, "BEGINNER")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithNegativeUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(SKILLS_URL).header(ID_HEADER, "-3").header(ROLE_HEADER, USER)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(1L, "BEGINNER")))
                .andExpect(status().isUnauthorized());
    }

    // --- missing/unsupported role ---

    @Test
    void createWithoutRoleHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(SKILLS_URL).header(ID_HEADER, "1").contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "BEGINNER")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithUnsupportedRoleReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(SKILLS_URL).header(ID_HEADER, "1").header(ROLE_HEADER, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(1L, "BEGINNER")))
                .andExpect(status().isUnauthorized());
    }

    // --- USER self-management ---

    @Test
    void userRoleCanManageOwnAssignments() throws Exception {
        createProfile(5L);
        Skill skill = createSkill("Java");

        String response = mockMvc.perform(as(post(SKILLS_URL), 5L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), "BEGINNER")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(as(get(SKILLS_URL), 5L, USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(as(put(SKILLS_URL + "/" + skill.getId()), 5L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("ADVANCED")))
                .andExpect(status().isOk());

        mockMvc.perform(as(delete(SKILLS_URL + "/" + skill.getId()), 5L, USER))
                .andExpect(status().isNoContent());
    }

    // --- PROJECT_MANAGER self-management ---

    @Test
    void projectManagerRoleCanManageOwnAssignments() throws Exception {
        createProfile(6L);
        Skill skill = createSkill("Java");

        mockMvc.perform(as(post(SKILLS_URL), 6L, MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(skill.getId(), "BEGINNER")))
                .andExpect(status().isCreated());

        mockMvc.perform(as(get(SKILLS_URL), 6L, MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(as(put(SKILLS_URL + "/" + skill.getId()), 6L, MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("ADVANCED")))
                .andExpect(status().isOk());

        mockMvc.perform(as(delete(SKILLS_URL + "/" + skill.getId()), 6L, MANAGER))
                .andExpect(status().isNoContent());
    }
}
