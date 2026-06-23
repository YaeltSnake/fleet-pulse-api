package com.fleetpulse.api.domain.exception;

public class ScheduleNotConfiguredException extends RuntimeException {
    public ScheduleNotConfiguredException(String numUnidad) {
        super("No dispatch window configured for unit: " + numUnidad
                + " — configure via PUT /api/units/{numUnidad}/schedule before starting a round");
    }
}
