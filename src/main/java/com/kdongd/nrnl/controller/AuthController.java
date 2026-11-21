package com.kdongd.nrnl.controller;

import com.kdongd.nrnl.dto.AuthTokens;
import com.kdongd.nrnl.dto.GoogleLoginRequest;
import com.kdongd.nrnl.dto.LoginRequest;
import com.kdongd.nrnl.dto.SignupRequest;
import com.kdongd.nrnl.dto.TokenResponse;
import com.kdongd.nrnl.exception.InvalidRefreshTokenException;
import com.kdongd.nrnl.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final AuthService authService;
    private final long refreshTokenExpirationMs;

    public AuthController(AuthService authService,
                          @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.authService = authService;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = authService.login(request);
        return respondWithTokens(tokens);
    }

    @PostMapping("/google")
    public ResponseEntity<TokenResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        AuthTokens tokens = authService.loginWithGoogle(request);
        return respondWithTokens(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest request) {
        String rawRefreshToken = resolveRefreshTokenCookie(request);
        AuthTokens tokens = authService.refresh(rawRefreshToken);
        return respondWithTokens(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String rawRefreshToken = resolveRefreshTokenCookieOrNull(request);
        if (rawRefreshToken != null) {
            authService.logout(rawRefreshToken);
        }

        ResponseCookie expiredCookie = buildRefreshTokenCookie("", 0);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .build();
    }

    private ResponseEntity<TokenResponse> respondWithTokens(AuthTokens tokens) {
        ResponseCookie cookie = buildRefreshTokenCookie(tokens.refreshToken(), refreshTokenExpirationMs / 1000);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(TokenResponse.of(tokens.accessToken()));
    }

    private ResponseCookie buildRefreshTokenCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/auth")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private String resolveRefreshTokenCookie(HttpServletRequest request) {
        String token = resolveRefreshTokenCookieOrNull(request);
        if (token == null) {
            throw new InvalidRefreshTokenException();
        }
        return token;
    }

    private String resolveRefreshTokenCookieOrNull(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
