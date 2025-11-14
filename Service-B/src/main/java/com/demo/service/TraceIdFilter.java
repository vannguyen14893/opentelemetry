package com.demo.service;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TraceIdFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);
    private final Tracer tracer;

    public TraceIdFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof HttpServletResponse httpResponse) {
            // Lấy trace ID hiện tại
            String traceId = tracer.currentSpan() != null
                    ? tracer.currentSpan().context().traceId()
                    : "no-trace-id";

            // Thêm trace ID vào response header
            httpResponse.setHeader("X-Trace-Id", traceId);

            log.info("Request processed with Trace ID: {}", traceId);
        }

        chain.doFilter(request, response);
    }
}
