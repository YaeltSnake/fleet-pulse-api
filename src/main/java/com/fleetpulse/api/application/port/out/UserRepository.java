package com.fleetpulse.api.application.port.out;

import com.fleetpulse.api.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    void deactivateById(Long id);
    boolean existsByUsername(String username);

}
