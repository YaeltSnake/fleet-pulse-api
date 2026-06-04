package com.fleetpulse.api.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(

        @NotBlank(message = "Refresh Token is required")
        String refreshToken

) {
}
