package com.skillteam.userskill;

import com.skillteam.userskill.entity.UserProfile;
import com.skillteam.userskill.repository.UserAvailabilityRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAvailabilityControllerTest {

    private static final String AVAILABILITY_URL = "/api/v1/users/me/availability";
    private static final String ID_HEADER = IdentityHeaderResolver.USER_ID_HEADER;
    private static final String ROLE_HEADER = IdentityHeaderResolver.USER_ROLE_HEADER;
    private static final String MANAGER = "PROJECT_MANAGER";
    private static final String USER = "USER";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAvailabilityRepository userAvailabilityRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @AfterEach
    void cleanUp() {
        userAvailabilityRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    private UserProfile createProfile(long authUserId) {
        return userProfileRepository.saveAndFlush(new UserProfile(authUserId, "Test User " + authUserId, null, null));
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder builder, long authUserId, String role) {
        return builder.header(ID_HEADER, String.valueOf(authUserId)).header(ROLE_HEADER, role);
    }

    private String body(Integer hours) {
        return "{\"availableHoursPerWeek\":" + (hours == null ? "null" : hours) + "}";
    }

    // --- successful creation ---

    @Test
    void createReturnsCreated() throws Exception {
        createProfile(1L);

        mockMvc.perform(as(post(AVAILABILITY_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(40)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.availableHoursPerWeek").value(40))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.userProfileId").doesNotExist());
    }

    // --- duplicate creation ---

    @Test
    void createDuplicateReturnsConflict() throws Exception {
        createProfile(1L);

        mockMvc.perform(as(post(AVAILABILITY_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(40)))
                .andExpect(status().isCreated());

        mockMvc.perform(as(post(AVAILABILITY_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(20)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // --- missing profile on creation ---

    @Test
    void createWithoutProfileReturnsNotFound() throws Exception {
        mockMvc.perform(as(post(AVAILABILITY_URL), 99L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(40)))
                .andExpect(status().isNotFound());
    }

    // --- successful read ---

    @Test
    void getReturnsOk() throws Exception {
        createProfile(1L);
        mockMvc.perform(as(post(AVAILABILITY_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(40)))
                .andExpect(status().isCreated());

        mockMvc.perform(as(get(AVAILABILITY_URL), 1L, USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableHoursPerWeek").value(40));
    }

    // --- read when absent ---

    @Test
    void getAbsentReturnsNotFound() throws Exception {
        createProfile(1L);

        mockMvc.perform(as(get(AVAILABILITY_URL), 1L, USER))
                .andExpect(status().isNotFound());
    }

    // --- successful update ---

    @Test
    void updateReturnsOk() throws Exception {
        createProfile(1L);
        mockMvc.perform(as(post(AVAILABILITY_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(40)))
                .andExpect(status().isCreated());

        mockMvc.perform(as(put(AVAILABILITY_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableHoursPerWeek").value(10));
    }

    // --- update when absent / no implicit create on PUT ---

    @Test
    void updateAbsentReturnsNotFoundAndDoesNotImplicitlyCreate() throws Exception {
        createProfile(1L);

        mockMvc.perform(as(put(AVAILABILITY_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(10)))
                .andExpect(status().isNotFound());

        mockMvc.perform(as(get(AVAILABILITY_URL), 1L, USER))
                .andExpect(status().isNotFound());
    }

    // --- update with missing profile ---

    @Test
    void updateWithoutProfileReturnsNotFound() throws Exception {
        mockMvc.perform(as(put(AVAILABILITY_URL), 99L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(10)))
                .andExpect(status().isNotFound());
    }

    // --- zero hours accepted ---

    @Test
    void createWithZeroHoursSucceeds() throws Exception {
        createProfile(1L);

        mockMvc.perform(as(post(AVAILABILITY_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(0)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availableHoursPerWeek").value(0));
    }

    // --- 168 hours accepted ---

    @Test
    void createWithMaxHoursSucceeds() throws Exception {
        createProfile(1L);

        mockMvc.perform(as(post(AVAILABILITY_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(168)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availableHoursPerWeek").value(168));
    }

    // --- negative value rejected ---

    @Test
    void createWithNegativeHoursReturnsBadRequest() throws Exception {
        createProfile(1L);

        mockMvc.perform(as(post(AVAILABILITY_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(-1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // --- value greater than 168 rejected ---

    @Test
    void createWithHoursAboveMaxReturnsBadRequest() throws Exception {
        createProfile(1L);

        mockMvc.perform(as(post(AVAILABILITY_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(169)))
                .andExpect(status().isBadRequest());
    }

    // --- missing value rejected ---

    @Test
    void createWithMissingHoursReturnsBadRequest() throws Exception {
        createProfile(1L);

        mockMvc.perform(as(post(AVAILABILITY_URL), 1L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(null)))
                .andExpect(status().isBadRequest());
    }

    // --- missing/malformed/zero/negative user ID ---

    @Test
    void createWithoutUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(AVAILABILITY_URL).header(ROLE_HEADER, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(40)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void createWithMalformedUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(AVAILABILITY_URL).header(ID_HEADER, "not-a-number").header(ROLE_HEADER, USER)
                        .contentType(MediaType.APPLICATION_JSON).content(body(40)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithZeroUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(AVAILABILITY_URL).header(ID_HEADER, "0").header(ROLE_HEADER, USER)
                        .contentType(MediaType.APPLICATION_JSON).content(body(40)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithNegativeUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(AVAILABILITY_URL).header(ID_HEADER, "-3").header(ROLE_HEADER, USER)
                        .contentType(MediaType.APPLICATION_JSON).content(body(40)))
                .andExpect(status().isUnauthorized());
    }

    // --- missing/unsupported role ---

    @Test
    void createWithoutRoleHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(AVAILABILITY_URL).header(ID_HEADER, "1").contentType(MediaType.APPLICATION_JSON)
                        .content(body(40)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithUnsupportedRoleReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(AVAILABILITY_URL).header(ID_HEADER, "1").header(ROLE_HEADER, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(body(40)))
                .andExpect(status().isUnauthorized());
    }

    // --- USER self-management ---

    @Test
    void userRoleCanManageOwnAvailability() throws Exception {
        createProfile(5L);

        mockMvc.perform(as(post(AVAILABILITY_URL), 5L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(30)))
                .andExpect(status().isCreated());

        mockMvc.perform(as(get(AVAILABILITY_URL), 5L, USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableHoursPerWeek").value(30));

        mockMvc.perform(as(put(AVAILABILITY_URL), 5L, USER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(50)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableHoursPerWeek").value(50));
    }

    // --- PROJECT_MANAGER self-management ---

    @Test
    void projectManagerRoleCanManageOwnAvailability() throws Exception {
        createProfile(6L);

        mockMvc.perform(as(post(AVAILABILITY_URL), 6L, MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(30)))
                .andExpect(status().isCreated());

        mockMvc.perform(as(get(AVAILABILITY_URL), 6L, MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableHoursPerWeek").value(30));

        mockMvc.perform(as(put(AVAILABILITY_URL), 6L, MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(body(50)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableHoursPerWeek").value(50));
    }
}
