package com.fleetpulse.api.application.service;

import com.fleetpulse.api.application.port.in.ManageRoundUseCase;
import com.fleetpulse.api.application.port.in.SendPulseUseCase;
import com.fleetpulse.api.application.port.out.GpsCoordinateProvider;
import com.fleetpulse.api.application.port.out.UnitRepository;
import com.fleetpulse.api.domain.exception.GpsProviderUnavailableException;
import com.fleetpulse.api.domain.exception.InvalidCoordinateException;
import com.fleetpulse.api.domain.exception.RoundAlreadyActiveException;
import com.fleetpulse.api.domain.exception.RoundNotActiveException;
import com.fleetpulse.api.domain.exception.ScheduleNotConfiguredException;
import com.fleetpulse.api.domain.exception.UnitNotActiveException;
import com.fleetpulse.api.domain.exception.UnitNotFoundException;
import com.fleetpulse.api.domain.model.CoordinateMode;
import com.fleetpulse.api.domain.model.GpsReading;
import com.fleetpulse.api.domain.model.ProviderType;
import com.fleetpulse.api.domain.model.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class RoundManagementService implements ManageRoundUseCase {

    private static final Logger log = LoggerFactory.getLogger(RoundManagementService.class);

    private final UnitRepository unitRepository;
    private final SendPulseUseCase sendPulseUseCase;
    private final GpsCoordinateProvider gpsProvider;
    private final Clock clock;
    private final long roundIntervalMs;

    private final ConcurrentHashMap<String, RoundState> activeRounds = new ConcurrentHashMap<>();

    public RoundManagementService(UnitRepository unitRepository,
                                   SendPulseUseCase sendPulseUseCase,
                                   GpsCoordinateProvider gpsProvider,
                                   Clock clock,
                                   long roundIntervalMs) {
        this.unitRepository   = Objects.requireNonNull(unitRepository,   "unitRepository must not be null");
        this.sendPulseUseCase = Objects.requireNonNull(sendPulseUseCase, "sendPulseUseCase must not be null");
        this.gpsProvider      = Objects.requireNonNull(gpsProvider,      "gpsProvider must not be null");
        this.clock            = Objects.requireNonNull(clock,            "clock must not be null");
        this.roundIntervalMs  = roundIntervalMs;
    }

    @Override
    public void startRound(String numUnidad, CoordinateMode coordinateMode,
                            BigDecimal manualLat, BigDecimal manualLon) {
        Unit unit = unitRepository.findByNumUnidad(numUnidad)
                .orElseThrow(() -> new UnitNotFoundException(numUnidad));

        if (!unit.isActive()) {
            throw new UnitNotActiveException(numUnidad);
        }

        if (activeRounds.containsKey(numUnidad)) {
            throw new RoundAlreadyActiveException(numUnidad);
        }

        if (coordinateMode == CoordinateMode.MANUAL) {
            if (manualLat == null || manualLon == null) {
                throw new InvalidCoordinateException("MANUAL mode requires lat and lon");
            }
            // Validate coordinate range via domain constructor — throws InvalidCoordinateException if OOB or 0,0
            new GpsReading(numUnidad, manualLat, manualLon, ZonedDateTime.now(clock), ProviderType.MANUAL);
        }

        LocalTime horaInicio = unit.getHoraInicio();
        LocalTime horaFin    = unit.getHoraFin();

        if (horaInicio.equals(LocalTime.MIN) && horaFin.equals(LocalTime.MAX)) {
            throw new ScheduleNotConfiguredException(numUnidad);
        }

        // putIfAbsent is the concurrent race guard: if a concurrent startRound sneaked in
        // between the containsKey check and here, putIfAbsent returns the existing value
        RoundState existing = activeRounds.putIfAbsent(numUnidad,
                new RoundState(horaInicio, horaFin, coordinateMode, manualLat, manualLon, null));

        if (existing != null) {
            throw new RoundAlreadyActiveException(numUnidad);
        }

        log.info("ROUND_STARTED numUnidad={} coordinateMode={} window={}-{}",
                numUnidad, coordinateMode, horaInicio, horaFin);
    }

    @Override
    public void stopRound(String numUnidad) {
        RoundState removed = activeRounds.remove(numUnidad);
        if (removed == null) {
            throw new RoundNotActiveException(numUnidad);
        }
        log.info("ROUND_STOPPED numUnidad={}", numUnidad);
    }

    @Override
    public boolean isRoundActive(String numUnidad) {
        return activeRounds.containsKey(numUnidad);
    }

    @Override
    public CoordinateMode getRoundCoordinateMode(String numUnidad) {
        RoundState state = activeRounds.get(numUnidad);
        return state != null ? state.coordinateMode() : null;
    }

    @Override
    public void processTick() {
        LocalTime now     = LocalTime.now(clock);
        Instant   instant = Instant.now(clock);

        for (Map.Entry<String, RoundState> entry : activeRounds.entrySet()) {
            String     numUnidad = entry.getKey();
            RoundState state     = entry.getValue();

            if (now.isBefore(state.horaInicio()) || now.isAfter(state.horaFin())) {
                continue;
            }

            if (state.ultimoEnvio() != null
                    && Duration.between(state.ultimoEnvio(), instant).toMillis() < roundIntervalMs) {
                continue;
            }

            try {
                GpsReading reading;
                if (state.coordinateMode() == CoordinateMode.MANUAL) {
                    reading = new GpsReading(numUnidad, state.manualLat(), state.manualLon(),
                            ZonedDateTime.now(clock), ProviderType.MANUAL);
                } else {
                    reading = gpsProvider.getCoordinates(numUnidad);
                }

                sendPulseUseCase.dispatch(numUnidad, reading);

                // computeIfPresent is atomic: no-op if the round was stopped between dispatch and here
                activeRounds.computeIfPresent(numUnidad,
                        (k, v) -> new RoundState(v.horaInicio(), v.horaFin(),
                                v.coordinateMode(), v.manualLat(), v.manualLon(), instant));

                log.info("ROUND_PULSE_SENT numUnidad={}", numUnidad);

            } catch (GpsProviderUnavailableException e) {
                activeRounds.remove(numUnidad);
                log.warn("ROUND_REMOVED_GPS_UNAVAILABLE numUnidad={}", numUnidad);

            } catch (UnitNotActiveException e) {
                activeRounds.remove(numUnidad);
                log.warn("ROUND_REMOVED_UNIT_INACTIVE numUnidad={}", numUnidad);

            } catch (Exception e) {
                log.error("ROUND_TICK_UNIT_FAILED numUnidad={} reason={}", numUnidad, e.getMessage());
            }
        }
    }

    /** Exposed for testing — returns an unmodifiable view of active rounds. */
    public Map<String, RoundState> activeRounds() {
        return java.util.Collections.unmodifiableMap(activeRounds);
    }
}
