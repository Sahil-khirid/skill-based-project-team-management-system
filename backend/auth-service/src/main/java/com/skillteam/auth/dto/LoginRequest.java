package com.skillteam.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {

    public LoginRequest {
        // Only the email is trimmed here; the password must reach validation and
        // authentication untouched, per the login input rules.
        email = email == null ? null : email.trim();
    }
}
