package com.fleetpulse.api.application.service;

import com.fleetpulse.api.application.port.in.TestProviderUseCase;
import com.fleetpulse.api.application.port.out.GpsCoordinateProvider;
import com.fleetpulse.api.application.port.out.UnitRepository;
import com.fleetpulse.api.domain.exception.GpsProviderUnavailableException;
import com.fleetpulse.api.domain.exception.UnitNotFoundException;
import com.fleetpulse.api.domain.model.GpsReading;
import com.fleetpulse.api.domain.model.ProviderType;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

public class ProviderTestService implements TestProviderUseCase {

    private final UnitRepository unitRepository;
    private final GpsCoordinateProvider gpsProvider;
    private final Clock clock;

    // PulseSender is intentionally NOT injected — this service never dispatches to QSolutions

    public ProviderTestService(UnitRepository unitRepository,
                                GpsCoordinateProvider gpsProvider,
                                Clock clock) {
        this.unitRepository = Objects.requireNonNull(unitRepository, "unitRepository must not be null");
        this.gpsProvider    = Objects.requireNonNull(gpsProvider,    "gpsProvider must not be null");
        this.clock          = Objects.requireNonNull(clock,          "clock must not be null");
    }

    @Override
    public GpsReading testProvider(String numUnidad) {
        unitRepository.findByNumUnidad(numUnidad)
                .orElseThrow(() -> new UnitNotFoundException(numUnidad));

        if (!gpsProvider.isAvailable(numUnidad)) {
            throw new GpsProviderUnavailableException("Provider not available for unit: " + numUnidad);
        }

        return gpsProvider.getCoordinates(numUnidad);
    }

    @Override
    public GpsReading testProviderWithOverride(String numUnidad, BigDecimal lat, BigDecimal lon) {
        unitRepository.findByNumUnidad(numUnidad)
                .orElseThrow(() -> new UnitNotFoundException(numUnidad));

        // GpsReading constructor validates range — throws InvalidCoordinateException if OOB or (0,0)
        // gpsProvider is NOT called — caller-supplied coords bypass the configured provider
        return new GpsReading(numUnidad, lat, lon, ZonedDateTime.now(clock), ProviderType.MANUAL);
    }
}
