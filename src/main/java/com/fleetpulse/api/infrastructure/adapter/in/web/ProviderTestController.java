package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fleetpulse.api.application.port.in.TestProviderUseCase;
import com.fleetpulse.api.domain.model.GpsReading;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.ManualCoordinateTestRequest;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.ProviderTestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Tag(name = "Provider Test", description = "GPS coordinate provider dry-run — never calls QSolutions")
@RestController
@RequestMapping("/api/units")
public class ProviderTestController {

    private final TestProviderUseCase testProviderUseCase;

    public ProviderTestController(TestProviderUseCase testProviderUseCase) {
        this.testProviderUseCase = Objects.requireNonNull(testProviderUseCase,
                "testProviderUseCase must not be null");
    }

    @GetMapping("/{numUnidad}/pulse/test")
    @Operation(
            summary = "Test the configured GPS provider for a unit",
            description = "Returns the coordinates from the configured GpsCoordinateProvider. "
                    + "Never dispatches to QSolutions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coordinates returned from configured provider"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Unit not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Configured GPS provider unavailable for this unit",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ProviderTestResponse> testProvider(@PathVariable String numUnidad) {
        GpsReading reading = testProviderUseCase.testProvider(numUnidad);
        return ResponseEntity.ok(ProviderTestResponse.from(reading));
    }

    @PostMapping("/{numUnidad}/pulse/test-manual")
    @Operation(
            summary = "Test dispatch path with caller-supplied coordinates",
            description = "Builds a GpsReading from the provided lat/lon without calling the configured "
                    + "GpsCoordinateProvider or QSolutions. Useful for verifying coordinate validation "
                    + "and response format with known safe values.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "GpsReading constructed from supplied coordinates"),
            @ApiResponse(responseCode = "400", description = "lat or lon is null, out of range, or (0,0)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Unit not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ProviderTestResponse> testWithManualCoordinates(
            @PathVariable String numUnidad,
            @Valid @RequestBody ManualCoordinateTestRequest request) {

        GpsReading reading = testProviderUseCase.testProviderWithOverride(
                numUnidad, request.lat(), request.lon());

        return ResponseEntity.ok(ProviderTestResponse.from(reading));
    }
}
