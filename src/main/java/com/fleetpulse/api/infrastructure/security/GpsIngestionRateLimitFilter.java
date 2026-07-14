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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttles GPS pushes per numUnidad so a misbehaving or misconfigured Traccar Client
 * (e.g. "highest" accuracy mode polling GPS multiple times per second) cannot flood
 * GpsPositionCache or the logs. One bucket per unit — a burst from one unit never
 * consumes another unit's quota. Throttled requests get a silent 200 (never 429):
 * Traccar Client doesn't retry gracefully on error codes, and this endpoint already
 * never reveals internal state to the device (same "no enumeration" rule as invalid
 * coordinates and unknown unit IDs).
 */
public class GpsIngestionRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GpsIngestionRateLimitFilter.class);
    private static final String GPS_POSITION_PATH = "/api/gps/position";

    private final int maxPushesPerWindow;
    private final Duration window;
    private final ConcurrentHashMap<String, Bucket> bucketsPerUnit = new ConcurrentHashMap<>();

    public GpsIngestionRateLimitFilter(int maxPushesPerWindow, Duration window) {
        this.maxPushesPerWindow = maxPushesPerWindow;
        this.window = window;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!isGpsPositionRequest(request)) {
            chain.doFilter(request, response);
            return;
        }

        String numUnidad = request.getParameter("id");
        if (!StringUtils.hasText(numUnidad)) {
            // No key to throttle on — let the controller reject it with its own 400.
            chain.doFilter(request, response);
            return;
        }

        Bucket bucket = bucketsPerUnit.computeIfAbsent(numUnidad, id -> buildBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.debug("GPS_PUSH_THROTTLED numUnidad={}", numUnidad);
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    private boolean isGpsPositionRequest(HttpServletRequest request) {
        return GPS_POSITION_PATH.equals(request.getServletPath());
    }

    private Bucket buildBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(maxPushesPerWindow, Refill.intervally(maxPushesPerWindow, window)))
                .build();
    }
}
