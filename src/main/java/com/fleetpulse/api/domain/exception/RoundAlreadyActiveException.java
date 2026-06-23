package com.fleetpulse.api.domain.exception;

public class RoundAlreadyActiveException extends RuntimeException {
    public RoundAlreadyActiveException(String numUnidad) {
        super("A round is already active for unit: " + numUnidad);
    }
}
