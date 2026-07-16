package com.fleetpulse.api.infrastructure.adapter.in.web.dto;

import com.fleetpulse.api.domain.model.PulseLogStatus;

import java.time.LocalDate;
import java.util.Map;

public record PulseLogStatsResponse(
        LocalDate date,
        Map<PulseLogStatus, Long> countsByStatus,
        long total
) {
    public static PulseLogStatsResponse of(LocalDate date, Map<PulseLogStatus, Long> countsByStatus) {
        long total = countsByStatus.values().stream().mapToLong(Long::longValue).sum();
        return new PulseLogStatsResponse(date, countsByStatus, total);
    }
}
