package com.skillteam.projectteam;

import com.skillteam.projectteam.repository.ProjectRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerTest {

    private static final String PROJECTS_URL = "/api/v1/projects";
    private static final String ID_HEADER = IdentityHeaderResolver.USER_ID_HEADER;
    private static final String ROLE_HEADER = IdentityHeaderResolver.USER_ROLE_HEADER;
    private static final String MANAGER = "PROJECT_MANAGER";
    private static final String USER = "USER";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @AfterEach
    void cleanUp() {
        projectRepository.deleteAll();
    }

    private String createBody(String name, String description) {
        return "{\"name\":" + quoteOrNull(name) + ",\"description\":" + quoteOrNull(description) + "}";
    }

    private String updateBody(String name, String description, String status) {
        return "{\"name\":" + quoteOrNull(name) + ",\"description\":" + quoteOrNull(description)
                + ",\"status\":" + quoteOrNull(status) + "}";
    }

    private String quoteOrNull(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private MockHttpServletRequestBuilder asManager(MockHttpServletRequestBuilder builder) {
        return builder.header(ID_HEADER, "1").header(ROLE_HEADER, MANAGER);
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder) {
        return builder.header(ID_HEADER, "2").header(ROLE_HEADER, USER);
    }

    // --- create success ---

    @Test
    void createWithValidRequestReturnsCreated() throws Exception {
        mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Apollo", "Moon mission")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Apollo"))
                .andExpect(jsonPath("$.description").value("Moon mission"))
                .andExpect(jsonPath("$.status").value("PLANNING"))
                .andExpect(jsonPath("$.ownerAuthUserId").value(1))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.normalizedName").doesNotExist());
    }

    // --- case-insensitive duplicate create ---

    @Test
    void createWithCaseInsensitiveDuplicateNameReturnsConflict() throws Exception {
        mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Apollo", null)))
                .andExpect(status().isCreated());

        mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("APOLLO", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value(PROJECTS_URL));
    }

    // --- list in alphabetical order ---

    @Test
    void listReturnsProjectsSortedAlphabeticallyByNormalizedName() throws Exception {
        mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Zeta", null))).andExpect(status().isCreated());
        mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Apollo", null))).andExpect(status().isCreated());
        mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Mercury", null))).andExpect(status().isCreated());

        mockMvc.perform(asManager(get(PROJECTS_URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Apollo"))
                .andExpect(jsonPath("$[1].name").value("Mercury"))
                .andExpect(jsonPath("$[2].name").value("Zeta"));
    }

    // --- get success ---

    @Test
    void getExistingProjectReturnsOk() throws Exception {
        String response = mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Gemini", "Two-person spacecraft")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(get(PROJECTS_URL + "/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gemini"))
                .andExpect(jsonPath("$.description").value("Two-person spacecraft"));
    }

    // --- get absent ---

    @Test
    void getMissingProjectReturnsNotFound() throws Exception {
        mockMvc.perform(asManager(get(PROJECTS_URL + "/999999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- update success ---

    @Test
    void updateExistingProjectReturnsOk() throws Exception {
        String response = mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Old Name", "Old description")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(put(PROJECTS_URL + "/" + id)).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("New Name", "New description", "ACTIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.description").value("New description"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // --- update absent ---

    @Test
    void updateMissingProjectReturnsNotFound() throws Exception {
        mockMvc.perform(asManager(put(PROJECTS_URL + "/999999")).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("Anything", null, "ACTIVE")))
                .andExpect(status().isNotFound());
    }

    // --- update duplicate ---

    @Test
    void updateToAnotherProjectsNameReturnsConflict() throws Exception {
        mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Voyager", null))).andExpect(status().isCreated());

        String response = mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Pioneer", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long pioneerId = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(put(PROJECTS_URL + "/" + pioneerId)).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("VOYAGER", null, "ACTIVE")))
                .andExpect(status().isConflict());
    }

    // --- delete success ---

    @Test
    void deleteExistingProjectReturnsNoContent() throws Exception {
        String response = mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Artemis", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(delete(PROJECTS_URL + "/" + id)))
                .andExpect(status().isNoContent());
    }

    // --- deleted project hidden from list and GET ---

    @Test
    void deletedProjectIsHiddenFromListAndGet() throws Exception {
        String response = mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Orion", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(delete(PROJECTS_URL + "/" + id))).andExpect(status().isNoContent());

        mockMvc.perform(asManager(get(PROJECTS_URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(asManager(get(PROJECTS_URL + "/" + id)))
                .andExpect(status().isNotFound());
    }

    // --- repeated delete returns 404 ---

    @Test
    void repeatedDeleteReturnsNotFound() throws Exception {
        String response = mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Skylab", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(delete(PROJECTS_URL + "/" + id))).andExpect(status().isNoContent());

        mockMvc.perform(asManager(delete(PROJECTS_URL + "/" + id)))
                .andExpect(status().isNotFound());
    }

    // --- validation failures ---

    @Test
    void createWithBlankNameReturnsBadRequest() throws Exception {
        mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("   ", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createWithTooShortNameReturnsBadRequest() throws Exception {
        mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("A", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithTooLongNameReturnsBadRequest() throws Exception {
        String tooLong = "A".repeat(151);
        mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(tooLong, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithTooLongDescriptionReturnsBadRequest() throws Exception {
        String tooLong = "A".repeat(501);
        mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Valid Name", tooLong)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWithMissingStatusReturnsBadRequest() throws Exception {
        String response = mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Hubble", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(put(PROJECTS_URL + "/" + id)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hubble\",\"description\":null,\"status\":null}"))
                .andExpect(status().isBadRequest());
    }

    // --- missing/malformed/zero/negative user ID ---

    @Test
    void createWithoutUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(PROJECTS_URL).header(ROLE_HEADER, MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("No Id", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void createWithMalformedUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(PROJECTS_URL).header(ID_HEADER, "not-a-number").header(ROLE_HEADER, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("Malformed Id", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithZeroUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(PROJECTS_URL).header(ID_HEADER, "0").header(ROLE_HEADER, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("Zero Id", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithNegativeUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(PROJECTS_URL).header(ID_HEADER, "-3").header(ROLE_HEADER, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("Negative Id", null)))
                .andExpect(status().isUnauthorized());
    }

    // --- missing/unsupported role ---

    @Test
    void createWithoutRoleHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(PROJECTS_URL).header(ID_HEADER, "1").contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("No Role", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void createWithUnsupportedRoleReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(PROJECTS_URL).header(ID_HEADER, "1").header(ROLE_HEADER, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("Bad Role", null)))
                .andExpect(status().isUnauthorized());
    }

    // --- USER can read ---

    @Test
    void userRoleCanListProjects() throws Exception {
        mockMvc.perform(asUser(get(PROJECTS_URL)))
                .andExpect(status().isOk());
    }

    @Test
    void userRoleCanGetProjectById() throws Exception {
        String response = mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Cassini", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asUser(get(PROJECTS_URL + "/" + id)))
                .andExpect(status().isOk());
    }

    // --- USER receives 403 for POST, PUT and DELETE ---

    @Test
    void userRoleCannotCreateProject() throws Exception {
        mockMvc.perform(asUser(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Forbidden", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access is denied."));
    }

    @Test
    void userRoleCannotUpdateProject() throws Exception {
        String response = mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Juno", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asUser(put(PROJECTS_URL + "/" + id)).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("Juno II", null, "ACTIVE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void userRoleCannotDeleteProject() throws Exception {
        String response = mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Galileo", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asUser(delete(PROJECTS_URL + "/" + id)))
                .andExpect(status().isForbidden());
    }

    // --- PROJECT_MANAGER can perform write operations ---

    @Test
    void projectManagerCanCreateUpdateAndDeleteProject() throws Exception {
        String response = mockMvc.perform(asManager(post(PROJECTS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("New Horizons", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(put(PROJECTS_URL + "/" + id)).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("New Horizons II", null, "COMPLETED")))
                .andExpect(status().isOk());

        mockMvc.perform(asManager(delete(PROJECTS_URL + "/" + id)))
                .andExpect(status().isNoContent());
    }
}
