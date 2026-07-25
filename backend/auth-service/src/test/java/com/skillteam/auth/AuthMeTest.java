package com.skillteam.auth;

import com.skillteam.auth.config.JwtProperties;
import com.skillteam.auth.entity.AuthUser;
import com.skillteam.auth.entity.Role;
import com.skillteam.auth.repository.AuthUserRepository;
import com.skillteam.auth.repository.RefreshTokenRepository;
import com.skillteam.auth.security.JwtService;
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthMeTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String ME_URL = "/api/v1/auth/me";
    private static final String RAW_PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        authUserRepository.deleteAll();
    }

    private AuthUser createUser(String email, boolean enabled) {
        AuthUser user = new AuthUser(email, passwordEncoder.encode(RAW_PASSWORD), Role.USER, enabled);
        return authUserRepository.saveAndFlush(user);
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    @Test
    void meWithoutTokenReturnsUnauthorizedJson() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication is required."))
                .andExpect(jsonPath("$.path").value(ME_URL));
    }

    @Test
    void meWithValidTokenReturnsOkWithCorrectIdentity() throws Exception {
        AuthUser user = createUser("me@example.com", true);
        String token = login("me@example.com");

        mockMvc.perform(get(ME_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().intValue()))
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void meResponseNeverContainsPasswordOrHash() throws Exception {
        createUser("nopassword@example.com", true);
        String token = login("nopassword@example.com");

        mockMvc.perform(get(ME_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void invalidSignatureTokenReturnsUnauthorized() throws Exception {
        createUser("sigcheck@example.com", true);
        String token = login("sigcheck@example.com");
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";

        mockMvc.perform(get(ME_URL).header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void malformedTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(ME_URL).header("Authorization", "Bearer not-a-jwt-at-all"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void expiredTokenReturnsUnauthorized() throws Exception {
        AuthUser user = createUser("expired@example.com", true);

        Clock pastClock = Clock.fixed(Instant.now().minus(Duration.ofMinutes(30)), ZoneOffset.UTC);
        JwtService pastJwtService = new JwtService(jwtProperties, pastClock);
        String expiredToken = pastJwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());

        mockMvc.perform(get(ME_URL).header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void tokenBelongingToSubsequentlyDisabledAccountReturnsUnauthorized() throws Exception {
        AuthUser user = createUser("laterdisabled@example.com", true);
        String token = login("laterdisabled@example.com");

        jdbcTemplate.update("update auth_users set enabled = false where id = ?", user.getId());

        mockMvc.perform(get(ME_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void noSessionCookieIsCreatedOnMe() throws Exception {
        createUser("nocookie@example.com", true);
        String token = login("nocookie@example.com");

        mockMvc.perform(get(ME_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }
}
