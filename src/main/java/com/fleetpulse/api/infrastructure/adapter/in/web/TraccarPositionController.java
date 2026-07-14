package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fleetpulse.api.domain.exception.InvalidCoordinateException;
import com.fleetpulse.api.domain.model.GpsReading;
import com.fleetpulse.api.domain.model.ProviderType;
import com.fleetpulse.api.infrastructure.adapter.out.cache.GpsPositionCache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

@Tag(name = "GPS", description = "Traccar OsmAnd push endpoint — public, no authentication required")
@RestController
@RequestMapping("/api/gps")
public class TraccarPositionController {

    private static final Logger log = LoggerFactory.getLogger(TraccarPositionController.class);

    private final GpsPositionCache gpsPositionCache;
    private final Clock clock;

    public TraccarPositionController(GpsPositionCache gpsPositionCache, Clock clock) {
        this.gpsPositionCache = Objects.requireNonNull(gpsPositionCache);
        this.clock = Objects.requireNonNull(clock);
    }

    @GetMapping("/position")
    @Operation(
            summary = "Receive GPS push from Traccar Client (HTTP GET / OsmAnd protocol)",
            description = "Public endpoint required by OsmAnd protocol. Uses GET with query params. "
                    + "Unknown unit IDs are silently accepted. Invalid coordinates return 400."
    )
    public ResponseEntity<Void> receivePosition(
            @RequestParam String id,
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false) Long timestamp,
            @RequestParam(required = false) Double accuracy,
            @RequestParam(required = false) Double speed,
            @RequestParam(required = false) Double bearing) {
        return storeReading(id, lat, lon);
    }

    @PostMapping("/position")
    @Operation(
            summary = "Receive GPS push from Traccar Client (HTTP POST / OsmAnd protocol)",
            description = "Public endpoint for Traccar Client's HTTP POST mode. The real client sends the "
                    + "same OsmAnd-style fields (id, lat, lon, timestamp, accuracy, altitude, batt, charge) "
                    + "as an application/x-www-form-urlencoded body, not a JSON payload — Spring binds "
                    + "form-urlencoded POST parameters the same way as query parameters. "
                    + "Unknown unit IDs are silently accepted. Invalid coordinates return 400."
    )
    public ResponseEntity<Void> receivePositionForm(
            @RequestParam String id,
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false) Long timestamp,
            @RequestParam(required = false) Double accuracy,
            @RequestParam(required = false) Double altitude,
            @RequestParam(required = false) Integer batt,
            @RequestParam(required = false) Boolean charge) {
        return storeReading(id, lat, lon);
    }

    private ResponseEntity<Void> storeReading(String id, double lat, double lon) {
        if (id == null || id.isBlank()) {
            throw new InvalidCoordinateException("id (numUnidad) must not be blank");
        }

        // GpsReading constructor validates range [-90,90]/[-180,180] and rejects (0.0, 0.0)
        GpsReading reading = new GpsReading(
                id,
                BigDecimal.valueOf(lat),
                BigDecimal.valueOf(lon),
                ZonedDateTime.now(clock),
                ProviderType.TRACCAR
        );

        gpsPositionCache.store(id, reading);

        // Round to 4 decimals in logs — never log precise GPS coordinates (Skill 01)
        log.info("GPS_RECEIVED numUnidad={} lat={} lon={}",
                id,
                String.format("%.4f", lat),
                String.format("%.4f", lon));


        return ResponseEntity.ok().build();
    }
}
