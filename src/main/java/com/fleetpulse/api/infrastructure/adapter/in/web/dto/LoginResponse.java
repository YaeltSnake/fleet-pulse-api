package com.fleetpulse.api.infrastructure.adapter.in.web.dto;

import com.fleetpulse.api.application.port.in.AuthUseCase;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        Instant refreshTokenExpiresAt
) {

    public static LoginResponse from(AuthUseCase.AuthResult result){
        return new LoginResponse(
                result.accessToken(),
                result.refreshTokenExpiresAt()
        );
    }
}
