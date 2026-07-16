package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fleetpulse.api.application.port.out.PulseLogRepository;
import com.fleetpulse.api.domain.model.FleetConstants;
import com.fleetpulse.api.domain.model.PulseLogStatus;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.PulseLogResponse;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.PulseLogStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Tag(name = "Pulse Log", description = "GPS pulse dispatch history")
@RestController
@RequestMapping("/api/pulse-log")
@Validated
public class PulseLogController {

    private final PulseLogRepository pulseLogRepository;
    private final Clock clock;

    public PulseLogController(PulseLogRepository pulseLogRepository, Clock clock) {
        this.pulseLogRepository = Objects.requireNonNull(pulseLogRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    @Operation(summary = "List pulse log entries with optional filters",
               description = "Returns paginated dispatch history. All filters are optional.")
    public ResponseEntity<PulseLogPageResponse> listLogs(
            @RequestParam(required = false) String numUnidad,
            @RequestParam(required = false) PulseLogStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        List<PulseLogResponse> content = pulseLogRepository
                .findByFilters(numUnidad, status, from, to, page, size)
                .stream()
                .map(PulseLogResponse::from)
                .toList();

        long total = pulseLogRepository.countByFilters(numUnidad, status, from, to);

        return ResponseEntity.ok(new PulseLogPageResponse(content, page, size, total));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    @Operation(summary = "Grouped dispatch counts by status for a single day",
               description = "Defaults to today (fleet timezone) if 'date' is omitted. "
                       + "Scoped to pulse-log stats only -- unit-active counts come from GET /api/units.")
    public ResponseEntity<PulseLogStatsResponse> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now(clock);
        ZonedDateTime from = targetDate.atStartOfDay(FleetConstants.FLEET_TIMEZONE);
        ZonedDateTime to = from.plusDays(1);

        Map<PulseLogStatus, Long> countsByStatus = pulseLogRepository.countGroupedByStatus(from, to);

        return ResponseEntity.ok(PulseLogStatsResponse.of(targetDate, countsByStatus));
    }

    public record PulseLogPageResponse(
            List<PulseLogResponse> content,
            int page,
            int size,
            long totalElements
    ) {}
}
