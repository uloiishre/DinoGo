package com.dinogo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    @BeforeEach
    void clearSecurityContextBeforeTest() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenCreatesAuthentication() throws Exception {
        JwtTokenUtil jwtTokenUtil = mock(JwtTokenUtil.class);
        when(jwtTokenUtil.extractSubject("valid-token")).thenReturn("user@example.com");
        when(jwtTokenUtil.extractMemberId("valid-token")).thenReturn(6);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenUtil);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(new AuthenticatedMember(6, "user@example.com"));
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void invalidBearerTokenReturnsUnauthorized() throws Exception {
        JwtTokenUtil jwtTokenUtil = mock(JwtTokenUtil.class);
        when(jwtTokenUtil.extractSubject("invalid-token"))
                .thenThrow(new IllegalArgumentException("invalid token"));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenUtil);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void authEndpointSkipsJwtFilter() throws Exception {
        JwtTokenUtil jwtTokenUtil = mock(JwtTokenUtil.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenUtil);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
