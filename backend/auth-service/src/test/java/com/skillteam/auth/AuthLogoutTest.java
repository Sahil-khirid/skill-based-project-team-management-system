package com.skillteam.auth;

import com.skillteam.auth.entity.AuthUser;
import com.skillteam.auth.entity.Role;
import com.skillteam.auth.repository.AuthUserRepository;
import com.skillteam.auth.repository.RefreshTokenRepository;
import com.skillteam.auth.security.RefreshTokenHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthLogoutTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String RAW_PASSWORD = "Password@123";
    private static final String GENERIC_MESSAGE = "Invalid or expired refresh token.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenHasher refreshTokenHasher;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        authUserRepository.deleteAll();
    }

    private void createUser(String email) {
        AuthUser user = new AuthUser(email, passwordEncoder.encode(RAW_PASSWORD), Role.USER, true);
        authUserRepository.saveAndFlush(user);
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.refreshToken");
    }

    private String tokenBody(String refreshToken) {
        return "{\"refreshToken\":\"" + refreshToken + "\"}";
    }

    @Test
    void activeRefreshTokenCanBeLoggedOut() throws Exception {
        createUser("logout@example.com");
        String refreshToken = login("logout@example.com");

        mockMvc.perform(post(LOGOUT_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody(refreshToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void tokenBecomesRevokedAfterLogout() throws Exception {
        createUser("revokedbylogout@example.com");
        String refreshToken = login("revokedbylogout@example.com");

        mockMvc.perform(post(LOGOUT_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody(refreshToken)))
                .andExpect(status().isNoContent());

        Instant revokedAt = jdbcTemplate.queryForObject(
                "select revoked_at from refresh_tokens where token_hash = ?",
                Instant.class, refreshTokenHasher.hash(refreshToken));

        assertThat(revokedAt).isNotNull();
    }

    @Test
    void refreshAfterLogoutFails() throws Exception {
        createUser("refreshafterlogout@example.com");
        String refreshToken = login("refreshafterlogout@example.com");

        mockMvc.perform(post(LOGOUT_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody(refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE));
    }

    @Test
    void repeatedLogoutOfAlreadyRevokedTokenDoesNotExposeExistence() throws Exception {
        createUser("repeatlogout@example.com");
        String refreshToken = login("repeatlogout@example.com");

        mockMvc.perform(post(LOGOUT_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody(refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(LOGOUT_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody(refreshToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void logoutOfUnknownTokenReturnsSameResponseAsRealToken() throws Exception {
        createUser("unknownlogout@example.com");
        String refreshToken = login("unknownlogout@example.com");

        MvcResult knownTokenResult = mockMvc.perform(post(LOGOUT_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody(refreshToken)))
                .andReturn();

        MvcResult unknownTokenResult = mockMvc.perform(post(LOGOUT_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody("this-token-was-never-issued-by-the-server")))
                .andReturn();

        assertThat(knownTokenResult.getResponse().getStatus())
                .isEqualTo(unknownTokenResult.getResponse().getStatus());
        assertThat(knownTokenResult.getResponse().getContentAsString())
                .isEqualTo(unknownTokenResult.getResponse().getContentAsString());
    }
}
