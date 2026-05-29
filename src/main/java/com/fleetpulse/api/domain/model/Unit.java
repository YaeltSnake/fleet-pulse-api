package com.fleetpulse.api.domain.model;

import java.time.LocalTime;
import java.util.Objects;

public class Unit {

    private final String numUnidad;
    private final boolean horarioFijo;
    private final LocalTime horaInicio;
    private final LocalTime horaFin;
    private final String trackingNumber; // nullable by design: null means global env default
    private final boolean active;

    public Unit(String numUnidad, boolean horarioFijo, LocalTime horaInicio,
                LocalTime horaFin, String trackingNumber, boolean active) {
        this.numUnidad = Objects.requireNonNull(numUnidad, "numUnidad must not be null");
        this.horarioFijo = horarioFijo;
        this.horaInicio = Objects.requireNonNull(horaInicio, "horaInicio must not be null");
        this.horaFin = Objects.requireNonNull(horaFin, "horaFin must not be null");
        this.trackingNumber = trackingNumber;
        this.active = active;
    }

    // Business invariant: fleet operates daytime only. Overnight schedules
    // (horaInicio > horaFin) are rejected at configuration time by
    // ConfigureScheduleUseCase. This method assumes valid daytime windows.
    public boolean isWithinActiveWindow(LocalTime now) {
        if (!active) return false;
        // Business invariant: fleet operates daytime only. Overnight schedules
        // (horaInicio > horaFin) are rejected at configuration time by
        // ConfigureScheduleUseCase. This method assumes valid daytime windows.
        if (horaInicio.isAfter(horaFin)) {
            throw new IllegalStateException(
                    "Overnight window detected for unit " + numUnidad +
                            ". This violates fleet operating policy.");
        }
        return !now.isBefore(horaInicio) && !now.isAfter(horaFin);
    }

    public String getNumUnidad() { return numUnidad; }
    public boolean isHorarioFijo() { return horarioFijo; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public String getTrackingNumber() { return trackingNumber; }
    public boolean isActive() { return active; }
}
