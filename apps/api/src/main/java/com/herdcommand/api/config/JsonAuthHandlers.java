package com.herdcommand.api.config;

import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.api.error.ErrorCodes;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.util.Locale;

@Component
public class JsonAuthHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    public JsonAuthHandlers(ObjectMapper objectMapper, MessageSource messageSource, LocaleResolver localeResolver) {
        this.objectMapper = objectMapper;
        this.messageSource = messageSource;
        this.localeResolver = localeResolver;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        write(response, request, HttpServletResponse.SC_UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "error.unauthorized");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        write(response, request, HttpServletResponse.SC_FORBIDDEN, ErrorCodes.FORBIDDEN, "error.forbidden");
    }

    private void write(
            HttpServletResponse response,
            HttpServletRequest request,
            int status,
            String code,
            String messageKey) throws IOException {
        Locale locale = localeResolver.resolveLocale(request);
        String message = messageSource.getMessage(messageKey, null, messageKey, locale);
        String correlationId = headerOrNew(request);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("X-Correlation-Id", correlationId);
        ApiError body = ApiError.of(code, message, request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private String headerOrNew(HttpServletRequest request) {
        String existing = request.getHeader("X-Correlation-Id");
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        return java.util.UUID.randomUUID().toString();
    }
}
