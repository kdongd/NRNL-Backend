
package com.kdongdexample.norunnolifeexample.controller;

import com.kdongdexample.norunnolifeexample.dto.CommunityLikeResponse;
import com.kdongdexample.norunnolifeexample.exception.CommunityCommentNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.CommunityPostNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.GlobalExceptionHandler;
import com.kdongdexample.norunnolifeexample.service.CommunityCommentLikeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommunityCommentLikeControllerTest {

    private CommunityCommentLikeService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(CommunityCommentLikeService.class);

        // 인증된 회원 ID를 1L로 가정
        HandlerMethodArgumentResolver userIdResolver =
                new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(
                                AuthenticationPrincipal.class
                        ) && parameter.getParameterType().equals(Long.class);
                    }

                    @Override
                    public Object resolveArgument(
                            MethodParameter parameter,
                            ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest,
                            org.springframework.web.bind.support.WebDataBinderFactory binderFactory
                    ) {
                        return 1L;
                    }
                };

        mockMvc = MockMvcBuilders
                .standaloneSetup(new CommunityCommentLikeController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(userIdResolver)
                .build();
    }

    @Test
    @DisplayName("댓글 좋아요 등록 성공 시 204를 반환한다")
    void like_success() throws Exception {
        mockMvc.perform(post("/community/posts/10/comments/100/likes"))
                .andExpect(status().isNoContent());

        verify(service).like(10L, 100L, 1L);
    }

    @Test
    @DisplayName("댓글 중복 좋아요 요청도 204를 반환한다")
    void like_duplicate() throws Exception {
        mockMvc.perform(post("/community/posts/10/comments/100/likes"))
                .andExpect(status().isNoContent());

        verify(service).like(10L, 100L, 1L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글의 댓글 좋아요 시 404를 반환한다")
    void like_postNotFound() throws Exception {
        doThrow(new CommunityPostNotFoundException(999L))
                .when(service).like(999L, 100L, 1L);

        mockMvc.perform(post("/community/posts/999/comments/100/likes"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("존재하지 않는 댓글 좋아요 시 404를 반환한다")
    void like_commentNotFound() throws Exception {
        doThrow(new CommunityCommentNotFoundException(999L))
                .when(service).like(10L, 999L, 1L);

        mockMvc.perform(post("/community/posts/10/comments/999/likes"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("다른 게시글에 속한 댓글 좋아요 시 404를 반환한다")
    void like_wrongPost() throws Exception {
        doThrow(new CommunityCommentNotFoundException(100L))
                .when(service).like(20L, 100L, 1L);

        mockMvc.perform(post("/community/posts/20/comments/100/likes"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 좋아요 취소 성공 시 204를 반환한다")
    void unlike_success() throws Exception {
        mockMvc.perform(delete("/community/posts/10/comments/100/likes"))
                .andExpect(status().isNoContent());

        verify(service).unlike(10L, 100L, 1L);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 좋아요 취소 시 404를 반환한다")
    void unlike_commentNotFound() throws Exception {
        doThrow(new CommunityCommentNotFoundException(999L))
                .when(service).unlike(10L, 999L, 1L);

        mockMvc.perform(delete("/community/posts/10/comments/999/likes"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 좋아요 조회 시 200과 좋아요 상태를 반환한다")
    void getLikeStatus_success() throws Exception {
        when(service.getLikeStatus(10L, 100L, 1L))
                .thenReturn(new CommunityLikeResponse(7L, true));

        mockMvc.perform(get("/community/posts/10/comments/100/likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(7))
                .andExpect(jsonPath("$.likedByMe").value(true));

        verify(service).getLikeStatus(10L, 100L, 1L);
    }

    @Test
    @DisplayName("좋아요하지 않은 댓글은 likedByMe가 false이다")
    void getLikeStatus_notLiked() throws Exception {
        when(service.getLikeStatus(10L, 100L, 1L))
                .thenReturn(new CommunityLikeResponse(0L, false));

        mockMvc.perform(get("/community/posts/10/comments/100/likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.likedByMe").value(false));
    }

    @Test
    @DisplayName("존재하지 않는 댓글 좋아요 조회 시 404를 반환한다")
    void getLikeStatus_commentNotFound() throws Exception {
        when(service.getLikeStatus(10L, 999L, 1L))
                .thenThrow(new CommunityCommentNotFoundException(999L));

        mockMvc.perform(get("/community/posts/10/comments/999/likes"))
                .andExpect(status().isNotFound());
    }
}
