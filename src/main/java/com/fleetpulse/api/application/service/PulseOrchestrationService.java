package com.fleetpulse.api.application.service;

import com.fleetpulse.api.application.port.in.SendPulseUseCase;
import com.fleetpulse.api.application.port.out.GpsCoordinateProvider;
import com.fleetpulse.api.application.port.out.PulseSender;
import com.fleetpulse.api.application.port.out.UnitRepository;
import com.fleetpulse.api.domain.exception.UnitNotActiveException;
import com.fleetpulse.api.domain.exception.UnitNotFoundException;
import com.fleetpulse.api.domain.model.GpsReading;
import com.fleetpulse.api.domain.model.ScheduledPulse;
import com.fleetpulse.api.domain.model.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Objects;

public class PulseOrchestrationService implements SendPulseUseCase {

    private static final Logger log = LoggerFactory.getLogger(PulseOrchestrationService.class);

    private final UnitRepository unitRepository;
    private final GpsCoordinateProvider gpsProvider;
    private final PulseSender pulseSender;
    private final String defaultTrackingNumber;
    private final Clock clock;

    public PulseOrchestrationService(UnitRepository unitRepository,
                                     GpsCoordinateProvider gpsProvider,
                                     PulseSender pulseSender,
                                     String defaultTrackingNumber,
                                     Clock clock) {
        this.unitRepository       = Objects.requireNonNull(unitRepository,       "unitRepository must not be null");
        this.gpsProvider          = Objects.requireNonNull(gpsProvider,           "gpsProvider must not be null");
        this.pulseSender          = Objects.requireNonNull(pulseSender,           "pulseSender must not be null");
        this.defaultTrackingNumber = Objects.requireNonNull(defaultTrackingNumber, "defaultTrackingNumber must not be null");
        this.clock                = Objects.requireNonNull(clock,                  "clock must not be null");
    }

    @Override
    public void sendPulse(String numUnidad) {
        Unit unit = unitRepository.findByNumUnidad(numUnidad)
                .orElseThrow(() -> new UnitNotFoundException(numUnidad));

        if (!unit.isActive()) {
            log.info("SKIPPED_UNIT_INACTIVE numUnidad={}", numUnidad);
            return;
        }

        if (!unit.isWithinActiveWindow(LocalTime.now(clock))) {
            log.info("SKIPPED_OUT_OF_WINDOW numUnidad={} window={}-{}",
                    numUnidad, unit.getHoraInicio(), unit.getHoraFin());
            return;
        }

        if (!gpsProvider.isAvailable(numUnidad)) {
            log.info("SKIPPED_NO_COORDINATES numUnidad={}", numUnidad);
            return;
        }

        GpsReading reading = gpsProvider.getCoordinates(numUnidad);
        sendScheduledPulse(unit, reading);
    }

    @Override
    public void dispatch(String numUnidad, GpsReading gpsReading) {
        Objects.requireNonNull(gpsReading, "gpsReading must not be null");

        Unit unit = unitRepository.findByNumUnidad(numUnidad)
                .orElseThrow(() -> new UnitNotFoundException(numUnidad));

        if (!unit.isActive()) {
            throw new UnitNotActiveException(numUnidad);
        }

        sendScheduledPulse(unit, gpsReading);
    }

    private void sendScheduledPulse(Unit unit, GpsReading reading) {
        String effectiveTracking = (unit.getTrackingNumber() != null)
                ? unit.getTrackingNumber()
                : defaultTrackingNumber;

        ZonedDateTime fechaRecepcion = ZonedDateTime.now(clock);

        // PulseSendException / GpsProviderUnavailableException propagate to GlobalExceptionHandler (502 / 503)
        pulseSender.send(new ScheduledPulse(unit, reading, fechaRecepcion, effectiveTracking));
    }
}
