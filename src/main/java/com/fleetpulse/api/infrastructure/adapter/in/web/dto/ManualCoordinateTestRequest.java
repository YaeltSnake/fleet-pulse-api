package com.fleetpulse.api.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ManualCoordinateTestRequest(
        @NotNull BigDecimal lat,
        @NotNull BigDecimal lon
) {}
