package com.fleetpulse.api.application.port.in;

import com.fleetpulse.api.domain.model.User;

import java.util.List;

public interface UserManagementUseCase {
    User createUser(CreateUserCommand command);
    User updateUser(UpdateUserCommand command);
    void deactivateUser(Long id);
    User findUser(Long id);
    List<User> findAllUsers();
}
