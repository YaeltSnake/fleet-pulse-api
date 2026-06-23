package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fleetpulse.api.application.port.in.SendPulseUseCase;
import com.fleetpulse.api.domain.exception.InvalidCoordinateException;
import com.fleetpulse.api.domain.model.CoordinateMode;
import com.fleetpulse.api.domain.model.FleetConstants;
import com.fleetpulse.api.domain.model.GpsReading;
import com.fleetpulse.api.domain.model.ProviderType;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.ForcePulseRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.Objects;

@Tag(name = "Pulse Dispatch", description = "Force GPS pulse dispatch to QSolutions for a single unit")
@RestController
@RequestMapping("/api/units")
public class PulseController {

    private final SendPulseUseCase sendPulseUseCase;

    public PulseController(SendPulseUseCase sendPulseUseCase) {
        this.sendPulseUseCase = Objects.requireNonNull(sendPulseUseCase,
                "sendPulseUseCase must not be null");
    }

    @PostMapping("/{numUnidad}/pulse/force")
    @Operation(
            summary = "Force-dispatch a GPS pulse for a single unit with operator-supplied coordinates",
            description = "Bypasses the schedule window check. coordinateMode=MANUAL requires lat and lon. "
                    + "coordinateMode=AUTOMATIC is reserved for Phase 6 (TraccarCoordinateAdapter).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pulse dispatched and confirmed by QSolutions"),
            @ApiResponse(responseCode = "400", description = "Invalid or missing coordinates, or unsupported coordinateMode",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Unit not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Unit is inactive",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "QSolutions rejected the pulse (isProcessed=false)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "QSolutions unreachable",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> forceDispatch(
            @PathVariable String numUnidad,
            @Valid @RequestBody ForcePulseRequest request) {

        // FIXME-PHASE6: remove this guard when TraccarCoordinateAdapter is wired (Phase 6)
        if (request.coordinateMode() == CoordinateMode.AUTOMATIC) {
            throw new InvalidCoordinateException("AUTOMATIC mode not supported until Phase 6");
        }

        if (request.lat() == null || request.lon() == null) {
            throw new InvalidCoordinateException("MANUAL mode requires lat and lon");
        }

        // GpsReading constructor validates range — throws InvalidCoordinateException if OOB or (0,0)
        GpsReading reading = new GpsReading(
                numUnidad,
                request.lat(),
                request.lon(),
                ZonedDateTime.now(FleetConstants.FLEET_TIMEZONE),
                ProviderType.MANUAL);

        sendPulseUseCase.dispatch(numUnidad, reading);

        return ResponseEntity.noContent().build();
    }
}
