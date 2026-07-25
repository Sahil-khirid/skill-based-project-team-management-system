package com.skillteam.auth;

import com.skillteam.auth.entity.AuthUser;
import com.skillteam.auth.entity.Role;
import com.skillteam.auth.repository.AuthUserRepository;
import com.skillteam.auth.repository.RefreshTokenRepository;
import com.skillteam.auth.security.RefreshTokenHasher;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRefreshTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String RAW_PASSWORD = "Password@123";
    private static final String GENERIC_MESSAGE = "Invalid or expired refresh token.";
    private static final String TEST_SECRET_BASE64 =
            "dGVzdC1vbmx5LWp3dC1zaWduaW5nLXNlY3JldC1mb3ItYXV0aC1zZXJ2aWNlLXVuaXQtdGVzdHMtMzJieXRlcw==";

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

    private AuthUser createUser(String email, boolean enabled) {
        AuthUser user = new AuthUser(email, passwordEncoder.encode(RAW_PASSWORD), Role.USER, enabled);
        return authUserRepository.saveAndFlush(user);
    }

    private Map<String, Object> login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return Map.of(
                "accessToken", com.jayway.jsonpath.JsonPath.read(body, "$.accessToken"),
                "refreshToken", com.jayway.jsonpath.JsonPath.read(body, "$.refreshToken"));
    }

    private String refreshBody(String refreshToken) {
        return "{\"refreshToken\":\"" + refreshToken + "\"}";
    }

    private String tokenHashOf(String rawToken) {
        return refreshTokenHasher.hash(rawToken);
    }

    @Test
    void validTokenReturnsNewAccessTokenAndNewRefreshToken() throws Exception {
        createUser("refresh@example.com", true);
        String oldRefreshToken = (String) login("refresh@example.com").get("refreshToken");

        MvcResult result = mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(oldRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        String newRefreshToken = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.refreshToken");

        assertThat(newRefreshToken).isNotBlank();
        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);
    }

    @Test
    void oldTokenBecomesRevokedAfterRefresh() throws Exception {
        createUser("revokeafter@example.com", true);
        String oldRefreshToken = (String) login("revokeafter@example.com").get("refreshToken");

        mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(oldRefreshToken)))
                .andExpect(status().isOk());

        Instant revokedAt = jdbcTemplate.queryForObject(
                "select revoked_at from refresh_tokens where token_hash = ?",
                Instant.class, tokenHashOf(oldRefreshToken));

        assertThat(revokedAt).isNotNull();
    }

    @Test
    void oldTokenReferencesReplacementToken() throws Exception {
        createUser("replaced@example.com", true);
        String oldRefreshToken = (String) login("replaced@example.com").get("refreshToken");

        MvcResult result = mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(oldRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        String newRefreshToken = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.refreshToken");

        Long replacedByTokenId = jdbcTemplate.queryForObject(
                "select replaced_by_token_id from refresh_tokens where token_hash = ?",
                Long.class, tokenHashOf(oldRefreshToken));
        Long newTokenId = jdbcTemplate.queryForObject(
                "select id from refresh_tokens where token_hash = ?",
                Long.class, tokenHashOf(newRefreshToken));

        assertThat(replacedByTokenId).isEqualTo(newTokenId);
    }

    @Test
    void oldTokenCannotBeReusedAfterRotation() throws Exception {
        createUser("reuse@example.com", true);
        String oldRefreshToken = (String) login("reuse@example.com").get("refreshToken");

        mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(oldRefreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(oldRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE));
    }

    @Test
    void newTokenCanRefreshNormallyAfterRotation() throws Exception {
        createUser("rotateagain@example.com", true);
        String oldRefreshToken = (String) login("rotateagain@example.com").get("refreshToken");

        MvcResult firstRotation = mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(oldRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        String rotatedRefreshToken = com.jayway.jsonpath.JsonPath.read(
                firstRotation.getResponse().getContentAsString(), "$.refreshToken");

        mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(rotatedRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void unknownTokenIsRejected() throws Exception {
        mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody("completely-unknown-refresh-token-value")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        createUser("expiredrefresh@example.com", true);
        String rawRefreshToken = (String) login("expiredrefresh@example.com").get("refreshToken");

        jdbcTemplate.update("update refresh_tokens set expires_at = ? where token_hash = ?",
                Instant.now().minus(1, ChronoUnit.DAYS), tokenHashOf(rawRefreshToken));

        mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(rawRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE));
    }

    @Test
    void revokedTokenIsRejected() throws Exception {
        createUser("revokedrefresh@example.com", true);
        String rawRefreshToken = (String) login("revokedrefresh@example.com").get("refreshToken");

        jdbcTemplate.update("update refresh_tokens set revoked_at = ? where token_hash = ?",
                Instant.now(), tokenHashOf(rawRefreshToken));

        mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(rawRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE));
    }

    @Test
    void blankTokenReturnsBadRequest() throws Exception {
        mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody("")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedTokenIsRejectedWithGenericMessage() throws Exception {
        mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody("not-a-real-token-@@@###")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE));
    }

    @Test
    void disabledUserTokenIsRejected() throws Exception {
        AuthUser user = createUser("disabledrefresh@example.com", true);
        String rawRefreshToken = (String) login("disabledrefresh@example.com").get("refreshToken");

        jdbcTemplate.update("update auth_users set enabled = false where id = ?", user.getId());

        mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(rawRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE));
    }

    @Test
    void newAccessTokenContainsExpectedClaims() throws Exception {
        AuthUser user = createUser("refreshclaims@example.com", true);
        String oldRefreshToken = (String) login("refreshclaims@example.com").get("refreshToken");

        MvcResult result = mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(oldRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        String newAccessToken = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.accessToken");

        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET_BASE64));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(newAccessToken).getPayload();

        assertThat(claims.getSubject()).isEqualTo(String.valueOf(user.getId()));
        assertThat(claims.get("email", String.class)).isEqualTo("refreshclaims@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getIssuer()).isEqualTo("skillteam-auth-service");
        assertThat(claims.getAudience()).contains("skillteam-api");
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    void databaseContainsNoRawRefreshTokenAfterRotation() throws Exception {
        createUser("norawrotate@example.com", true);
        String oldRefreshToken = (String) login("norawrotate@example.com").get("refreshToken");

        MvcResult result = mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(oldRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        String newRefreshToken = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.refreshToken");

        Integer oldRawMatches = jdbcTemplate.queryForObject(
                "select count(*) from refresh_tokens where token_hash = ?", Integer.class, oldRefreshToken);
        Integer newRawMatches = jdbcTemplate.queryForObject(
                "select count(*) from refresh_tokens where token_hash = ?", Integer.class, newRefreshToken);

        assertThat(oldRawMatches).isZero();
        assertThat(newRawMatches).isZero();
    }
}
