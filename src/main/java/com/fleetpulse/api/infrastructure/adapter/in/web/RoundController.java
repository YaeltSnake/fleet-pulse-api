package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fleetpulse.api.application.port.in.ManageRoundUseCase;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.RoundStartRequest;
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

import java.util.Objects;

@Tag(name = "Round Scheduling", description = "Per-unit dynamic round dispatch lifecycle")
@RestController
@RequestMapping("/api/units")
public class RoundController {

    private final ManageRoundUseCase manageRoundUseCase;

    public RoundController(ManageRoundUseCase manageRoundUseCase) {
        this.manageRoundUseCase = Objects.requireNonNull(manageRoundUseCase,
                "manageRoundUseCase must not be null");
    }

    @PostMapping("/{numUnidad}/round/start")
    @Operation(
            summary = "Start a per-unit dispatch round",
            description = "Schedule window must be configured first via PUT /api/units/{numUnidad}/schedule. "
                    + "coordinateMode=MANUAL requires lat and lon. "
                    + "coordinateMode=AUTOMATIC is reserved for Phase 6 (TraccarCoordinateAdapter).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Round started — tick will dispatch on the next qualifying interval"),
            @ApiResponse(responseCode = "400", description = "Invalid or missing coordinates, or unsupported coordinateMode",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Unit not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409",
                    description = "Round already active / unit inactive / schedule not configured",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> startRound(
            @PathVariable String numUnidad,
            @Valid @RequestBody RoundStartRequest request) {

        manageRoundUseCase.startRound(
                numUnidad,
                request.coordinateMode(),
                request.lat(),
                request.lon());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{numUnidad}/round/stop")
    @Operation(summary = "Stop the active dispatch round for a unit")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Round stopped — no further dispatches will occur"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "No active round for this unit",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> stopRound(@PathVariable String numUnidad) {
        manageRoundUseCase.stopRound(numUnidad);
        return ResponseEntity.noContent().build();
    }
}
