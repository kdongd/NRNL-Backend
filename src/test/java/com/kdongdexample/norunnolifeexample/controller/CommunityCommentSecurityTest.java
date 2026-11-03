
package com.kdongdexample.norunnolifeexample.controller;

import com.kdongdexample.norunnolifeexample.config.SecurityConfig;
import com.kdongdexample.norunnolifeexample.dto.CommunityCommentResponse;
import com.kdongdexample.norunnolifeexample.security.CustomAuthenticationEntryPoint;
import com.kdongdexample.norunnolifeexample.security.JwtTokenProvider;
import com.kdongdexample.norunnolifeexample.security.TokenStatus;
import com.kdongdexample.norunnolifeexample.service.CommunityCommentService;
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

@WebMvcTest({
        CommunityCommentController.class,
        CommunityMyCommentController.class
})
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        CustomAuthenticationEntryPoint.class
})
@TestPropertySource(properties = {
        "cors.allowed-origins=http://localhost:5173"
})
class CommunityCommentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommunityCommentService service;

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

    // 테스트용 댓글 응답 생성
    private CommunityCommentResponse createCommentResponse() {
        LocalDateTime now =
                LocalDateTime.of(2026, 11, 3, 10, 0);

        return new CommunityCommentResponse(
                100L,
                10L,
                1L,
                "러닝왕",
                "수고하셨습니다!",
                now,
                now
        );
    }

    @Test
    @DisplayName("비로그인 사용자는 게시글 댓글 목록을 조회할 수 있다")
    void publicGetComments() throws Exception {
        when(service.findByPost(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of(
                        createCommentResponse()
                )));

        mockMvc.perform(get("/community/posts/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].postId").value(10));
    }

    @Test
    @DisplayName("비로그인 사용자는 댓글을 작성할 수 없다")
    void anonymousCreateDenied() throws Exception {
        mockMvc.perform(post("/community/posts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "댓글 작성"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("비로그인 사용자는 댓글을 수정할 수 없다")
    void anonymousUpdateDenied() throws Exception {
        mockMvc.perform(put("/community/posts/10/comments/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "댓글 수정"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("비로그인 사용자는 댓글을 삭제할 수 없다")
    void anonymousDeleteDenied() throws Exception {
        mockMvc.perform(delete("/community/posts/10/comments/100"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("비로그인 사용자는 내 댓글 목록을 조회할 수 없다")
    void anonymousMyCommentsDenied() throws Exception {
        mockMvc.perform(get("/community/comments/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("유효한 JWT가 있으면 댓글을 작성할 수 있다")
    void authenticatedCreate() throws Exception {
        mockValidToken();

        when(service.create(eq(10L), any(), eq(1L)))
                .thenReturn(createCommentResponse());

        mockMvc.perform(post("/community/posts/10/comments")
                        .header(
                                "Authorization",
                                "Bearer " + VALID_TOKEN
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "수고하셨습니다!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.authorId").value(1));

        verify(service).create(eq(10L), any(), eq(1L));
    }

    @Test
    @DisplayName("유효한 JWT가 있으면 댓글을 수정할 수 있다")
    void authenticatedUpdate() throws Exception {
        mockValidToken();

        when(service.update(eq(10L), eq(100L), any(), eq(1L)))
                .thenReturn(createCommentResponse());

        mockMvc.perform(put("/community/posts/10/comments/100")
                        .header(
                                "Authorization",
                                "Bearer " + VALID_TOKEN
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "수정된 댓글"
                                }
                                """))
                .andExpect(status().isOk());

        verify(service).update(eq(10L), eq(100L), any(), eq(1L));
    }

    @Test
    @DisplayName("유효한 JWT가 있으면 댓글을 삭제할 수 있다")
    void authenticatedDelete() throws Exception {
        mockValidToken();

        mockMvc.perform(delete("/community/posts/10/comments/100")
                        .header(
                                "Authorization",
                                "Bearer " + VALID_TOKEN
                        ))
                .andExpect(status().isNoContent());

        verify(service).delete(10L, 100L, 1L);
    }

    @Test
    @DisplayName("유효한 JWT가 있으면 내 댓글 목록을 조회할 수 있다")
    void authenticatedMyComments() throws Exception {
        mockValidToken();

        when(service.findMyComments(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(
                        createCommentResponse()
                )));

        mockMvc.perform(get("/community/comments/me")
                        .header(
                                "Authorization",
                                "Bearer " + VALID_TOKEN
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].authorId").value(1))
                .andExpect(jsonPath("$.content[0].id").value(100));

        verify(service).findMyComments(eq(1L), any());
    }

    @Test
    @DisplayName("유효하지 않은 JWT로 댓글 작성 시 401을 반환한다")
    void invalidTokenDenied() throws Exception {
        when(jwtTokenProvider.validate("invalid-token"))
                .thenReturn(TokenStatus.INVALID);

        mockMvc.perform(post("/community/posts/10/comments")
                        .header(
                                "Authorization",
                                "Bearer invalid-token"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "댓글 내용"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode")
                        .value("TOKEN_INVALID"));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("만료된 JWT로 내 댓글 조회 시 401을 반환한다")
    void expiredTokenDenied() throws Exception {
        when(jwtTokenProvider.validate("expired-token"))
                .thenReturn(TokenStatus.EXPIRED);

        mockMvc.perform(get("/community/comments/me")
                        .header(
                                "Authorization",
                                "Bearer expired-token"
                        ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode")
                        .value("TOKEN_EXPIRED"));

        verifyNoInteractions(service);
    }
}
