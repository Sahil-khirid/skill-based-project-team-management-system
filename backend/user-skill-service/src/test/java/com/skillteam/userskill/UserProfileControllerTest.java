package com.skillteam.userskill;

import com.skillteam.userskill.repository.UserProfileRepository;
import com.skillteam.userskill.security.IdentityHeaderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileControllerTest {

    private static final String ME_URL = "/api/v1/users/me";
    private static final String HEADER = IdentityHeaderResolver.USER_ID_HEADER;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @AfterEach
    void cleanUp() {
        userProfileRepository.deleteAll();
    }

    private String body(String displayName, String headline, String bio) {
        return "{\"displayName\":" + quoteOrNull(displayName)
                + ",\"headline\":" + quoteOrNull(headline)
                + ",\"bio\":" + quoteOrNull(bio) + "}";
    }

    private String quoteOrNull(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    // --- successful creation ---

    @Test
    void createWithValidRequestReturnsCreated() throws Exception {
        mockMvc.perform(post(ME_URL)
                        .header(HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Ada Lovelace", "Engineer", "Loves algorithms")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.authUserId").value(1))
                .andExpect(jsonPath("$.displayName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.headline").value("Engineer"))
                .andExpect(jsonPath("$.bio").value("Loves algorithms"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void createTrimsWhitespaceAndConvertsBlankOptionalFieldsToNull() throws Exception {
        mockMvc.perform(post(ME_URL)
                        .header(HEADER, "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("  Grace Hopper  ", "   ", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value("Grace Hopper"))
                .andExpect(jsonPath("$.headline").doesNotExist())
                .andExpect(jsonPath("$.bio").doesNotExist());
    }

    // --- duplicate creation ---

    @Test
    void createWhenProfileAlreadyExistsReturnsConflict() throws Exception {
        mockMvc.perform(post(ME_URL).header(HEADER, "3").contentType(MediaType.APPLICATION_JSON)
                        .content(body("First", null, null)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(ME_URL).header(HEADER, "3").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Second", null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value(ME_URL));
    }

    // --- successful retrieval ---

    @Test
    void getExistingProfileReturnsOk() throws Exception {
        mockMvc.perform(post(ME_URL).header(HEADER, "4").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Linus Torvalds", "Kernel dev", null)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(ME_URL).header(HEADER, "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authUserId").value(4))
                .andExpect(jsonPath("$.displayName").value("Linus Torvalds"))
                .andExpect(jsonPath("$.headline").value("Kernel dev"));
    }

    // --- missing profile ---

    @Test
    void getMissingProfileReturnsNotFound() throws Exception {
        mockMvc.perform(get(ME_URL).header(HEADER, "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value(ME_URL));
    }

    // --- successful update ---

    @Test
    void updateExistingProfileReturnsOk() throws Exception {
        mockMvc.perform(post(ME_URL).header(HEADER, "5").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Old Name", "Old headline", "Old bio")))
                .andExpect(status().isCreated());

        mockMvc.perform(put(ME_URL).header(HEADER, "5").contentType(MediaType.APPLICATION_JSON)
                        .content(body("New Name", "New headline", "New bio")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("New Name"))
                .andExpect(jsonPath("$.headline").value("New headline"))
                .andExpect(jsonPath("$.bio").value("New bio"));
    }

    // --- update when absent ---

    @Test
    void updateWhenProfileAbsentReturnsNotFoundAndDoesNotCreate() throws Exception {
        mockMvc.perform(put(ME_URL).header(HEADER, "6").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Nobody", null, null)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(ME_URL).header(HEADER, "6"))
                .andExpect(status().isNotFound());
    }

    // --- validation failures ---

    @Test
    void createWithBlankDisplayNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post(ME_URL).header(HEADER, "7").contentType(MediaType.APPLICATION_JSON)
                        .content(body("   ", null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createWithTooShortDisplayNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post(ME_URL).header(HEADER, "7").contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", null, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithTooLongDisplayNameReturnsBadRequest() throws Exception {
        String tooLong = "A".repeat(101);
        mockMvc.perform(post(ME_URL).header(HEADER, "7").contentType(MediaType.APPLICATION_JSON)
                        .content(body(tooLong, null, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithTooLongHeadlineReturnsBadRequest() throws Exception {
        String tooLong = "A".repeat(161);
        mockMvc.perform(post(ME_URL).header(HEADER, "7").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Valid Name", tooLong, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithTooLongBioReturnsBadRequest() throws Exception {
        String tooLong = "A".repeat(501);
        mockMvc.perform(post(ME_URL).header(HEADER, "7").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Valid Name", null, tooLong)))
                .andExpect(status().isBadRequest());
    }

    // --- missing identity header ---

    @Test
    void createWithoutIdentityHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(ME_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(body("No Header", null, null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    // --- malformed identity header ---

    @Test
    void createWithMalformedIdentityHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(ME_URL).header(HEADER, "not-a-number").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Malformed", null, null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    // --- zero or negative identity header ---

    @Test
    void createWithZeroIdentityHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(ME_URL).header(HEADER, "0").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Zero", null, null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void createWithNegativeIdentityHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(ME_URL).header(HEADER, "-5").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Negative", null, null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }
}
