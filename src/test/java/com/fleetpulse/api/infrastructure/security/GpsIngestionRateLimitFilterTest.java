package com.fleetpulse.api.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GpsIngestionRateLimitFilterTest {

    private GpsIngestionRateLimitFilter filter;

    @Mock
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new GpsIngestionRateLimitFilter(1, Duration.ofSeconds(10));
    }

    @Test
    void shouldAllowFirstPushFromUnit() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(gpsRequest("Peugeot"), resp, chain);

        verify(chain, times(1)).doFilter(any(), any());
        assertThat(resp.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldThrottleSecondPushWithinWindowFromSameUnit() throws Exception {
        filter.doFilter(gpsRequest("Peugeot"), new MockHttpServletResponse(), chain);

        MockHttpServletResponse throttled = new MockHttpServletResponse();
        filter.doFilter(gpsRequest("Peugeot"), throttled, chain);

        // chain still invoked only once — the throttled request never reached the controller
        verify(chain, times(1)).doFilter(any(), any());
        // silent 200 — never reveals throttling to the device
        assertThat(throttled.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldNotThrottleDifferentUnits() throws Exception {
        filter.doFilter(gpsRequest("Peugeot"), new MockHttpServletResponse(), chain);

        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(gpsRequest("Kangoo"), resp, chain);

        // Peugeot's throttled bucket never affects Kangoo's independent bucket
        verify(chain, times(2)).doFilter(any(), any());
        assertThat(resp.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldShareBucketAcrossGetAndPostForSameUnit() throws Exception {
        filter.doFilter(gpsRequest("Peugeot", "GET"), new MockHttpServletResponse(), chain);

        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(gpsRequest("Peugeot", "POST"), resp, chain);

        // same numUnidad regardless of HTTP method — POST here is throttled
        verify(chain, times(1)).doFilter(any(), any());
        assertThat(resp.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldNotThrottleNonGpsPaths() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/units");
            req.setServletPath("/api/units");
            req.setParameter("id", "Peugeot");
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }
        verify(chain, times(5)).doFilter(any(), any());
    }

    @Test
    void shouldPassThroughWhenIdParamMissing() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/gps/position");
        req.setServletPath("/api/gps/position");
        // no "id" param — nothing to key a bucket on

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        // passes through — the controller's own validation rejects blank id with 400
        verify(chain, times(1)).doFilter(any(), any());
    }

    private MockHttpServletRequest gpsRequest(String numUnidad) {
        return gpsRequest(numUnidad, "GET");
    }

    private MockHttpServletRequest gpsRequest(String numUnidad, String method) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, "/api/gps/position");
        req.setServletPath("/api/gps/position");
        req.setParameter("id", numUnidad);
        return req;
    }
}
