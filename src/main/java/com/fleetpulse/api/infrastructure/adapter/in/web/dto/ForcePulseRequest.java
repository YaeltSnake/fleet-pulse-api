package com.fleetpulse.api.infrastructure.adapter.in.web.dto;

import com.fleetpulse.api.domain.model.CoordinateMode;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ForcePulseRequest(
        @NotNull CoordinateMode coordinateMode,
        BigDecimal lat,   // required when coordinateMode == MANUAL
        BigDecimal lon    // required when coordinateMode == MANUAL
) {}
