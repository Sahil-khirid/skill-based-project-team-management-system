package com.skillteam.gateway.security;

public class InvalidGatewayTokenException extends RuntimeException {

    public InvalidGatewayTokenException(String message) {
        super(message);
    }
}
