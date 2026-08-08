package com.mssousa.authserver.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
    }

    @Test
    void devePermitirRequisicoesAbaixoDoLimiteParaRotaProtegida() throws Exception {
        HttpServletRequest request = requestFor("/api/auth/login", "10.0.0.1");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void deveBloquearAposEstourarOLimiteDeLogin() throws Exception {
        String ip = "10.0.0.2";
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            HttpServletRequest request = requestFor("/api/auth/login", ip);
            HttpServletResponse response = mock(HttpServletResponse.class);
            filter.doFilter(request, response, chain);
        }

        HttpServletRequest overLimitRequest = requestFor("/api/auth/login", ip);
        HttpServletResponse overLimitResponse = mock(HttpServletResponse.class);
        when(overLimitResponse.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilter(overLimitRequest, overLimitResponse, chain);

        verify(overLimitResponse).setStatus(429);
    }

    @Test
    void naoDeveLimitarRotasForaDaLista() throws Exception {
        HttpServletRequest request = requestFor("/actuator/health", "10.0.0.3");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 50; i++) {
            filter.doFilter(request, response, chain);
        }

        verify(chain, times(50)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void devIsolarLimitePorIp() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            filter.doFilter(requestFor("/api/auth/login", "10.0.0.4"), mock(HttpServletResponse.class), chain);
        }

        HttpServletRequest outroIp = requestFor("/api/auth/login", "10.0.0.5");
        HttpServletResponse response = mock(HttpServletResponse.class);
        filter.doFilter(outroIp, response, chain);

        verify(response, never()).setStatus(429);
    }

    private HttpServletRequest requestFor(String uri, String remoteAddr) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        return request;
    }
}
