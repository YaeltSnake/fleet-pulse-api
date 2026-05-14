package com.fleetpulse.api.domain.model;

import com.fleetpulse.api.domain.exception.InvalidCoordinateException;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class GpsReading {

    private final String numUnidad;
    private final BigDecimal latitud;
    private final BigDecimal longitud;
    private final ZonedDateTime fechaHoraEvento;
    private final ProviderType providerType;

    public GpsReading(String numUnidad, BigDecimal latitud, BigDecimal longitud,
                      ZonedDateTime fechaHoraEvento, ProviderType providerType) {
        if (latitud.compareTo(new BigDecimal("-90")) < 0 || latitud.compareTo(new BigDecimal("90")) > 0) {
            throw new InvalidCoordinateException("Latitude out of range [-90, 90]: " + latitud);
        }
        if (longitud.compareTo(new BigDecimal("-180")) < 0 || longitud.compareTo(new BigDecimal("180")) > 0) {
            throw new InvalidCoordinateException("Longitude out of range [-180, 180]: " + longitud);
        }
        if (BigDecimal.ZERO.compareTo(latitud) == 0 && BigDecimal.ZERO.compareTo(longitud) == 0) {
            throw new InvalidCoordinateException("Coordinates (0, 0) are not valid for a fleet unit");
        }
        this.numUnidad = numUnidad;
        this.latitud = latitud;
        this.longitud = longitud;
        this.fechaHoraEvento = fechaHoraEvento;
        this.providerType = providerType;
    }

    public String getNumUnidad() { return numUnidad; }
    public BigDecimal getLatitud() { return latitud; }
    public BigDecimal getLongitud() { return longitud; }
    public ZonedDateTime getFechaHoraEvento() { return fechaHoraEvento; }
    public ProviderType getProviderType() { return providerType; }

    // TODO: implement equals/hashCode by business key in a future phase
}
