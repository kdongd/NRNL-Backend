
package com.kdongdexample.norunnolifeexample.controller;

import com.kdongdexample.norunnolifeexample.dto.CommunityCommentResponse;
import com.kdongdexample.norunnolifeexample.service.CommunityCommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
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

class CommunityMyCommentControllerTest {

    private CommunityCommentService service;
    private MockMvc mockMvc;
    private CommunityCommentResponse comment;

    @BeforeEach
    void setUp() {
        service = mock(CommunityCommentService.class);

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
                .standaloneSetup(
                        new CommunityMyCommentController(service)
                )
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
                "오늘도 운동 완료!",
                now,
                now
        );
    }

    @Test
    @DisplayName("내 댓글 목록 조회 성공 시 200을 반환한다")
    void getMyComments_success() throws Exception {
        when(service.findMyComments(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(comment)));

        mockMvc.perform(get("/community/comments/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].postId").value(10))
                .andExpect(jsonPath("$.content[0].authorId").value(1))
                .andExpect(jsonPath("$.content[0].content")
                        .value("오늘도 운동 완료!"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(service).findMyComments(eq(1L), any());
    }

    @Test
    @DisplayName("내 댓글 조회는 기본 10개씩 최신순으로 정렬한다")
    void getMyComments_defaultPageable() throws Exception {
        when(service.findMyComments(eq(1L), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/community/comments/me"))
                .andExpect(status().isOk());

        verify(service).findMyComments(
                eq(1L),
                argThat(pageable ->
                        pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 10
                                && pageable.getSort().getOrderFor("createdAt")
                                .getDirection() == Sort.Direction.DESC
                                && pageable.getSort().getOrderFor("id")
                                .getDirection() == Sort.Direction.DESC
                )
        );
    }

    @Test
    @DisplayName("내 댓글 조회 시 페이지 번호와 크기를 지정할 수 있다")
    void getMyComments_customPageable() throws Exception {
        PageRequest requestedPage = PageRequest.of(
                1,
                5,
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );

        when(service.findMyComments(eq(1L), any()))
                .thenReturn(new PageImpl<>(
                        List.of(comment),
                        requestedPage,
                        6
                ));

        mockMvc.perform(get("/community/comments/me")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(service).findMyComments(
                eq(1L),
                argThat(pageable ->
                        pageable.getPageNumber() == 1
                                && pageable.getPageSize() == 5
                )
        );
    }

    @Test
    @DisplayName("내가 작성한 댓글이 없으면 빈 목록을 반환한다")
    void getMyComments_empty() throws Exception {
        when(service.findMyComments(eq(1L), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/community/comments/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
