package com.fleetpulse.api.application.port.out;

import com.fleetpulse.api.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);
    // FIXME: findById uses database surrogate key. Migrate to business identifier
    // (username or UUID) before Phase 6. See ADR-003
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    void deactivateById(Long id);
    boolean existsByUsername(String username);

}
