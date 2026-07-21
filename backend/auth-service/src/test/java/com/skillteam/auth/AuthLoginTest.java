package com.skillteam.auth;

import com.skillteam.auth.entity.AuthUser;
import com.skillteam.auth.entity.Role;
import com.skillteam.auth.repository.AuthUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthLoginTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String RAW_PASSWORD = "Password@123";
    private static final String TEST_SECRET_BASE64 =
            "dGVzdC1vbmx5LWp3dC1zaWduaW5nLXNlY3JldC1mb3ItYXV0aC1zZXJ2aWNlLXVuaXQtdGVzdHMtMzJieXRlcw==";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanUp() {
        authUserRepository.deleteAll();
    }

    private AuthUser createUser(String email, boolean enabled) {
        AuthUser user = new AuthUser(email, passwordEncoder.encode(RAW_PASSWORD), Role.USER, enabled);
        return authUserRepository.saveAndFlush(user);
    }

    private String loginBody(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void validLoginReturnsOk() throws Exception {
        createUser("login@example.com", true);

        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("login@example.com", RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.id").exists())
                .andExpect(jsonPath("$.user.email").value("login@example.com"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void loginEmailIsTrimmedAndNormalized() throws Exception {
        createUser("normalize@example.com", true);

        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("  Normalize@Example.com  ", RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("normalize@example.com"));
    }

    @Test
    void generatedTokenHasValidSignatureAndExpectedClaims() throws Exception {
        AuthUser user = createUser("claims@example.com", true);

        MvcResult result = mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("claims@example.com", RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(responseBody, "$.accessToken");

        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET_BASE64));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo(String.valueOf(user.getId()));
        assertThat(claims.get("email", String.class)).isEqualTo("claims@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getIssuer()).isEqualTo("skillteam-auth-service");
        assertThat(claims.getAudience()).contains("skillteam-api");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getExpiration().toInstant())
                .isCloseTo(claims.getIssuedAt().toInstant().plus(Duration.ofMinutes(15)), within(5, ChronoUnit.SECONDS));
    }

    @Test
    void invalidPasswordReturnsUnauthorized() throws Exception {
        createUser("badpassword@example.com", true);

        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("badpassword@example.com", "WrongPassword@1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void unknownEmailReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("unknown@example.com", RAW_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void unknownEmailAndInvalidPasswordUseTheSameSafeMessage() throws Exception {
        createUser("known@example.com", true);

        MvcResult unknownResult = mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("unknown2@example.com", RAW_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        MvcResult wrongPasswordResult = mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("known@example.com", "WrongPassword@1")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String unknownMessage = com.jayway.jsonpath.JsonPath.read(unknownResult.getResponse().getContentAsString(), "$.message");
        String wrongPasswordMessage = com.jayway.jsonpath.JsonPath.read(wrongPasswordResult.getResponse().getContentAsString(), "$.message");

        assertThat(unknownMessage).isEqualTo(wrongPasswordMessage).isEqualTo("Invalid email or password.");
    }

    @Test
    void blankLoginRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedLoginRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"malformed@example.com\", \"password\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body."));
    }

    @Test
    void disabledAccountCannotLogIn() throws Exception {
        createUser("disabled@example.com", false);

        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("disabled@example.com", RAW_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void noSessionCookieIsCreatedOnLogin() throws Exception {
        createUser("nosession@example.com", true);

        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nosession@example.com", RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }
}
