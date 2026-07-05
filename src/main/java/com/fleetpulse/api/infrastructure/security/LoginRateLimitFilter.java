package com.fleetpulse.api.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String RATE_LIMITED_BODY =
            "{\"type\":\"/errors/rate-limited\",\"title\":\"Too many requests\",\"status\":429," +
            "\"detail\":\"Maximum 5 login attempts per minute exceeded. Try again later.\"}";

    private final int maxAttempts;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public LoginRateLimitFilter(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!isLoginRequest(request)) {
            chain.doFilter(request, response);
            return;
        }

        // FIXME-PROXY: getRemoteAddr() returns proxy IP when behind nginx.
        // Switch to X-Forwarded-For header when reverse proxy is configured in Phase 7 deploy.
        String clientIp = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(clientIp, ip -> buildBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("RATE_LIMITED ip={}", clientIp);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write(RATE_LIMITED_BODY);
        }
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && LOGIN_PATH.equals(request.getServletPath());
    }

    private Bucket buildBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(maxAttempts, Refill.intervally(maxAttempts, Duration.ofMinutes(1))))
                .build();
    }
}
