package com.fleetpulse.api.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginRateLimitFilterTest {

    private LoginRateLimitFilter filter;

    @Mock
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter(5);
    }

    @Test
    void shouldAllowFiveAttemptsFromSameIp() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = loginRequest("10.0.0.1");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, chain);
            assertThat(resp.getStatus()).isNotEqualTo(429);
        }
        verify(chain, times(5)).doFilter(any(), any());
    }

    @Test
    void shouldReturn429OnSixthAttemptFromSameIp() throws Exception {
        for (int i = 0; i < 5; i++) {
            filter.doFilter(loginRequest("10.0.0.2"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(loginRequest("10.0.0.2"), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getContentType()).contains("application/problem+json");
        assertThat(blocked.getContentAsString()).contains("/errors/rate-limited");
        assertThat(blocked.getContentAsString()).contains("429");
    }

    @Test
    void shouldNotRateLimitDifferentIps() throws Exception {
        for (int i = 0; i < 5; i++) {
            filter.doFilter(loginRequest("10.0.0.3"), new MockHttpServletResponse(), chain);
        }

        // different IP — has its own fresh bucket
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(loginRequest("10.0.0.4"), resp, chain);

        assertThat(resp.getStatus()).isNotEqualTo(429);
    }

    @Test
    void shouldNotRateLimitNonLoginPaths() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/refresh");
            req.setServletPath("/api/auth/refresh");
            req.setRemoteAddr("10.0.0.5");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, chain);
            assertThat(resp.getStatus()).isNotEqualTo(429);
        }
        verify(chain, times(10)).doFilter(any(), any());
    }

    @Test
    void shouldNotRateLimitGetRequests() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/auth/login");
            req.setServletPath("/api/auth/login");
            req.setRemoteAddr("10.0.0.6");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, chain);
            assertThat(resp.getStatus()).isNotEqualTo(429);
        }
        verify(chain, times(10)).doFilter(any(), any());
    }

    private MockHttpServletRequest loginRequest(String remoteAddr) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setServletPath("/api/auth/login");
        req.setRemoteAddr(remoteAddr);
        return req;
    }
}
