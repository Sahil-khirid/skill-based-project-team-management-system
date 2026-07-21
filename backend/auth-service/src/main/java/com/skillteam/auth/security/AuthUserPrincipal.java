package com.skillteam.auth.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.skillteam.auth.entity.AuthUser;
import com.skillteam.auth.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Objects;

public final class AuthUserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final Role role;
    private final boolean enabled;
    private final String passwordHash;

    public AuthUserPrincipal(Long id, String email, Role role, boolean enabled, String passwordHash) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.role = Objects.requireNonNull(role);
        this.enabled = enabled;
        this.passwordHash = Objects.requireNonNull(passwordHash);
    }

    public static AuthUserPrincipal fromAuthUser(AuthUser authUser) {
        return new AuthUserPrincipal(
                authUser.getId(),
                authUser.getEmail(),
                authUser.getRole(),
                authUser.isEnabled(),
                authUser.getPasswordHash());
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public String toString() {
        return "AuthUserPrincipal{id=" + id + ", email='" + email + "', role=" + role + ", enabled=" + enabled + "}";
    }
}
