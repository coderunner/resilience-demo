package com.inf5190.resilience;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class HttpFilter implements Filter {
    private Logger logger = LoggerFactory.getLogger(HttpFilter.class);

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;

        String correlationId = httpRequest.getHeader(CorrelationId.CORRELATION_ID);
        if (correlationId == null || correlationId.length() == 0) {
            correlationId = UUID.randomUUID().toString();
        }

        httpRequest.setAttribute(CorrelationId.CORRELATION_ID, correlationId);
        MDC.put(CorrelationId.CORRELATION_ID, correlationId);

        logger.info("Request {} for URL {}", httpRequest.getMethod(), httpRequest.getRequestURI());
        chain.doFilter(request, response);

        MDC.remove(CorrelationId.CORRELATION_ID);
    }
}