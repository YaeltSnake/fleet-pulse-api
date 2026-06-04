package com.fleetpulse.api.infrastructure.adapter.in.web.dto;

import com.fleetpulse.api.application.service.command.UpdateUserCommand;
import com.fleetpulse.api.domain.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8)
        String rawPassword,

        @NotNull
        Role role,

        boolean active
) {

    public UpdateUserCommand toCommand(Long id){
        return new UpdateUserCommand(
                id,
                this.username,
                this.rawPassword,
                this.role,
                this.active
        );
    }
}
