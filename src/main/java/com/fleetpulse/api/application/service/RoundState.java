package com.fleetpulse.api.application.service;

import com.fleetpulse.api.domain.model.CoordinateMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

public record RoundState(
        LocalTime horaInicio,
        LocalTime horaFin,
        CoordinateMode coordinateMode,
        BigDecimal manualLat,   // null when coordinateMode == AUTOMATIC
        BigDecimal manualLon,   // null when coordinateMode == AUTOMATIC
        Instant ultimoEnvio     // null if unit has not yet dispatched in this round
) {}
