package com.fleetpulse.api.infrastructure.security;

import com.fleetpulse.api.application.port.out.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;


public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        return userRepository.findById(Long.decode(username))
                .map(user -> new org.springframework.security.core.userdetails.
                        User(user.getUsername(),
                        user.getPasswordHash(),
                        List.of(new SimpleGrantedAuthority(user.getRole().name()))))
                        .orElseThrow(() -> new UsernameNotFoundException(username));

    }
}
