package com.fleetpulse.api.infrastructure.config;
import com.fleetpulse.api.infrastructure.security.JwtAuthenticationFilter;
import com.fleetpulse.api.infrastructure.security.LoginRateLimitFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_ADMIN = "ADMIN";

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;
    /**
     * Encoder de passwords. Spring Security lo usa internamente
     * cuando inyectamos AuthenticationManager o cuando comparamos
     * passwords en cualquier adapter.
     */
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    /**
     * Cadena de filtros de seguridad. Stateless, sin CSRF, con CORS
     * permisivo para desarrollo, y matriz de autorización basada en
     * authorities (no roles) para alinearse con JwtAuthenticationFilter.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            LoginRateLimitFilter loginRateLimitFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()

                        // FIXME-SEC (Phase 6): Traccar endpoint. Currently permitAll() for OsmAnd
                        // push protocol. Must add API-key validation or IP whitelist before v1.1.0.
                        // FIXME-SEC: /api/gps/position is fully public — no IP whitelist.
                        // Add IP restriction for known Traccar devices in Phase 6 (v1.1.0).
                        .requestMatchers(HttpMethod.GET,  "/api/gps/position").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/gps/position").permitAll()

                        // Unit schedule and pulse operations (USER + ADMIN) — specific rules before broad PUT /**
                        .requestMatchers(HttpMethod.PUT,  "/api/units/*/schedule").hasAnyAuthority(ROLE_ADMIN, ROLE_USER)
                        .requestMatchers(HttpMethod.POST, "/api/units/*/pulse/force").hasAnyAuthority(ROLE_ADMIN, ROLE_USER)
                        .requestMatchers(HttpMethod.GET,  "/api/units/*/pulse/test").hasAnyAuthority(ROLE_ADMIN, ROLE_USER)
                        .requestMatchers(HttpMethod.POST, "/api/units/*/pulse/test-manual").hasAnyAuthority(ROLE_ADMIN, ROLE_USER)

                        // Logout requires valid token (any authenticated role)
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").hasAnyAuthority(ROLE_ADMIN, ROLE_USER)

                        // Users (ADMIN-only)
                        .requestMatchers(HttpMethod.GET,  "/api/users").hasAuthority(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/users").hasAuthority(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT,  "/api/users/**").hasAuthority(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAuthority(ROLE_ADMIN)

                        // Units — GET (ADMIN + USER); PUT sub-resources before broad PUT /**
                        .requestMatchers(HttpMethod.GET,  "/api/units").hasAnyAuthority(ROLE_ADMIN, ROLE_USER)
                        .requestMatchers(HttpMethod.GET,  "/api/units/*").hasAnyAuthority(ROLE_ADMIN, ROLE_USER)
                        .requestMatchers(HttpMethod.PUT,  "/api/units/*/activate").hasAuthority(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT,  "/api/units/*/deactivate").hasAuthority(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/units/*/round/start").hasAnyAuthority(ROLE_ADMIN, ROLE_USER)
                        .requestMatchers(HttpMethod.POST, "/api/units/*/round/stop").hasAnyAuthority(ROLE_ADMIN, ROLE_USER)
                        .requestMatchers(HttpMethod.POST, "/api/units").hasAuthority(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT,  "/api/units/**").hasAuthority(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/units/**").hasAuthority(ROLE_ADMIN)

                        // Pulse log — readable by ADMIN and USER (dashboard)
                        .requestMatchers(HttpMethod.GET, "/api/pulse-log").hasAnyAuthority(ROLE_ADMIN, ROLE_USER)

                        // Secure default
                        .anyRequest().denyAll()
                )
                .exceptionHandling(
                        ex -> ex
                                .authenticationEntryPoint((request, response, authException) ->
                                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loginRateLimitFilter, JwtAuthenticationFilter.class)
                .build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(allowedOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
