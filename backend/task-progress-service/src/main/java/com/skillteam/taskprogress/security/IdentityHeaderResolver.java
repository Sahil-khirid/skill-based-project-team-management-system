package com.skillteam.taskprogress.security;

import com.skillteam.taskprogress.exception.ForbiddenException;
import com.skillteam.taskprogress.exception.UnauthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Reads the Gateway-injected {@code X-Auth-User-Id} and {@code X-Auth-User-Role} headers. This
 * service does not perform independent JWT validation — it trusts the Gateway to have stripped
 * any client-supplied identity headers and to add its own only for requests that passed JWT
 * validation (see the API Gateway's TrustedIdentityHeaderFilter).
 */
@Component
public class IdentityHeaderResolver {

    public static final String USER_ID_HEADER = "X-Auth-User-Id";
    public static final String USER_ROLE_HEADER = "X-Auth-User-Role";

    private static final String ROLE_USER = "USER";
    private static final String ROLE_PROJECT_MANAGER = "PROJECT_MANAGER";

    private static final String UNAUTHENTICATED_MESSAGE = "Authentication is required.";
    private static final String FORBIDDEN_MESSAGE = "Access is denied.";

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

    public String resolveRole(HttpServletRequest request) {
        String value = request.getHeader(USER_ROLE_HEADER);
        if (value == null || value.isBlank()) {
            throw new UnauthenticatedException(UNAUTHENTICATED_MESSAGE);
        }

        String role = value.trim();
        if (!ROLE_USER.equals(role) && !ROLE_PROJECT_MANAGER.equals(role)) {
            throw new UnauthenticatedException(UNAUTHENTICATED_MESSAGE);
        }

        return role;
    }

    public void requireProjectManager(String role) {
        if (!ROLE_PROJECT_MANAGER.equals(role)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
    }
}
