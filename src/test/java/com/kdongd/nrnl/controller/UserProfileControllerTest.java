package com.kdongd.nrnl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdongd.nrnl.config.SecurityConfig;
import com.kdongd.nrnl.domain.ExperienceLevel;
import com.kdongd.nrnl.domain.Gender;
import com.kdongd.nrnl.domain.User;
import com.kdongd.nrnl.domain.UserProfile;
import com.kdongd.nrnl.domain.WorkoutType;
import com.kdongd.nrnl.dto.UserProfileRequest;
import com.kdongd.nrnl.exception.UserProfileAlreadyExistsException;
import com.kdongd.nrnl.security.CustomAuthenticationEntryPoint;
import com.kdongd.nrnl.security.JwtTokenProvider;
import com.kdongd.nrnl.service.UserProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, CustomAuthenticationEntryPoint.class})
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserProfileService userProfileService;

    private static UsernamePasswordAuthenticationToken authAs(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private UserProfileRequest validRequest() {
        return new UserProfileRequest(Gender.MAN, 28, 175.0, 70.0, ExperienceLevel.INTERMEDIATE,
                Set.of(WorkoutType.RUNNING), "10km 완주");
    }

    @Test
    @DisplayName("POST /users/me/profile - 유효한 요청이면 201과 생성된 프로필을 반환한다")
    void createProfile_returns201_whenRequestIsValid() throws Exception {
        User user = User.create("user@test.com", "encoded-password");
        UserProfile profile = UserProfile.create(user, Gender.MAN, 28, 175.0, 70.0,
                ExperienceLevel.INTERMEDIATE, Set.of(WorkoutType.RUNNING), "10km 완주");
        given(userProfileService.createProfile(eq(1L), any())).willReturn(profile);

        mockMvc.perform(post("/users/me/profile")
                        .with(authentication(authAs(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gender").value("MAN"))
                .andExpect(jsonPath("$.age").value(28))
                .andExpect(jsonPath("$.goal").value("10km 완주"));
    }

    @Test
    @DisplayName("POST /users/me/profile - 나이가 범위를 벗어나면 400")
    void createProfile_returns400_whenAgeOutOfRange() throws Exception {
        UserProfileRequest invalid = new UserProfileRequest(Gender.MAN, 3, 175.0, 70.0,
                ExperienceLevel.INTERMEDIATE, Set.of(WorkoutType.RUNNING), "10km 완주");

        mockMvc.perform(post("/users/me/profile")
                        .with(authentication(authAs(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users/me/profile - 주요 운동을 하나도 선택하지 않으면 400")
    void createProfile_returns400_whenPrimaryExercisesEmpty() throws Exception {
        UserProfileRequest invalid = new UserProfileRequest(Gender.MAN, 28, 175.0, 70.0,
                ExperienceLevel.INTERMEDIATE, Set.of(), "10km 완주");

        mockMvc.perform(post("/users/me/profile")
                        .with(authentication(authAs(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users/me/profile - 이미 프로필이 있으면 409")
    void createProfile_returns409_whenProfileAlreadyExists() throws Exception {
        willThrow(new UserProfileAlreadyExistsException(1L)).given(userProfileService).createProfile(eq(1L), any());

        mockMvc.perform(post("/users/me/profile")
                        .with(authentication(authAs(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /users/me/profile - 인증되지 않은 요청이면 401")
    void createProfile_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }
}
