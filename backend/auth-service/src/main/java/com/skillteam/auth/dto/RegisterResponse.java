package com.skillteam.auth.dto;

import com.skillteam.auth.entity.Role;

import java.time.Instant;

public record RegisterResponse(
        Long id,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt
) {
}
