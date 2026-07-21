package com.skillteam.auth;

import com.skillteam.auth.entity.AuthUser;
import com.skillteam.auth.entity.Role;
import com.skillteam.auth.repository.AuthUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRegistrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";

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

    @Test
    void validRegistrationReturnsCreated() throws Exception {
        String body = """
                {"email":"User@Example.com","password":"Password@123"}
                """;

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registeredEmailIsNormalizedToLowercase() throws Exception {
        String body = """
                {"email":"MixedCase@Example.com","password":"Password@123"}
                """;

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        Optional<AuthUser> saved = authUserRepository.findByEmail("mixedcase@example.com");
        assertThat(saved).isPresent();
    }

    @Test
    void passwordIsStoredAsBcryptHashAndVerifiable() throws Exception {
        String rawPassword = "Password@123";
        String body = "{\"email\":\"hash@example.com\",\"password\":\"" + rawPassword + "\"}";

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        AuthUser saved = authUserRepository.findByEmail("hash@example.com").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(saved.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches(rawPassword, saved.getPasswordHash())).isTrue();
    }

    @Test
    void registeredRoleIsUser() throws Exception {
        String body = """
                {"email":"role@example.com","password":"Password@123"}
                """;

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        AuthUser saved = authUserRepository.findByEmail("role@example.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void registeredAccountIsEnabled() throws Exception {
        String body = """
                {"email":"enabled@example.com","password":"Password@123"}
                """;

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        AuthUser saved = authUserRepository.findByEmail("enabled@example.com").orElseThrow();
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void duplicateEmailRegistrationReturnsConflict() throws Exception {
        String body = """
                {"email":"dup@example.com","password":"Password@123"}
                """;

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("An account with this email already exists."));
    }

    @Test
    void invalidEmailReturnsBadRequest() throws Exception {
        String body = """
                {"email":"not-an-email","password":"Password@123"}
                """;

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankPasswordReturnsBadRequest() throws Exception {
        String body = """
                {"email":"blankpw@example.com","password":""}
                """;

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shortPasswordReturnsBadRequest() throws Exception {
        String body = """
                {"email":"shortpw@example.com","password":"short1"}
                """;

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emailWithWhitespaceAndUppercaseIsStoredTrimmedAndLowercase() throws Exception {
        String body = """
                {"email":"  TrimMe@Example.com  ","password":"Password@123"}
                """;

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("trimme@example.com"));

        Optional<AuthUser> saved = authUserRepository.findByEmail("trimme@example.com");
        assertThat(saved).isPresent();
    }

    @Test
    void malformedJsonReturnsBadRequestWithSafeErrorStructure() throws Exception {
        String body = "{\"email\":\"malformed@example.com\", \"password\":";

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed request body."))
                .andExpect(jsonPath("$.path").value(REGISTER_URL));
    }
}
