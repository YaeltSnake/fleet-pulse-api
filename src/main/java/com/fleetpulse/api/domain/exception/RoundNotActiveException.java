package com.fleetpulse.api.domain.exception;

public class RoundNotActiveException extends RuntimeException {
    public RoundNotActiveException(String numUnidad) {
        super("No active round found for unit: " + numUnidad);
    }
}
