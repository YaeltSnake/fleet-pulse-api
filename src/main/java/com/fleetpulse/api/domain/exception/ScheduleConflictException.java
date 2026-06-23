package com.fleetpulse.api.domain.exception;

public class ScheduleConflictException extends RuntimeException {
    public ScheduleConflictException(String numUnidad) {
        super("Schedule conflict for unit: " + numUnidad + " — horaInicio must be before horaFin");
    }
}
