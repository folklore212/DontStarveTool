package com.iccuu.general_web_backend.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Trusts X-User-Id header from internal Gateway.
 * When services are split behind an API Gateway, the Gateway validates
 * the JWT and injects X-User-Id header into downstream requests.
 * This filter populates Spring Security context from that header.
 */
@Component
public class GatewayTrustFilter extends OncePerRequestFilter {

    @Value("${gateway.trust.enabled:false}")
    private boolean enabled;

    @Value("${gateway.trust.internal-cidr:10.0.0.0/8,172.16.0.0/12,192.168.0.0/16}")
    private String internalCidr;

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLES = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader(HEADER_USER_ID);
        if (userId != null && isInternalRequest(request)) {
            String rolesHeader = request.getHeader(HEADER_USER_ROLES);
            List<SimpleGrantedAuthority> authorities = (rolesHeader != null && !rolesHeader.isBlank())
                    ? List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    : List.of();

            var auth = new UsernamePasswordAuthenticationToken(
                    Long.parseLong(userId), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }

    private boolean isInternalRequest(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        // Trust private/Docker network addresses
        return remoteAddr != null && (
                remoteAddr.startsWith("10.") ||
                remoteAddr.startsWith("172.") ||
                remoteAddr.startsWith("192.168.") ||
                remoteAddr.equals("127.0.0.1"));
    }
}
