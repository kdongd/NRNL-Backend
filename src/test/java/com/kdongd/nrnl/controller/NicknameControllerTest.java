
package com.kdongd.nrnl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdongd.nrnl.config.SecurityConfig;
import com.kdongd.nrnl.dto.NicknameAvailabilityResponse;
import com.kdongd.nrnl.dto.NicknameUpdateRequest;
import com.kdongd.nrnl.exception.InvalidNicknameException;
import com.kdongd.nrnl.exception.NicknameAlreadyExistsException;
import com.kdongd.nrnl.repository.UserRepository;
import com.kdongd.nrnl.security.CustomAuthenticationEntryPoint;
import com.kdongd.nrnl.security.JwtTokenProvider;
import com.kdongd.nrnl.service.NicknameService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NicknameController.class)
@Import({
        SecurityConfig.class,
        JwtTokenProvider.class,
        CustomAuthenticationEntryPoint.class
})
class NicknameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NicknameService nicknameService;

    // NicknameController가 의존하는 Repository를 테스트용 Mock으로 등록
    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("닉네임 사용 가능 여부 조회는 미인증 상태에서도 200을 반환한다")
    void checkNicknameAvailability_unauthenticated_success() throws Exception {

        when(nicknameService.checkAvailability("Runner"))
                .thenReturn(new NicknameAvailabilityResponse(true, null));

        mockMvc.perform(
                        get("/users/nicknames/availability")
                                .param("nickname", "Runner")
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("정상적인 닉네임 변경 요청은 200을 반환한다")
    void changeNickname_success() throws Exception {

        Long userId = 1L;

        NicknameUpdateRequest request =
                new NicknameUpdateRequest("Runner");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of()
                );

        mockMvc.perform(
                        put("/users/me/nickname")
                                .with(securityContext(
                                        createSecurityContext(authentication)
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());

        verify(nicknameService)
                .changeNickname(userId, "Runner");
    }

    @Test
    @DisplayName("잘못된 닉네임으로 변경하면 400을 반환한다")
    void changeNickname_invalidNickname() throws Exception {

        Long userId = 1L;
        String nickname = "admin";

        doThrow(new InvalidNicknameException("사용할 수 없는 닉네임입니다"))
                .when(nicknameService)
                .changeNickname(anyLong(), eq(nickname));

        NicknameUpdateRequest request =
                new NicknameUpdateRequest(nickname);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of()
                );

        mockMvc.perform(
                        put("/users/me/nickname")
                                .with(securityContext(
                                        createSecurityContext(authentication)
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("중복된 닉네임으로 변경하면 409를 반환한다")
    void changeNickname_duplicateNickname() throws Exception {

        Long userId = 1L;

        doThrow(new NicknameAlreadyExistsException(
                "이미 사용 중인 닉네임입니다"
        ))
                .when(nicknameService)
                .changeNickname(anyLong(), eq("Runner"));

        NicknameUpdateRequest request =
                new NicknameUpdateRequest("Runner");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of()
                );

        mockMvc.perform(
                        put("/users/me/nickname")
                                .with(securityContext(
                                        createSecurityContext(authentication)
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("미인증 상태에서 닉네임을 변경하면 401을 반환한다")
    void changeNickname_unauthenticated() throws Exception {

        NicknameUpdateRequest request =
                new NicknameUpdateRequest("Runner");

        mockMvc.perform(
                        put("/users/me/nickname")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());
    }

    private SecurityContext createSecurityContext(
            UsernamePasswordAuthenticationToken authentication
    ) {
        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        return context;
    }
}
