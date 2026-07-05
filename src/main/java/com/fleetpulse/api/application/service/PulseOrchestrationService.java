package com.fleetpulse.api.application.service;

import com.fleetpulse.api.application.port.in.SendPulseUseCase;
import com.fleetpulse.api.application.port.out.GpsCoordinateProvider;
import com.fleetpulse.api.application.port.out.PulseLogRepository;
import com.fleetpulse.api.application.port.out.PulseSender;
import com.fleetpulse.api.application.port.out.UnitRepository;
import com.fleetpulse.api.domain.exception.GpsProviderUnavailableException;
import com.fleetpulse.api.domain.exception.PulseSendException;
import com.fleetpulse.api.domain.exception.UnitNotActiveException;
import com.fleetpulse.api.domain.exception.UnitNotFoundException;
import com.fleetpulse.api.domain.model.GpsReading;
import com.fleetpulse.api.domain.model.PulseLog;
import com.fleetpulse.api.domain.model.PulseLogStatus;
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
    private final PulseLogRepository pulseLogRepository;
    private final String defaultTrackingNumber;
    private final Clock clock;

    public PulseOrchestrationService(UnitRepository unitRepository,
                                     GpsCoordinateProvider gpsProvider,
                                     PulseSender pulseSender,
                                     PulseLogRepository pulseLogRepository,
                                     String defaultTrackingNumber,
                                     Clock clock) {
        this.unitRepository        = Objects.requireNonNull(unitRepository,        "unitRepository must not be null");
        this.gpsProvider           = Objects.requireNonNull(gpsProvider,            "gpsProvider must not be null");
        this.pulseSender           = Objects.requireNonNull(pulseSender,            "pulseSender must not be null");
        this.pulseLogRepository    = Objects.requireNonNull(pulseLogRepository,     "pulseLogRepository must not be null");
        this.defaultTrackingNumber = Objects.requireNonNull(defaultTrackingNumber,  "defaultTrackingNumber must not be null");
        this.clock                 = Objects.requireNonNull(clock,                  "clock must not be null");
    }

    /**
     * Scheduler path — resilient: SOAP rejections are logged and swallowed.
     * The scheduler tick must not die on a single unit failure.
     */
    @Override
    public void sendPulse(String numUnidad) {
        Unit unit = unitRepository.findByNumUnidad(numUnidad)
                .orElseThrow(() -> new UnitNotFoundException(numUnidad));

        ZonedDateTime now = ZonedDateTime.now(clock);

        if (!unit.isActive()) {
            log.info("SKIPPED_UNIT_INACTIVE numUnidad={}", numUnidad);
            pulseLogRepository.save(PulseLog.skipped(numUnidad, PulseLogStatus.SKIPPED_INACTIVE, now));
            return;
        }

        if (!unit.isWithinActiveWindow(LocalTime.now(clock))) {
            log.info("SKIPPED_OUT_OF_WINDOW numUnidad={} window={}-{}",
                    numUnidad, unit.getHoraInicio(), unit.getHoraFin());
            pulseLogRepository.save(PulseLog.skipped(numUnidad, PulseLogStatus.SKIPPED_OUT_OF_WINDOW, now));
            return;
        }

        GpsReading reading;
        try {
            reading = gpsProvider.getCoordinates(numUnidad);
        } catch (GpsProviderUnavailableException e) {
            PulseLogStatus skipStatus = isStaleMessage(e)
                    ? PulseLogStatus.SKIPPED_STALE
                    : PulseLogStatus.SKIPPED_NO_COORDS;
            log.info("SKIPPED numUnidad={} reason={}", numUnidad, skipStatus);
            pulseLogRepository.save(PulseLog.skipped(numUnidad, skipStatus, now));
            return;
        }

        String tracking = resolveTracking(unit);
        try {
            pulseSender.send(new ScheduledPulse(unit, reading, now, tracking));
            log.info("PULSE_SENT numUnidad={}", numUnidad);
            pulseLogRepository.save(PulseLog.sent(numUnidad, reading, tracking, now));
        } catch (PulseSendException e) {
            // Swallow: scheduler continues to next unit
            log.warn("PULSE_REJECTED numUnidad={} reason={}", numUnidad, e.getMessage());
            pulseLogRepository.save(PulseLog.rejected(numUnidad, reading, tracking, now, e.getMessage()));
        } catch (Exception e) {
            // Swallow unexpected errors: scheduler stays alive
            log.error("PULSE_ERROR numUnidad={} reason={}", numUnidad, e.getMessage());
            pulseLogRepository.save(PulseLog.error(numUnidad, reading, tracking, now, e.getMessage()));
        }
    }

    /**
     * Force-dispatch path — propagates failures to the HTTP layer (502 / 500).
     */
    @Override
    public void dispatch(String numUnidad, GpsReading gpsReading) {
        Objects.requireNonNull(gpsReading, "gpsReading must not be null");

        Unit unit = unitRepository.findByNumUnidad(numUnidad)
                .orElseThrow(() -> new UnitNotFoundException(numUnidad));

        if (!unit.isActive()) {
            throw new UnitNotActiveException(numUnidad);
        }

        String tracking = resolveTracking(unit);
        ZonedDateTime now = ZonedDateTime.now(clock);
        try {
            pulseSender.send(new ScheduledPulse(unit, gpsReading, now, tracking));
            log.info("DISPATCH_SENT numUnidad={}", numUnidad);
            pulseLogRepository.save(PulseLog.sent(numUnidad, gpsReading, tracking, now));
        } catch (PulseSendException e) {
            log.warn("DISPATCH_REJECTED numUnidad={} reason={}", numUnidad, e.getMessage());
            pulseLogRepository.save(PulseLog.rejected(numUnidad, gpsReading, tracking, now, e.getMessage()));
            throw e;  // propagates → 502
        } catch (Exception e) {
            log.error("DISPATCH_ERROR numUnidad={} reason={}", numUnidad, e.getMessage());
            pulseLogRepository.save(PulseLog.error(numUnidad, gpsReading, tracking, now, e.getMessage()));
            throw e;  // propagates → 500
        }
    }

    private String resolveTracking(Unit unit) {
        return unit.getTrackingNumber() != null ? unit.getTrackingNumber() : defaultTrackingNumber;
    }

    private boolean isStaleMessage(GpsProviderUnavailableException e) {
        return e.getMessage() != null && e.getMessage().contains("stale");
    }
}
