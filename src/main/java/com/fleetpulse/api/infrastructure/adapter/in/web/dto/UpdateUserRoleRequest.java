package com.fleetpulse.api.infrastructure.adapter.in.web.dto;

import com.fleetpulse.api.domain.model.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull(message = "Role is required")
        Role role,

        boolean active
) {
}
