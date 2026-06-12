package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fleetpulse.api.application.port.in.AuthUseCase;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Login, token refresh, and logout operations")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @Operation(summary = "Authenticate user", description = "Validates credentials and returns JWT access and refresh tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request){

        AuthUseCase.AuthResult result = authUseCase.login(request.toCommand());

        return ResponseEntity.status(HttpStatus.OK).body(LoginResponse.from(result));

    }

    @Operation(summary = "Refresh access token", description = "Issues a new access token using a valid refresh token. Old refresh token is revoked.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalid, expired, or revoked",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })


    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@RequestBody @Valid RefreshRequest request){

        AuthUseCase.AuthResult result = authUseCase.refresh(request.refreshToken());

        return ResponseEntity.status(HttpStatus.OK).body(RefreshResponse.from(result));

    }


    @Operation(summary = "Logout user", description = "Blacklists the access token in Redis and revokes the refresh token in DB")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authorization token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(
            @RequestBody @Valid LogoutRequest request,
            @RequestHeader("Authorization") String authHeader
            ){
        String accessToken = authHeader.substring(7);
        authUseCase.logout(accessToken, request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
