package com.skillteam.auth.dto;

import com.skillteam.auth.entity.Role;

public record AuthenticatedUserResponse(
        Long id,
        String email,
        Role role
) {
}
