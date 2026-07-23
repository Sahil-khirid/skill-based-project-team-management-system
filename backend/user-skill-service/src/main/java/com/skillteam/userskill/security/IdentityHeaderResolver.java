package com.skillteam.userskill.security;

import com.skillteam.userskill.exception.UnauthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Reads the Gateway-injected {@code X-Auth-User-Id} header. This service does not perform
 * independent JWT validation in this milestone — it trusts the Gateway to have stripped any
 * client-supplied identity headers and to add its own only for requests that passed JWT
 * validation (see the API Gateway's TrustedIdentityHeaderFilter).
 */
@Component
public class IdentityHeaderResolver {

    public static final String USER_ID_HEADER = "X-Auth-User-Id";

    private static final String UNAUTHENTICATED_MESSAGE = "Authentication is required.";

    public Long resolve(HttpServletRequest request) {
        String value = request.getHeader(USER_ID_HEADER);
        if (value == null || value.isBlank()) {
            throw new UnauthenticatedException(UNAUTHENTICATED_MESSAGE);
        }

        long authUserId;
        try {
            authUserId = Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new UnauthenticatedException(UNAUTHENTICATED_MESSAGE);
        }

        if (authUserId <= 0) {
            throw new UnauthenticatedException(UNAUTHENTICATED_MESSAGE);
        }

        return authUserId;
    }
}
