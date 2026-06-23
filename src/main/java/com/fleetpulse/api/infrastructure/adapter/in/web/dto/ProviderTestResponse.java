package com.fleetpulse.api.infrastructure.adapter.in.web.dto;

import com.fleetpulse.api.domain.model.GpsReading;
import com.fleetpulse.api.domain.model.ProviderType;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record ProviderTestResponse(
        String numUnidad,
        BigDecimal lat,
        BigDecimal lon,
        ZonedDateTime timestamp,
        ProviderType providerType
) {
    public static ProviderTestResponse from(GpsReading reading) {
        return new ProviderTestResponse(
                reading.getNumUnidad(),
                reading.getLatitud(),
                reading.getLongitud(),
                reading.getFechaHoraEvento(),
                reading.getProviderType());
    }
}
