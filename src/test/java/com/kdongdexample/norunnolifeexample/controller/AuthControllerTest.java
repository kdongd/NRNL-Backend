
package com.kdongdexample.norunnolifeexample.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdongdexample.norunnolifeexample.config.SecurityConfig;
import com.kdongdexample.norunnolifeexample.domain.ExperienceLevel;
import com.kdongdexample.norunnolifeexample.domain.Gender;
import com.kdongdexample.norunnolifeexample.domain.WorkoutType;
import com.kdongdexample.norunnolifeexample.dto.AuthTokens;
import com.kdongdexample.norunnolifeexample.dto.GoogleLoginRequest;
import com.kdongdexample.norunnolifeexample.dto.LoginRequest;
import com.kdongdexample.norunnolifeexample.dto.SignupRequest;
import com.kdongdexample.norunnolifeexample.dto.UserProfileRequest;
import com.kdongdexample.norunnolifeexample.exception.EmailAlreadyExistsException;
import com.kdongdexample.norunnolifeexample.exception.InvalidCredentialsException;
import com.kdongdexample.norunnolifeexample.exception.InvalidGoogleTokenException;
import com.kdongdexample.norunnolifeexample.exception.InvalidRefreshTokenException;
import com.kdongdexample.norunnolifeexample.security.CustomAuthenticationEntryPoint;
import com.kdongdexample.norunnolifeexample.security.JwtTokenProvider;
import com.kdongdexample.norunnolifeexample.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, CustomAuthenticationEntryPoint.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("signup - 유효한 요청이면 201")
    void signup_returns201_whenRequestIsValid() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest(
                                        "test@test.com",
                                        "password1234",
                                        "테스트유저",
                                        new UserProfileRequest(
                                                Gender.MAN,
                                                28,
                                                175.0,
                                                70.0,
                                                ExperienceLevel.INTERMEDIATE,
                                                Set.of(WorkoutType.RUNNING),
                                                "10km 완주"
                                        )
                                )
                        )))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("signup - 이미 존재하는 이메일이면 409")
    void signup_returns409_whenEmailAlreadyExists() throws Exception {
        willThrow(new EmailAlreadyExistsException("test@test.com")).given(authService).signup(any());

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest(
                                        "test@test.com",
                                        "password1234",
                                        "테스트유저",
                                        new UserProfileRequest(
                                                Gender.MAN,
                                                28,
                                                175.0,
                                                70.0,
                                                ExperienceLevel.INTERMEDIATE,
                                                Set.of(WorkoutType.RUNNING),
                                                "10km 완주"
                                        )
                                )
                        )))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("login - 올바른 요청이면 200, 액세스 토큰은 바디로, 리프레시 토큰은 쿠키로 반환한다")
    void login_returns200WithAccessTokenBodyAndRefreshTokenCookie_whenRequestIsValid() throws Exception {
        given(authService.login(any())).willReturn(new AuthTokens("jwt-token", "raw-refresh-token"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("test@test.com", "password1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(cookie().value("refreshToken", "raw-refresh-token"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/auth"));
    }

    @Test
    @DisplayName("login - 잘못된 자격증명이면 401")
    void login_returns401_whenCredentialsAreInvalid() throws Exception {
        given(authService.login(any())).willThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("test@test.com", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("loginWithGoogle - 유효한 idToken이면 200, 액세스 토큰은 바디로, 리프레시 토큰은 쿠키로 반환한다")
    void loginWithGoogle_returns200WithAccessTokenBodyAndRefreshTokenCookie_whenIdTokenIsValid() throws Exception {
        given(authService.loginWithGoogle(any())).willReturn(new AuthTokens("jwt-token", "raw-refresh-token"));

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleLoginRequest("valid-id-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(cookie().value("refreshToken", "raw-refresh-token"));
    }

    @Test
    @DisplayName("loginWithGoogle - 유효하지 않은 idToken이면 401")
    void loginWithGoogle_returns401_whenIdTokenIsInvalid() throws Exception {
        given(authService.loginWithGoogle(any())).willThrow(new InvalidGoogleTokenException());

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleLoginRequest("invalid-token"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refresh - 유효한 리프레시 토큰 쿠키가 있으면 200과 새 액세스/리프레시 토큰을 반환한다")
    void refresh_returns200WithNewTokens_whenRefreshTokenCookieIsValid() throws Exception {
        given(authService.refresh("raw-old-token")).willReturn(new AuthTokens("new-access-token", "raw-new-token"));

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "raw-old-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(cookie().value("refreshToken", "raw-new-token"));
    }

    @Test
    @DisplayName("refresh - 리프레시 토큰 쿠키가 없으면 401")
    void refresh_returns401_whenRefreshTokenCookieIsMissing() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refresh - 유효하지 않은 리프레시 토큰이면 401")
    void refresh_returns401_whenRefreshTokenIsInvalid() throws Exception {
        given(authService.refresh("bad-token")).willThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "bad-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logout - 리프레시 토큰 쿠키가 있으면 204와 함께 쿠키를 만료시킨다")
    void logout_returns204AndExpiresCookie_whenRefreshTokenCookiePresent() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refreshToken", "raw-token")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refreshToken", 0));

        verify(authService).logout("raw-token");
    }

    @Test
    @DisplayName("logout - 리프레시 토큰 쿠키가 없어도 204를 반환한다")
    void logout_returns204_whenRefreshTokenCookieIsMissing() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent());

        verify(authService, never()).logout(any());
    }
}
