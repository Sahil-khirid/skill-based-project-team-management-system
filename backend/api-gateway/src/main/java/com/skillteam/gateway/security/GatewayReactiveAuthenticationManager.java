package com.skillteam.gateway.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class GatewayReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final GatewayJwtValidator jwtValidator;

    public GatewayReactiveAuthenticationManager(GatewayJwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (!(authentication instanceof GatewayBearerAuthenticationToken bearerToken)) {
            return Mono.empty();
        }

        return Mono.<Authentication>fromCallable(() -> {
            GatewayUserPrincipal principal = jwtValidator.validate((String) bearerToken.getCredentials());
            return new GatewayBearerAuthenticationToken(principal, principal.authorities());
        }).onErrorMap(InvalidGatewayTokenException.class,
                ex -> new BadCredentialsException("Invalid access token."));
    }
}
