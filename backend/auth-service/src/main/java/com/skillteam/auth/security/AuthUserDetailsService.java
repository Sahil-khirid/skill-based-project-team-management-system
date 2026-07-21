package com.skillteam.auth.security;

import com.skillteam.auth.repository.AuthUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthUserDetailsService implements UserDetailsService {

    private static final String GENERIC_FAILURE_MESSAGE = "Invalid email or password.";

    private final AuthUserRepository authUserRepository;

    public AuthUserDetailsService(AuthUserRepository authUserRepository) {
        this.authUserRepository = authUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase(Locale.ROOT);

        return authUserRepository.findByEmail(normalizedEmail)
                .map(AuthUserPrincipal::fromAuthUser)
                .orElseThrow(() -> new UsernameNotFoundException(GENERIC_FAILURE_MESSAGE));
    }
}
