package com.portabilidade.api.config.observability;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String HEADER_NAME = "X-Correlation-Id";
	public static final String MDC_KEY = "correlationId";
	private static final Pattern VALID_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String correlationId = correlationId(request.getHeader(HEADER_NAME));
		MDC.put(MDC_KEY, correlationId);
		try {
			response.setHeader(HEADER_NAME, correlationId);
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(MDC_KEY);
		}
	}

	private String correlationId(String requestedId) {
		return requestedId != null && VALID_CORRELATION_ID.matcher(requestedId).matches()
				? requestedId
				: UUID.randomUUID().toString();
	}
}
