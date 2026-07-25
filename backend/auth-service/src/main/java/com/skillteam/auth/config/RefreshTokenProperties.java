package com.skillteam.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.Duration;

@ConfigurationProperties(prefix = "auth.refresh-token")
public class RefreshTokenProperties {

    private final Duration ttl;

    @ConstructorBinding
    public RefreshTokenProperties(Duration ttl) {
        this.ttl = requirePositive(ttl);
    }

    private static Duration requirePositive(Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException("auth.refresh-token.ttl must be a positive duration.");
        }
        return value;
    }

    public Duration getTtl() {
        return ttl;
    }

    @Override
    public String toString() {
        return "RefreshTokenProperties{ttl=" + ttl + "}";
    }
}
