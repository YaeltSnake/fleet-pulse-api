package com.fleetpulse.api.application.service;

import com.fleetpulse.api.application.port.in.CreateUserCommand;
import com.fleetpulse.api.application.port.in.UpdateUserCommand;
import com.fleetpulse.api.application.port.in.UserManagementUseCase;
import com.fleetpulse.api.application.port.out.PasswordHasher;
import com.fleetpulse.api.application.port.out.UserRepository;
import com.fleetpulse.api.domain.exception.UserNotFoundException;
import com.fleetpulse.api.domain.exception.UsernameAlreadyExistsException;
import com.fleetpulse.api.domain.model.User;

import java.util.List;

public class UserManagementService implements UserManagementUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserManagementService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    //UsernameAlreadyExistsException
    @Override
    public User createUser(CreateUserCommand command) {
        if (userRepository.existsByUsername(command.username())){
            throw new UsernameAlreadyExistsException(command.username());
        }
        String hash = passwordHasher.encode(command.rawPassword());
        return userRepository.save(new User(null, command.username(), hash, command.role(), true));
    }

    @Override
    public User updateUser(UpdateUserCommand command) {
        User existing = userRepository.findById(command.id())
                .orElseThrow(() -> new UserNotFoundException(command.id()));

        if (!existing.getUsername().equals(command.username())
                && userRepository.existsByUsername(command.username())) {
            throw new UsernameAlreadyExistsException(command.username());
        }

        String hash = command.rawPassword() != null
                ? passwordHasher.encode(command.rawPassword())
                : existing.getPasswordHash();

        return userRepository.save(new User(existing.getId(), command.username(), hash, command.role(), command.active()));
    }

    @Override
    public void deactivateUser(Long id) {

        userRepository.deactivateById(id);
    }

    @Override
    public User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
}
