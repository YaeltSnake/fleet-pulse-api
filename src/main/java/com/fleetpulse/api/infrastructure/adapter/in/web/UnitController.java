package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fleetpulse.api.application.port.in.ConfigureScheduleUseCase;
import com.fleetpulse.api.application.port.in.ManageRoundUseCase;
import com.fleetpulse.api.application.port.in.ManageUnitUseCase;
import com.fleetpulse.api.domain.exception.UnitNotFoundException;
import com.fleetpulse.api.domain.model.CoordinateMode;
import com.fleetpulse.api.domain.model.Unit;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.ScheduleUpdateRequest;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.UnitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Tag(name = "Units", description = "Fleet unit status and schedule management")
@RestController
@RequestMapping("/api/units")
public class UnitController {

    private final ManageUnitUseCase manageUnitUseCase;
    private final ConfigureScheduleUseCase configureScheduleUseCase;
    private final ManageRoundUseCase manageRoundUseCase;

    public UnitController(ManageUnitUseCase manageUnitUseCase,
                          ConfigureScheduleUseCase configureScheduleUseCase,
                          ManageRoundUseCase manageRoundUseCase) {
        this.manageUnitUseCase = Objects.requireNonNull(manageUnitUseCase,
                "manageUnitUseCase must not be null");
        this.configureScheduleUseCase = Objects.requireNonNull(configureScheduleUseCase,
                "configureScheduleUseCase must not be null");
        this.manageRoundUseCase = Objects.requireNonNull(manageRoundUseCase,
                "manageRoundUseCase must not be null");
    }

    @GetMapping
    @Operation(summary = "List all fleet units")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unit list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<UnitResponse>> listUnits() {
        List<UnitResponse> responses = manageUnitUseCase.listAllUnits().stream()
                .map(unit -> UnitResponse.from(
                        unit,
                        manageRoundUseCase.isRoundActive(unit.getNumUnidad()),
                        manageRoundUseCase.getRoundCoordinateMode(unit.getNumUnidad())))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{numUnidad}")
    @Operation(summary = "Get a single unit by its identifier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unit found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Unit not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UnitResponse> getUnit(@PathVariable String numUnidad) {
        Unit unit = manageUnitUseCase.findByNumUnidad(numUnidad)
                .orElseThrow(() -> new UnitNotFoundException(numUnidad));
        boolean roundActive = manageRoundUseCase.isRoundActive(numUnidad);
        CoordinateMode currentMode = manageRoundUseCase.getRoundCoordinateMode(numUnidad);
        return ResponseEntity.ok(UnitResponse.from(unit, roundActive, currentMode));
    }

    @PutMapping("/{numUnidad}/activate")
    @Operation(summary = "Activate a unit — enables it for dispatch")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unit activated"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Unit not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UnitResponse> activateUnit(@PathVariable String numUnidad) {
        Unit updated = manageUnitUseCase.activateUnit(numUnidad);
        boolean roundActive = manageRoundUseCase.isRoundActive(numUnidad);
        CoordinateMode currentMode = manageRoundUseCase.getRoundCoordinateMode(numUnidad);
        return ResponseEntity.ok(UnitResponse.from(updated, roundActive, currentMode));
    }

    @PutMapping("/{numUnidad}/deactivate")
    @Operation(summary = "Deactivate a unit — removes it from dispatch")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unit deactivated"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Unit not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UnitResponse> deactivateUnit(@PathVariable String numUnidad) {
        Unit updated = manageUnitUseCase.deactivateUnit(numUnidad);
        boolean roundActive = manageRoundUseCase.isRoundActive(numUnidad);
        CoordinateMode currentMode = manageRoundUseCase.getRoundCoordinateMode(numUnidad);
        return ResponseEntity.ok(UnitResponse.from(updated, roundActive, currentMode));
    }

    @PutMapping("/{numUnidad}/schedule")
    @Operation(summary = "Update dispatch schedule",
            description = "USER role can only update if unit.horarioFijo == false. ADMIN always proceeds.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schedule updated"),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "USER cannot modify a fixed schedule",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Unit not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "horaInicio must be before horaFin",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UnitResponse> updateSchedule(
            @PathVariable String numUnidad,
            @Valid @RequestBody ScheduleUpdateRequest request,
            Authentication authentication) {

        Unit unit = manageUnitUseCase.findByNumUnidad(numUnidad)
                .orElseThrow(() -> new UnitNotFoundException(numUnidad));

        boolean isUser = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("USER"));

        if (isUser && unit.isHorarioFijo()) {
            throw new AccessDeniedException("USER role cannot modify a fixed schedule");
        }

        Unit updated = configureScheduleUseCase.updateSchedule(
                numUnidad, request.horarioFijo(), request.horaInicio(), request.horaFin());

        boolean roundActive = manageRoundUseCase.isRoundActive(numUnidad);
        CoordinateMode currentMode = manageRoundUseCase.getRoundCoordinateMode(numUnidad);
        return ResponseEntity.ok(UnitResponse.from(updated, roundActive, currentMode));
    }
}
