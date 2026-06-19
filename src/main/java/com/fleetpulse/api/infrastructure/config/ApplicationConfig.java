package com.fleetpulse.api.infrastructure.config;

import com.fleetpulse.api.application.port.in.SendPulseUseCase;
import com.fleetpulse.api.application.port.out.*;
import com.fleetpulse.api.application.service.AuthService;
import com.fleetpulse.api.application.service.PulseOrchestrationService;
import com.fleetpulse.api.application.service.UserManagementService;
import com.fleetpulse.api.domain.model.FleetConstants;
import com.fleetpulse.api.infrastructure.adapter.out.soap.QSolutionsSoapAdapter;
import com.fleetpulse.api.infrastructure.init.AdminUserInitializer;
import com.fleetpulse.api.infrastructure.security.BcryptPasswordHasherAdapter;
import com.fleetpulse.api.infrastructure.security.JwtAuthenticationFilter;
import com.fleetpulse.api.infrastructure.security.JwtService;
import com.fleetpulse.api.infrastructure.security.SpringSecurityUserDetailsAdapter;
import jakarta.xml.ws.BindingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.tempuri.ReceiveGPSInfo;
import org.tempuri.ReceiveGPSInfoSoap;

import java.net.URL;
import java.time.Clock;

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
    public SpringSecurityUserDetailsAdapter userDetailsService(UserRepository userRepository){
        return new SpringSecurityUserDetailsAdapter(userRepository);
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

    @Bean
    public ReceiveGPSInfoSoap receiveGpsInfoSoap(
            @Value("${qsolutions.endpoint}") String endpoint,
            @Value("${qsolutions.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${qsolutions.read-timeout-ms}") int readTimeoutMs) {

        // WSDL loaded from classpath — not from file:// hardcoded by wsimport
        URL wsdlUrl = ReceiveGPSInfo.class.getResource("/wsdl/ReceiveGPSInfo.wsdl");

        ReceiveGPSInfo service = new ReceiveGPSInfo(wsdlUrl);
        ReceiveGPSInfoSoap port = service.getReceiveGPSInfoSoap();

        // Cast to BindingProvider to configure network channel
        // The proxy implements both ReceiveGPSInfoSoap and BindingProvider at runtime
        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        bp.getRequestContext().put("com.sun.xml.ws.connect.timeout", connectTimeoutMs);
        bp.getRequestContext().put("com.sun.xml.ws.request.timeout", readTimeoutMs);

        // NOT thread-safe — JAX-WS port proxy shares a single requestContext (HashMap).
        // Safe for Phase 4: scheduler dispatches units sequentially (single thread).
        // REVISIT if concurrent scheduling introduced in Phase 8+: use ThreadLocal or prototype scope.
        return port;
    }

    @Bean
    public PulseSender qSolutionsSoapAdapter(
            ReceiveGPSInfoSoap receiveGpsInfoSoap,
            @Value("${qsolutions.username}") String username,
            @Value("${qsolutions.password}") String password,
            @Value("${qsolutions.proveedor}") String proveedor) {
        return new QSolutionsSoapAdapter(receiveGpsInfoSoap, username, password, proveedor);
    }

    @Bean
    public Clock clock() {
        return Clock.system(FleetConstants.FLEET_TIMEZONE);
    }

    @Bean
    public PulseOrchestrationService pulseOrchestrationService(
            UnitRepository unitRepository,
            GpsCoordinateProvider gpsProvider,
            PulseSender pulseSender,
            @Value("${qsolutions.tracking-number}") String defaultTrackingNumber,
            Clock clock) {
        return new PulseOrchestrationService(unitRepository, gpsProvider, pulseSender, defaultTrackingNumber, clock);
    }

}
