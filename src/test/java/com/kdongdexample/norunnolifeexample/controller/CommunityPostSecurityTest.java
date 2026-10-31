
package com.kdongdexample.norunnolifeexample.controller;

import com.kdongdexample.norunnolifeexample.config.SecurityConfig;
import com.kdongdexample.norunnolifeexample.dto.CommunityPostResponse;
import com.kdongdexample.norunnolifeexample.security.CustomAuthenticationEntryPoint;
import com.kdongdexample.norunnolifeexample.security.JwtTokenProvider;
import com.kdongdexample.norunnolifeexample.security.TokenStatus;
import com.kdongdexample.norunnolifeexample.service.CommunityPostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommunityPostController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        CustomAuthenticationEntryPoint.class
})
@TestPropertySource(properties = {
        "cors.allowed-origins=http://localhost:5173"
})
class CommunityPostSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommunityPostService service;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private static final String VALID_TOKEN = "valid-test-token";

    // 유효한 JWT를 가진 회원 ID를 1L로 설정
    private void mockValidToken() {
        when(jwtTokenProvider.validate(VALID_TOKEN))
                .thenReturn(TokenStatus.VALID);

        when(jwtTokenProvider.getUserId(VALID_TOKEN))
                .thenReturn(1L);
    }

    // 테스트용 게시글 응답 생성
    private CommunityPostResponse createPostResponse() {
        LocalDateTime now = LocalDateTime.of(
                2026, 10, 30, 12, 0
        );

        return new CommunityPostResponse(
                10L,
                1L,
                "러닝왕",
                "오늘 러닝 완료",
                "5km 달렸습니다.",
                now,
                now
        );
    }

    @Test
    @DisplayName("비로그인 사용자는 전체 게시글을 조회할 수 있다")
    void publicGetPosts() throws Exception {
        when(service.findAll(any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/community/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("비로그인 사용자는 게시글 상세 조회가 가능하다")
    void publicGetPost() throws Exception {
        when(service.findById(10L))
                .thenReturn(createPostResponse());

        mockMvc.perform(get("/community/posts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("오늘 러닝 완료"));
    }

    @Test
    @DisplayName("비로그인 사용자는 게시글을 작성할 수 없다")
    void anonymousCreateDenied() throws Exception {
        mockMvc.perform(post("/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "오늘 운동 완료",
                                  "content": "5km 달렸습니다."
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("비로그인 사용자는 게시글을 수정할 수 없다")
    void anonymousUpdateDenied() throws Exception {
        mockMvc.perform(put("/community/posts/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정된 제목",
                                  "content": "수정된 내용"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("비로그인 사용자는 게시글을 삭제할 수 없다")
    void anonymousDeleteDenied() throws Exception {
        mockMvc.perform(delete("/community/posts/10"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("비로그인 사용자는 내 게시글을 조회할 수 없다")
    void anonymousMyPostsDenied() throws Exception {
        mockMvc.perform(get("/community/posts/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("유효한 JWT가 있으면 게시글을 작성할 수 있다")
    void authenticatedCreate() throws Exception {
        mockValidToken();

        when(service.create(any(), eq(1L)))
                .thenReturn(createPostResponse());

        mockMvc.perform(post("/community/posts")
                        .header(
                                "Authorization",
                                "Bearer " + VALID_TOKEN
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "오늘 러닝 완료",
                                  "content": "5km 달렸습니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.authorId").value(1));

        verify(service).create(any(), eq(1L));
    }

    @Test
    @DisplayName("유효한 JWT가 있으면 게시글을 수정할 수 있다")
    void authenticatedUpdate() throws Exception {
        mockValidToken();

        when(service.update(eq(10L), any(), eq(1L)))
                .thenReturn(createPostResponse());

        mockMvc.perform(put("/community/posts/10")
                        .header(
                                "Authorization",
                                "Bearer " + VALID_TOKEN
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정된 제목",
                                  "content": "수정된 내용"
                                }
                                """))
                .andExpect(status().isOk());

        verify(service).update(eq(10L), any(), eq(1L));
    }

    @Test
    @DisplayName("유효한 JWT가 있으면 게시글을 삭제할 수 있다")
    void authenticatedDelete() throws Exception {
        mockValidToken();

        mockMvc.perform(delete("/community/posts/10")
                        .header(
                                "Authorization",
                                "Bearer " + VALID_TOKEN
                        ))
                .andExpect(status().isNoContent());

        verify(service).delete(10L, 1L);
    }

    @Test
    @DisplayName("유효하지 않은 JWT로 게시글 작성 시 401을 반환한다")
    void invalidTokenDenied() throws Exception {
        when(jwtTokenProvider.validate("invalid-token"))
                .thenReturn(TokenStatus.INVALID);

        mockMvc.perform(post("/community/posts")
                        .header(
                                "Authorization",
                                "Bearer invalid-token"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "제목",
                                  "content": "내용"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode")
                        .value("TOKEN_INVALID"));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("만료된 JWT로 게시글 작성 시 401을 반환한다")
    void expiredTokenDenied() throws Exception {
        when(jwtTokenProvider.validate("expired-token"))
                .thenReturn(TokenStatus.EXPIRED);

        mockMvc.perform(post("/community/posts")
                        .header(
                                "Authorization",
                                "Bearer expired-token"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "제목",
                                  "content": "내용"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode")
                        .value("TOKEN_EXPIRED"));

        verifyNoInteractions(service);
    }
}
