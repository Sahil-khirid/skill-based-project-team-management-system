package com.skillteam.auth.service;

import com.skillteam.auth.dto.RegisterRequest;
import com.skillteam.auth.exception.DuplicateEmailException;
import com.skillteam.auth.repository.AuthUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRegistrationServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthRegistrationService authRegistrationService;

    @Test
    void concurrentDuplicateRegistrationIsTranslatedToDuplicateEmailException() {
        RegisterRequest request = new RegisterRequest("race@example.com", "Password@123");

        when(authUserRepository.existsByEmail("race@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("test-hash");
        when(authUserRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> authRegistrationService.register(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("An account with this email already exists.");
    }
}
