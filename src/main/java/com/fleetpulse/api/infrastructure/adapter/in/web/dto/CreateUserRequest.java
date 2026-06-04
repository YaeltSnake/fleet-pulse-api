package com.fleetpulse.api.infrastructure.adapter.in.web.dto;

import com.fleetpulse.api.application.service.command.CreateUserCommand;
import com.fleetpulse.api.domain.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50)
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8)
        String rawPassword,

        @NotNull(message = "Role is required")
        Role role
) {

    public CreateUserCommand toCommand(){
        return new CreateUserCommand(
                this.username,
                this.rawPassword,
                this.role
        );
    }
}
