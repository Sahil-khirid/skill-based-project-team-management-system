package com.skillteam.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {

    public RegisterRequest {
        // Only the email is trimmed here; the password must reach validation
        // and encoding untouched, per the registration input rules.
        email = email == null ? null : email.trim();
    }
}
