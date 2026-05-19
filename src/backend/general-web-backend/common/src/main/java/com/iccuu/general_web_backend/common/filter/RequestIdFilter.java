package com.iccuu.general_web_backend.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_ATTRIBUTE = "requestId";
    private static final String RESPONSE_HEADER_REQUEST_ID = "X-Request-Id";
    private static final String CLIENT_IP_CHAIN_ATTRIBUTE = "clientIpChain";
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(RESPONSE_HEADER_REQUEST_ID, requestId);

        String clientIpChain = extractClientIpChain(request);
        request.setAttribute(CLIENT_IP_CHAIN_ATTRIBUTE, clientIpChain);

        filterChain.doFilter(request, response);
    }

    private String extractClientIpChain(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor;
        }
        return request.getRemoteAddr();
    }
}
