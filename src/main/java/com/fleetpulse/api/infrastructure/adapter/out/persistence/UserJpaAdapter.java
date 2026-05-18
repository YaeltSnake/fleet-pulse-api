package com.fleetpulse.api.infrastructure.adapter.out.persistence;

import com.fleetpulse.api.application.port.out.UserRepository;
import com.fleetpulse.api.domain.exception.UserNotFoundException;
import com.fleetpulse.api.domain.model.User;
import com.fleetpulse.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class UserJpaAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserJpaAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        UserEntity saved = jpaRepository.save(toEntity(user));
        return toDomain(saved);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void deactivateByUsername(String username) {
        UserEntity entity = jpaRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        entity.setActive(false);
        jpaRepository.save(entity);

    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    private User toDomain(UserEntity entity){
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.isActive()
        );
    }

    private UserEntity toEntity(User user){
        return UserEntity.builder()
                .username(user.getUsername())
                .passwordHash(user.getPasswordHash())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }
}
