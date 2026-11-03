
package com.kdongdexample.norunnolifeexample.controller;

import com.kdongdexample.norunnolifeexample.dto.CommunityCommentResponse;
import com.kdongdexample.norunnolifeexample.exception.CommunityCommentNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.CommunityPostNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.GlobalExceptionHandler;
import com.kdongdexample.norunnolifeexample.service.CommunityCommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommunityCommentControllerTest {

    private CommunityCommentService service;
    private MockMvc mockMvc;
    private CommunityCommentResponse comment;

    @BeforeEach
    void setUp() {
        service = mock(CommunityCommentService.class);

        // 인증된 사용자 ID를 1L로 가정
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
                .standaloneSetup(new CommunityCommentController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        userIdResolver,
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();

        LocalDateTime now =
                LocalDateTime.of(2026, 11, 3, 10, 0);

        comment = new CommunityCommentResponse(
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
    @DisplayName("댓글 작성 성공 시 201과 Location 헤더를 반환한다")
    void create_success() throws Exception {
        when(service.create(eq(10L), any(), eq(1L)))
                .thenReturn(comment);

        mockMvc.perform(post("/community/posts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "수고하셨습니다!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "http://localhost/community/posts/10/comments/100"
                ))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.postId").value(10))
                .andExpect(jsonPath("$.authorId").value(1))
                .andExpect(jsonPath("$.content")
                        .value("수고하셨습니다!"));

        verify(service).create(eq(10L), any(), eq(1L));
    }

    @Test
    @DisplayName("댓글 내용이 비어 있으면 400을 반환한다")
    void create_blankContent() throws Exception {
        mockMvc.perform(post("/community/posts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("댓글 내용이 공백만 있으면 400을 반환한다")
    void create_whitespaceContent() throws Exception {
        mockMvc.perform(post("/community/posts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("댓글 내용이 200자를 초과하면 400을 반환한다")
    void create_contentTooLong() throws Exception {
        String content = "가".repeat(201);

        mockMvc.perform(post("/community/posts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + content + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 댓글 작성 시 404를 반환한다")
    void create_postNotFound() throws Exception {
        when(service.create(eq(999L), any(), eq(1L)))
                .thenThrow(new CommunityPostNotFoundException(999L));

        mockMvc.perform(post("/community/posts/999/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "댓글 내용"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("게시글 댓글 목록 조회 시 200과 페이지 데이터를 반환한다")
    void getComments_success() throws Exception {
        when(service.findByPost(eq(10L), any()))
                .thenReturn(new PageImpl<>(
                        List.of(comment),
                        PageRequest.of(
                                0,
                                10,
                                Sort.by(
                                        Sort.Direction.ASC,
                                        "createdAt",
                                        "id"
                                )
                        ),
                        1
                ));

        mockMvc.perform(get("/community/posts/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].postId").value(10))
                .andExpect(jsonPath("$.content[0].authorNickname")
                        .value("러닝왕"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("댓글 조회 시 기본 정렬은 작성일과 ID 오름차순이다")
    void getComments_defaultSort() throws Exception {
        when(service.findByPost(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of(comment)));

        mockMvc.perform(get("/community/posts/10/comments"))
                .andExpect(status().isOk());

        verify(service).findByPost(
                eq(10L),
                argThat(pageable ->
                        pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 10
                                && pageable.getSort().getOrderFor("createdAt")
                                .getDirection() == Sort.Direction.ASC
                                && pageable.getSort().getOrderFor("id")
                                .getDirection() == Sort.Direction.ASC
                )
        );
    }

    @Test
    @DisplayName("존재하지 않는 게시글의 댓글 조회 시 404를 반환한다")
    void getComments_postNotFound() throws Exception {
        when(service.findByPost(eq(999L), any()))
                .thenThrow(new CommunityPostNotFoundException(999L));

        mockMvc.perform(get("/community/posts/999/comments"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 수정 성공 시 200을 반환한다")
    void update_success() throws Exception {
        CommunityCommentResponse updated =
                new CommunityCommentResponse(
                        100L,
                        10L,
                        1L,
                        "러닝왕",
                        "수정된 댓글",
                        comment.createdAt(),
                        comment.updatedAt()
                );

        when(service.update(eq(10L), eq(100L), any(), eq(1L)))
                .thenReturn(updated);

        mockMvc.perform(put("/community/posts/10/comments/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "수정된 댓글"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.content").value("수정된 댓글"));

        verify(service).update(eq(10L), eq(100L), any(), eq(1L));
    }

    @Test
    @DisplayName("댓글 수정 내용이 비어 있으면 400을 반환한다")
    void update_blankContent() throws Exception {
        mockMvc.perform(put("/community/posts/10/comments/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("댓글 수정 내용이 200자를 초과하면 400을 반환한다")
    void update_contentTooLong() throws Exception {
        String content = "가".repeat(201);

        mockMvc.perform(put("/community/posts/10/comments/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + content + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("댓글 수정 권한이 없으면 404를 반환한다")
    void update_otherUser() throws Exception {
        when(service.update(eq(10L), eq(100L), any(), eq(1L)))
                .thenThrow(new CommunityCommentNotFoundException(100L));

        mockMvc.perform(put("/community/posts/10/comments/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "수정 시도"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 삭제 성공 시 204를 반환한다")
    void delete_success() throws Exception {
        mockMvc.perform(delete("/community/posts/10/comments/100"))
                .andExpect(status().isNoContent());

        verify(service).delete(10L, 100L, 1L);
    }

    @Test
    @DisplayName("댓글 삭제 권한이 없으면 404를 반환한다")
    void delete_otherUser() throws Exception {
        doThrow(new CommunityCommentNotFoundException(100L))
                .when(service).delete(10L, 100L, 1L);

        mockMvc.perform(delete("/community/posts/10/comments/100"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("존재하지 않는 댓글 삭제 시 404를 반환한다")
    void delete_notFound() throws Exception {
        doThrow(new CommunityCommentNotFoundException(999L))
                .when(service).delete(10L, 999L, 1L);

        mockMvc.perform(delete("/community/posts/10/comments/999"))
                .andExpect(status().isNotFound());
    }
}
