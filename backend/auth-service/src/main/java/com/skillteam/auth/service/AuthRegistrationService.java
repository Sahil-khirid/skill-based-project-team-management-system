package com.skillteam.auth.service;

import com.skillteam.auth.dto.RegisterRequest;
import com.skillteam.auth.dto.RegisterResponse;
import com.skillteam.auth.entity.AuthUser;
import com.skillteam.auth.entity.Role;
import com.skillteam.auth.exception.DuplicateEmailException;
import com.skillteam.auth.repository.AuthUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthRegistrationService {

    private static final String DUPLICATE_EMAIL_MESSAGE = "An account with this email already exists.";

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthRegistrationService(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().toLowerCase(Locale.ROOT);

        if (authUserRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(DUPLICATE_EMAIL_MESSAGE);
        }

        String passwordHash = passwordEncoder.encode(request.password());
        AuthUser authUser = new AuthUser(normalizedEmail, passwordHash, Role.USER, true);

        AuthUser saved;
        try {
            saved = authUserRepository.saveAndFlush(authUser);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEmailException(DUPLICATE_EMAIL_MESSAGE);
        }

        return new RegisterResponse(saved.getId(), saved.getEmail(), saved.getRole(), saved.isEnabled(), saved.getCreatedAt());
    }
}
