package com.fleetpulse.api.infrastructure.adapter.in.web.dto;

import com.fleetpulse.api.application.service.command.LoginCommand;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password

) {

    public LoginCommand toCommand(){
        return new LoginCommand(
                this.username,
                this.password
        );
    }
}
