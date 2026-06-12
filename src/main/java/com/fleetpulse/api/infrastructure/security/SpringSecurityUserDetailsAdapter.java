package com.fleetpulse.api.infrastructure.security;

import com.fleetpulse.api.application.port.out.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

/**
 * Satisfies Spring Security's auto-configuration requirement for a UserDetailsService bean.
 * NOTE: This class is NOT called by JwtAuthenticationFilter — the filter reads claims
 * directly from the JWT. This adapter exists solely to suppress Spring Boot's
 * UserDetailsService auto-configuration. See ADR-006.
 */
public class SpringSecurityUserDetailsAdapter implements UserDetailsService {

    private final UserRepository userRepository;

    public SpringSecurityUserDetailsAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findById(Long.decode(username))
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPasswordHash(),
                        List.of(new SimpleGrantedAuthority(user.getRole().name()))))
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }


}
