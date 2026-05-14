package com.fleetpulse.api.domain.exception;

public class UnitNotFoundException extends RuntimeException {
    public UnitNotFoundException(String numUnidad) {
        super("Unit not found: " + numUnidad);
    }
}
