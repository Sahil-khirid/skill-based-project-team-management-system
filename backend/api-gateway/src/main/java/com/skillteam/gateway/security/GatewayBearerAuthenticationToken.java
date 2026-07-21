package com.skillteam.gateway.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collections;
import java.util.List;

/**
 * Authentication token carrying either an unvalidated bearer token (pre-authentication) or a
 * validated {@link GatewayUserPrincipal} with granted authorities (post-authentication).
 */
public class GatewayBearerAuthenticationToken extends AbstractAuthenticationToken {

    private final String token;
    private final GatewayUserPrincipal principal;

    public GatewayBearerAuthenticationToken(String token) {
        super(Collections.emptyList());
        this.token = token;
        this.principal = null;
        setAuthenticated(false);
    }

    public GatewayBearerAuthenticationToken(GatewayUserPrincipal principal, List<GrantedAuthority> authorities) {
        super(authorities);
        this.token = null;
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
