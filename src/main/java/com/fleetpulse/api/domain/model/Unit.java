package com.fleetpulse.api.domain.model;

import java.time.LocalTime;

public class Unit {

    private String numUnidad;
    private boolean horarioFijo;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String trackingNumber;
    private boolean active;

    public Unit(String numUnidad, boolean horarioFijo, LocalTime horaInicio,
                LocalTime horaFin, String trackingNumber, boolean active) {
        this.numUnidad = numUnidad;
        this.horarioFijo = horarioFijo;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.trackingNumber = trackingNumber;
        this.active = active;
    }

    public boolean isWithinActiveWindow(LocalTime now) {
        if (!active) return false;
        // Note: overnight windows (horaInicio > horaFin) are not supported — always returns false
        return !now.isBefore(horaInicio) && !now.isAfter(horaFin);
    }

    public String getNumUnidad() { return numUnidad; }
    public boolean isHorarioFijo() { return horarioFijo; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public String getTrackingNumber() { return trackingNumber; }
    public boolean isActive() { return active; }
}
