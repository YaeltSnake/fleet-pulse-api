package com.fleetpulse.api.infrastructure.config;

import com.fleetpulse.api.application.port.out.*;
import com.fleetpulse.api.application.service.AuthService;
import com.fleetpulse.api.application.service.UserManagementService;
import com.fleetpulse.api.infrastructure.init.AdminUserInitializer;
import com.fleetpulse.api.infrastructure.security.BcryptPasswordHasherAdapter;
import com.fleetpulse.api.infrastructure.security.JwtAuthenticationFilter;
import com.fleetpulse.api.infrastructure.security.JwtService;
import com.fleetpulse.api.infrastructure.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class ApplicationConfig {

    @Bean
    public PasswordHasher passwordHasher(PasswordEncoder passwordEncoder){
        return new BcryptPasswordHasherAdapter(passwordEncoder);
    }

    @Bean
    public TokenService tokenService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiry-seconds}") long accessExpirySeconds,
            @Value("${jwt.refresh-expiry-seconds}") long refreshExpirySeconds
    ){
        return new JwtService(secret, accessExpirySeconds, refreshExpirySeconds);
    }


    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(TokenService tokenService, TokenBlacklist tokenBlacklist){
        return new JwtAuthenticationFilter(tokenService, tokenBlacklist);
    }

    @Bean
    public UserDetailsServiceImpl userDetailsService(UserRepository userRepository){
        return new UserDetailsServiceImpl(userRepository);
    }

    @Bean
    public AuthService authService(
            PasswordHasher passwordHasher,
            TokenBlacklist tokenBlacklist,
            TokenService tokenService,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository
            )
    {
        return new AuthService(passwordHasher, tokenBlacklist, tokenService, userRepository, refreshTokenRepository);
    }

    @Bean
    public UserManagementService userManagementService(UserRepository userRepository, PasswordHasher passwordHasher){
        return new UserManagementService(userRepository, passwordHasher);
    }

    @Bean
    public AdminUserInitializer adminUserInitializer(
            UserRepository userRepository,
            PasswordHasher hasher,
            @Value("${app.initial-admin-username}") String adminUsername,
            @Value("${app.initial-admin-password}") String adminPassword
    ){
        return new AdminUserInitializer(userRepository, hasher, adminUsername, adminPassword);
    }

}
