package com.kdongd.nrnl.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // CustomAuthenticationEntryPoint가 "왜" 인증에 실패했는지(만료/무효) 읽어갈 수 있게
    // 요청 attribute로 남겨두는 키. 토큰이 아예 없는 경우엔 이 attribute 자체가 없다 -> TOKEN_MISSING으로 구분.
    public static final String TOKEN_ERROR_ATTRIBUTE = "jwt_token_error";

    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            TokenStatus status = jwtTokenProvider.validate(token);
            if (status == TokenStatus.VALID) {
                Long userId = jwtTokenProvider.getUserId(token);
                var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                request.setAttribute(TOKEN_ERROR_ATTRIBUTE, status);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }
        return null;
    }
}
