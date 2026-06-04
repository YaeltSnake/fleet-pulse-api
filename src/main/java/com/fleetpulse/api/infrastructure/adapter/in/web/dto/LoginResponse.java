package com.fleetpulse.api.infrastructure.adapter.in.web.dto;

import com.fleetpulse.api.application.port.in.AuthUseCase;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Instant expiresAt
) {

    public static LoginResponse from(AuthUseCase.AuthResult result){
        return new LoginResponse(
                result.accessToken(),
                result.refreshToken(),
                result.expiresAt()
        );
    }
}
