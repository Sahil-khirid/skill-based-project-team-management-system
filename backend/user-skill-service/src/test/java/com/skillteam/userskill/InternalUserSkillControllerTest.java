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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalUserSkillControllerTest {

    private static final String OWN_SKILLS_URL = "/api/v1/users/me/skills";
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

    private String internalSkillsUrl(long targetAuthUserId) {
        return "/api/v1/users/" + targetAuthUserId + "/skills";
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder builder, long authUserId, String role) {
        return builder.header(ID_HEADER, String.valueOf(authUserId)).header(ROLE_HEADER, role);
    }

    private String assignBody(Long skillId, String proficiencyLevel) {
        return "{\"skillId\":" + skillId + ",\"proficiencyLevel\":\"" + proficiencyLevel + "\"}";
    }

    private void assignSkillToUser(long targetAuthUserId, Long skillId, String proficiencyLevel) throws Exception {
        mockMvc.perform(as(post(OWN_SKILLS_URL), targetAuthUserId, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(skillId, proficiencyLevel)))
                .andExpect(status().isCreated());
    }

    // --- successful lookup of another user's skills ---

    @Test
    void managerCanLookUpAnotherUsersSkills() throws Exception {
        createProfile(42L);
        Skill skill = createSkill("Java");
        assignSkillToUser(42L, skill.getId(), "BEGINNER");

        mockMvc.perform(as(get(internalSkillsUrl(42L)), 1L, MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].skillId").value(skill.getId().intValue()))
                .andExpect(jsonPath("$[0].skillName").value("Java"))
                .andExpect(jsonPath("$[0].proficiencyLevel").value("BEGINNER"))
                .andExpect(jsonPath("$[0].skillActive").value(true))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists());
    }

    // --- ordering matches the self-service endpoint ---

    @Test
    void listSortsByNormalizedNameAscending() throws Exception {
        createProfile(43L);
        Skill zookeeper = createSkill("Zookeeper");
        Skill java = createSkill("Java");
        Skill docker = createSkill("Docker");

        assignSkillToUser(43L, zookeeper.getId(), "BEGINNER");
        assignSkillToUser(43L, java.getId(), "BEGINNER");
        assignSkillToUser(43L, docker.getId(), "BEGINNER");

        mockMvc.perform(as(get(internalSkillsUrl(43L)), 1L, MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skillName").value("Docker"))
                .andExpect(jsonPath("$[1].skillName").value("Java"))
                .andExpect(jsonPath("$[2].skillName").value("Zookeeper"));
    }

    // --- includes both active and inactive assigned skills ---

    @Test
    void listIncludesActiveAndInactiveAssignedSkills() throws Exception {
        createProfile(44L);
        Skill activeSkill = createSkill("Java");
        Skill inactiveSkill = createSkill("Cobol");

        assignSkillToUser(44L, activeSkill.getId(), "BEGINNER");
        assignSkillToUser(44L, inactiveSkill.getId(), "EXPERT");

        deactivate(inactiveSkill);

        mockMvc.perform(as(get(internalSkillsUrl(44L)), 1L, MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].skillName").value("Cobol"))
                .andExpect(jsonPath("$[0].skillActive").value(false))
                .andExpect(jsonPath("$[1].skillName").value("Java"))
                .andExpect(jsonPath("$[1].skillActive").value(true));
    }

    // --- target user has no profile at all ---

    @Test
    void lookupForUserWithoutProfileReturnsEmptyArray() throws Exception {
        mockMvc.perform(as(get(internalSkillsUrl(999999L)), 1L, MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- target user has a profile but no assigned skills ---

    @Test
    void lookupForUserWithNoAssignedSkillsReturnsEmptyArray() throws Exception {
        createProfile(45L);

        mockMvc.perform(as(get(internalSkillsUrl(45L)), 1L, MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- caller looking up their own id via the internal path still works ---

    @Test
    void managerCanLookUpOwnSkillsViaInternalPath() throws Exception {
        createProfile(46L);
        Skill skill = createSkill("Kubernetes");
        assignSkillToUser(46L, skill.getId(), "ADVANCED");

        mockMvc.perform(as(get(internalSkillsUrl(46L)), 46L, MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].skillName").value("Kubernetes"));
    }

    // --- USER role is forbidden ---

    @Test
    void userRoleCannotLookUpAnotherUsersSkills() throws Exception {
        createProfile(47L);

        mockMvc.perform(as(get(internalSkillsUrl(47L)), 2L, USER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access is denied."));
    }

    // --- missing/malformed/zero/negative caller identity ---

    @Test
    void lookupWithoutUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(internalSkillsUrl(47L)).header(ROLE_HEADER, MANAGER))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void lookupWithMalformedUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(internalSkillsUrl(47L)).header(ID_HEADER, "not-a-number").header(ROLE_HEADER, MANAGER))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lookupWithZeroUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(internalSkillsUrl(47L)).header(ID_HEADER, "0").header(ROLE_HEADER, MANAGER))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lookupWithNegativeUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(internalSkillsUrl(47L)).header(ID_HEADER, "-3").header(ROLE_HEADER, MANAGER))
                .andExpect(status().isUnauthorized());
    }

    // --- missing/unsupported role header ---

    @Test
    void lookupWithoutRoleHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(internalSkillsUrl(47L)).header(ID_HEADER, "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void lookupWithUnsupportedRoleHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(internalSkillsUrl(47L)).header(ID_HEADER, "1").header(ROLE_HEADER, "ADMIN"))
                .andExpect(status().isUnauthorized());
    }
}
