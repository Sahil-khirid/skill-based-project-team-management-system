package com.skillteam.userskill;

import com.skillteam.userskill.repository.SkillRepository;
import com.skillteam.userskill.security.IdentityHeaderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SkillControllerTest {

    private static final String SKILLS_URL = "/api/v1/skills";
    private static final String ID_HEADER = IdentityHeaderResolver.USER_ID_HEADER;
    private static final String ROLE_HEADER = IdentityHeaderResolver.USER_ROLE_HEADER;
    private static final String MANAGER = "PROJECT_MANAGER";
    private static final String USER = "USER";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SkillRepository skillRepository;

    @AfterEach
    void cleanUp() {
        skillRepository.deleteAll();
    }

    private String body(String name, String description) {
        return "{\"name\":" + quoteOrNull(name) + ",\"description\":" + quoteOrNull(description) + "}";
    }

    private String quoteOrNull(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder asManager(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header(ID_HEADER, "1").header(ROLE_HEADER, MANAGER);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder asUser(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header(ID_HEADER, "2").header(ROLE_HEADER, USER);
    }

    // --- create success ---

    @Test
    void createWithValidRequestReturnsCreated() throws Exception {
        mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Java", "JVM language")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Java"))
                .andExpect(jsonPath("$.description").value("JVM language"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.normalizedName").doesNotExist())
                .andExpect(jsonPath("$.active").doesNotExist());
    }

    // --- case-insensitive duplicate create ---

    @Test
    void createWithCaseInsensitiveDuplicateNameReturnsConflict() throws Exception {
        mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Java", null)))
                .andExpect(status().isCreated());

        mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("JAVA", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value(SKILLS_URL));
    }

    // --- normalized whitespace ---

    @Test
    void createCollapsesInternalWhitespaceAndTrimsOuterWhitespace() throws Exception {
        mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(" Spring   Boot ", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Spring Boot"));

        mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("spring boot", null)))
                .andExpect(status().isConflict());
    }

    // --- list active skills in alphabetical order ---

    @Test
    void listReturnsActiveSkillsSortedAlphabeticallyByNormalizedName() throws Exception {
        mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Zookeeper", null))).andExpect(status().isCreated());
        mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Java", null))).andExpect(status().isCreated());
        mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Docker", null))).andExpect(status().isCreated());

        mockMvc.perform(asManager(get(SKILLS_URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Docker"))
                .andExpect(jsonPath("$[1].name").value("Java"))
                .andExpect(jsonPath("$[2].name").value("Zookeeper"));
    }

    // --- get success ---

    @Test
    void getExistingSkillReturnsOk() throws Exception {
        String response = mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Kubernetes", "Container orchestration")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(get(SKILLS_URL + "/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kubernetes"))
                .andExpect(jsonPath("$.description").value("Container orchestration"));
    }

    // --- get absent ---

    @Test
    void getMissingSkillReturnsNotFound() throws Exception {
        mockMvc.perform(asManager(get(SKILLS_URL + "/999999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- update success ---

    @Test
    void updateExistingSkillReturnsOk() throws Exception {
        String response = mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Old Name", "Old description")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(put(SKILLS_URL + "/" + id)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("New Name", "New description")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.description").value("New description"));
    }

    // --- update absent ---

    @Test
    void updateMissingSkillReturnsNotFound() throws Exception {
        mockMvc.perform(asManager(put(SKILLS_URL + "/999999")).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Anything", null)))
                .andExpect(status().isNotFound());
    }

    // --- update duplicate ---

    @Test
    void updateToAnotherSkillsNameReturnsConflict() throws Exception {
        mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Python", null))).andExpect(status().isCreated());

        String response = mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Ruby", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long rubyId = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(put(SKILLS_URL + "/" + rubyId)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("PYTHON", null)))
                .andExpect(status().isConflict());
    }

    // --- delete success ---

    @Test
    void deleteExistingSkillReturnsNoContent() throws Exception {
        String response = mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Terraform", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(delete(SKILLS_URL + "/" + id)))
                .andExpect(status().isNoContent());
    }

    // --- deleted skill hidden from list and GET ---

    @Test
    void deletedSkillIsHiddenFromListAndGet() throws Exception {
        String response = mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Ansible", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(delete(SKILLS_URL + "/" + id))).andExpect(status().isNoContent());

        mockMvc.perform(asManager(get(SKILLS_URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(asManager(get(SKILLS_URL + "/" + id)))
                .andExpect(status().isNotFound());
    }

    // --- repeated delete returns 404 ---

    @Test
    void repeatedDeleteReturnsNotFound() throws Exception {
        String response = mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Vagrant", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(delete(SKILLS_URL + "/" + id))).andExpect(status().isNoContent());

        mockMvc.perform(asManager(delete(SKILLS_URL + "/" + id)))
                .andExpect(status().isNotFound());
    }

    // --- validation failures ---

    @Test
    void createWithBlankNameReturnsBadRequest() throws Exception {
        mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("   ", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createWithTooLongNameReturnsBadRequest() throws Exception {
        String tooLong = "A".repeat(101);
        mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(tooLong, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithTooLongDescriptionReturnsBadRequest() throws Exception {
        String tooLong = "A".repeat(501);
        mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Valid Name", tooLong)))
                .andExpect(status().isBadRequest());
    }

    // --- missing/malformed/zero/negative user ID ---

    @Test
    void createWithoutUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(SKILLS_URL).header(ROLE_HEADER, MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(body("No Id", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void createWithMalformedUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(SKILLS_URL).header(ID_HEADER, "not-a-number").header(ROLE_HEADER, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Malformed Id", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithZeroUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(SKILLS_URL).header(ID_HEADER, "0").header(ROLE_HEADER, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Zero Id", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithNegativeUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(SKILLS_URL).header(ID_HEADER, "-3").header(ROLE_HEADER, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Negative Id", null)))
                .andExpect(status().isUnauthorized());
    }

    // --- missing/unsupported role ---

    @Test
    void createWithoutRoleHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(SKILLS_URL).header(ID_HEADER, "1").contentType(MediaType.APPLICATION_JSON)
                        .content(body("No Role", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void createWithUnsupportedRoleReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(SKILLS_URL).header(ID_HEADER, "1").header(ROLE_HEADER, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(body("Bad Role", null)))
                .andExpect(status().isUnauthorized());
    }

    // --- USER can read ---

    @Test
    void userRoleCanListSkills() throws Exception {
        mockMvc.perform(asUser(get(SKILLS_URL)))
                .andExpect(status().isOk());
    }

    @Test
    void userRoleCanGetSkillById() throws Exception {
        String response = mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Go", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asUser(get(SKILLS_URL + "/" + id)))
                .andExpect(status().isOk());
    }

    // --- USER receives 403 for POST, PUT and DELETE ---

    @Test
    void userRoleCannotCreateSkill() throws Exception {
        mockMvc.perform(asUser(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Forbidden", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access is denied."));
    }

    @Test
    void userRoleCannotUpdateSkill() throws Exception {
        String response = mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Rust", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asUser(put(SKILLS_URL + "/" + id)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Rust Lang", null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userRoleCannotDeleteSkill() throws Exception {
        String response = mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Elixir", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asUser(delete(SKILLS_URL + "/" + id)))
                .andExpect(status().isForbidden());
    }

    // --- PROJECT_MANAGER can perform write operations ---

    @Test
    void projectManagerCanCreateUpdateAndDeleteSkill() throws Exception {
        String response = mockMvc.perform(asManager(post(SKILLS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Scala", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(put(SKILLS_URL + "/" + id)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Scala 3", null)))
                .andExpect(status().isOk());

        mockMvc.perform(asManager(delete(SKILLS_URL + "/" + id)))
                .andExpect(status().isNoContent());
    }
}
