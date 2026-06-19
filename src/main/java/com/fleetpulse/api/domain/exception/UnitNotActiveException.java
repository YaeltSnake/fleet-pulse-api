package com.fleetpulse.api.domain.exception;

public class UnitNotActiveException extends RuntimeException {
    public UnitNotActiveException(String numUnidad) {
        super("Unit is not active: " + numUnidad);
    }
}
