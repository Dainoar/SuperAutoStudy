package com.tihai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protects the operational task endpoints. With no configured token the
 * application is intentionally limited to loopback callers.
 */
@Component
public class OperatorAuthFilter extends OncePerRequestFilter {

    private final String apiToken;

    public OperatorAuthFilter(@Value("${security.api-token:}") String apiToken) {
        this.apiToken = apiToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/chaoxing")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAuthorized(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":201,\"subCode\":401,\"msg\":\"未授权\"}");
    }

    private boolean isAuthorized(HttpServletRequest request) {
        if (!StringUtils.hasText(apiToken)) {
            String remoteAddress = request.getRemoteAddr();
            return "127.0.0.1".equals(remoteAddress) || "::1".equals(remoteAddress)
                    || "0:0:0:0:0:0:0:1".equals(remoteAddress);
        }
        String authorization = request.getHeader("Authorization");
        String suppliedToken = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring("Bearer ".length())
                : request.getHeader("X-API-Token");
        return suppliedToken != null && MessageDigest.isEqual(
                apiToken.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8));
    }
}
