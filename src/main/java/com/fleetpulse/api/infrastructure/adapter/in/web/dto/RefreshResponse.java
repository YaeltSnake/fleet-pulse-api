package com.fleetpulse.api.infrastructure.adapter.in.web.dto;

import com.fleetpulse.api.application.port.in.AuthUseCase;

import java.time.Instant;

public record RefreshResponse(

        String accessToken,

        Instant refreshTokenExpiresAt
) {

    public static RefreshResponse from(AuthUseCase.AuthResult result){

        return new RefreshResponse(
                result.accessToken(),
                result.refreshTokenExpiresAt()
        );
    }
}
