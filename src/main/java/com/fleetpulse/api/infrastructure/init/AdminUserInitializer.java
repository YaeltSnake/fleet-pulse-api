package com.fleetpulse.api.infrastructure.init;

import com.fleetpulse.api.application.port.out.PasswordHasher;
import com.fleetpulse.api.application.port.out.UserRepository;
import com.fleetpulse.api.domain.model.Role;
import com.fleetpulse.api.domain.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.Objects;

@Slf4j
public class AdminUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final String adminUsername;
    private final String adminPassword;

    public AdminUserInitializer(UserRepository userRepository,
                                PasswordHasher passwordHasher,
                                String adminUsername,
                                String adminPassword) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher must not be null");
        this.adminUsername = Objects.requireNonNull(adminUsername, "adminUsername must not be null");
        this.adminPassword = Objects.requireNonNull(adminPassword, "adminPassword must not be null");
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {
        if(adminAlreadyExists()){
            log.info("ADMIN_ALREADY_EXISTS");
            return;
        }
        createInitialAdmin();
        log.info("ADMIN_SEEDED");
    }

    private boolean adminAlreadyExists() {
        return userRepository.existsByRole(Role.ADMIN);
    }


    private void createInitialAdmin(){
        if (adminPassword.isBlank()){
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD env var is required when no ADMIN user exists");
        }
        String hash = passwordHasher.encode(adminPassword);
        userRepository.save(new User(null, this.adminUsername, hash, Role.ADMIN, true));
    }


}
