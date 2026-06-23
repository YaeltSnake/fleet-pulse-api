package com.fleetpulse.api.application.port.in;

import com.fleetpulse.api.domain.model.GpsReading;

import java.math.BigDecimal;

public interface TestProviderUseCase {

    /**
     * Returns the reading from the configured GpsCoordinateProvider for this unit.
     * Never calls PulseSender.
     */
    GpsReading testProvider(String numUnidad);

    /**
     * Returns a GpsReading built from caller-supplied coordinates using the current clock.
     * Never calls the configured GpsCoordinateProvider or PulseSender.
     * GpsReading constructor validates range — throws InvalidCoordinateException if OOB or (0,0).
     */
    GpsReading testProviderWithOverride(String numUnidad, BigDecimal lat, BigDecimal lon);
}
