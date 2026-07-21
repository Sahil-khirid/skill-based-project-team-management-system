package com.skillteam.gateway.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Objects;

public final class GatewayUserPrincipal {

    private final Long userId;
    private final String email;
    private final String role;

    public GatewayUserPrincipal(Long userId, String email, String role) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String toString() {
        return "GatewayUserPrincipal{userId=" + userId + ", email='" + email + "', role='" + role + "'}";
    }
}
