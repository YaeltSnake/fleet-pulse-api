package com.fleetpulse.api.application.port.in;

import com.fleetpulse.api.application.service.command.CreateUserCommand;
import com.fleetpulse.api.application.service.command.UpdateUserCommand;
import com.fleetpulse.api.domain.model.Role;
import com.fleetpulse.api.domain.model.User;

import java.util.List;

public interface UserManagementUseCase {
    User createUser(CreateUserCommand command);
    User updateUser(UpdateUserCommand command);
    User updateRoleAndActive(Long id, Role role, boolean active);
    void deactivateUser(Long id);
    User findUser(Long id);
    List<User> findAllUsers(int page, int size);
    long countActiveUsers();
    long countAllUsers();
}
