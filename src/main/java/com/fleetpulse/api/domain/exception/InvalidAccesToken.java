package com.fleetpulse.api.domain.exception;

public class InvalidAccesToken extends RuntimeException {
    public InvalidAccesToken(String message) {
        super(message);
    }
}
