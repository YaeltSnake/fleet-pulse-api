package com.fleetpulse.api.infrastructure.adapter.in.web.dto;

import com.fleetpulse.api.domain.model.Role;
import com.fleetpulse.api.domain.model.User;

public record UserResponse(
        Long id,
        String username,
        Role role,
        Boolean active
) {

    public static UserResponse from(User user){
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.isActive()
        );
    }
}
