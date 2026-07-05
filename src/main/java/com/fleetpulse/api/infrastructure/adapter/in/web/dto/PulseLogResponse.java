package com.fleetpulse.api.infrastructure.adapter.in.web.dto;

import com.fleetpulse.api.domain.model.PulseLog;
import com.fleetpulse.api.domain.model.PulseLogStatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record PulseLogResponse(
        String numUnidad,
        PulseLogStatus status,
        BigDecimal lat,
        BigDecimal lon,
        String provider,
        String trackingNumber,
        ZonedDateTime sentAt,
        String errorMessage
) {
    public static PulseLogResponse from(PulseLog pulseLog) {
        return new PulseLogResponse(
                pulseLog.numUnidad(),
                pulseLog.status(),
                pulseLog.lat(),
                pulseLog.lon(),
                pulseLog.provider(),
                pulseLog.trackingNumber(),
                pulseLog.sentAt(),
                pulseLog.errorMessage()
        );
    }
}
