package com.kdongd.nrnl.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdongd.nrnl.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        TokenStatus tokenStatus = (TokenStatus) request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_ATTRIBUTE);

        String errorCode;
        String message;
        if (tokenStatus == TokenStatus.EXPIRED) {
            errorCode = "TOKEN_EXPIRED";
            message = "인증이 만료되었습니다.";
        } else if (tokenStatus == TokenStatus.INVALID) {
            errorCode = "TOKEN_INVALID";
            message = "유효하지 않은 인증 정보입니다.";
        } else {
            errorCode = "TOKEN_MISSING";
            message = "인증이 필요합니다.";
        }

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                message,
                List.of(),
                errorCode
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
