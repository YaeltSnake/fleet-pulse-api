package com.fleetpulse.api.infrastructure.security;

import com.fleetpulse.api.application.port.out.TokenBlacklist;
import com.fleetpulse.api.application.port.out.TokenService;
import com.fleetpulse.api.domain.exception.ExternalServiceUnavailableException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log =  LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final TokenBlacklist tokenBlacklist;

    public JwtAuthenticationFilter(TokenService tokenService,
                                   TokenBlacklist tokenBlacklist) {
        this.tokenService = tokenService;
        this.tokenBlacklist = tokenBlacklist;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String jwt = getJwtFromRequest(request);

        if (!StringUtils.hasText(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!tokenService.isTokenValid(jwt)) {
            SecurityContextHolder.clearContext();

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid, expired token");
            return;
        }

        try {
            if (tokenBlacklist.isBlacklisted(jwt)) {
                SecurityContextHolder.clearContext();

                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been revoked");
                return;
            }
        } catch (ExternalServiceUnavailableException e) {

            log.warn("Token blacklist service unavailable for request to {}", request.getRequestURI(), e);

            SecurityContextHolder.clearContext();

            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Redis service unavailable");
            return;
        }



        Long userId = tokenService.extractUserId(jwt);
        String role = tokenService.extractRole(jwt);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);


    }

    private String getJwtFromRequest(HttpServletRequest request){

        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)){
            return bearerToken.substring(BEARER_PREFIX.length()).trim();
        }

        return null;

    }

}
