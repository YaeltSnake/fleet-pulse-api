package com.fleetpulse.api.domain.model;

import java.time.ZonedDateTime;

public class ScheduledPulse {

    private final Unit unit;
    private final GpsReading gpsReading;
    private final ZonedDateTime fechaRecepcion;
    private final String effectiveTrackingNumber;

    public ScheduledPulse(Unit unit, GpsReading gpsReading,
                          ZonedDateTime fechaRecepcion, String effectiveTrackingNumber) {
        this.unit = unit;
        this.gpsReading = gpsReading;
        this.fechaRecepcion = fechaRecepcion;
        this.effectiveTrackingNumber = effectiveTrackingNumber;
    }

    public Unit getUnit() { return unit; }
    public GpsReading getGpsReading() { return gpsReading; }
    public ZonedDateTime getFechaRecepcion() { return fechaRecepcion; }
    public String getEffectiveTrackingNumber() { return effectiveTrackingNumber; }
}
