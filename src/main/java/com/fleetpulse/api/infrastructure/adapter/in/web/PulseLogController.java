package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fleetpulse.api.application.port.out.PulseLogRepository;
import com.fleetpulse.api.domain.model.PulseLogStatus;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.PulseLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

@Tag(name = "Pulse Log", description = "GPS pulse dispatch history")
@RestController
@RequestMapping("/api/pulse-log")
public class PulseLogController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE     = 100;

    private final PulseLogRepository pulseLogRepository;

    public PulseLogController(PulseLogRepository pulseLogRepository) {
        this.pulseLogRepository = Objects.requireNonNull(pulseLogRepository);
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, MAX_SIZE);

        List<PulseLogResponse> content = pulseLogRepository
                .findByFilters(numUnidad, status, from, to, page, size)
                .stream()
                .map(PulseLogResponse::from)
                .toList();

        long total = pulseLogRepository.countByFilters(numUnidad, status, from, to);

        return ResponseEntity.ok(new PulseLogPageResponse(content, page, size, total));
    }

    public record PulseLogPageResponse(
            List<PulseLogResponse> content,
            int page,
            int size,
            long totalElements
    ) {}
}
