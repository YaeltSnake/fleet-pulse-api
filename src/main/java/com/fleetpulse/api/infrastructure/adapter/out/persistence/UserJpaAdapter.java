package com.fleetpulse.api.infrastructure.adapter.out.persistence;

import com.fleetpulse.api.application.port.out.UserRepository;
import com.fleetpulse.api.domain.model.User;
import com.fleetpulse.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserJpaAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    public UserJpaAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        UserEntity saved = jpaRepository.save(toEntity(user));
        return toDomain(saved);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deactivateById(Long id) {
        jpaRepository.deactivateById(id);
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
                .id(user.getId())
                .username(user.getUsername())
                .passwordHash(user.getPasswordHash())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }
}
