
package com.kdongdexample.norunnolifeexample.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdongdexample.norunnolifeexample.dto.CommunityPostResponse;
import com.kdongdexample.norunnolifeexample.exception.CommunityPostNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.GlobalExceptionHandler;
import com.kdongdexample.norunnolifeexample.service.CommunityPostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.context.request.NativeWebRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommunityPostControllerTest {

    private CommunityPostService service;
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    private CommunityPostResponse post;

    @BeforeEach
    void setUp() {
        service = mock(CommunityPostService.class);

        // 인증된 회원 ID를 1L로 가정하는 Controller 전용 테스트
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
                .standaloneSetup(new CommunityPostController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        userIdResolver,
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();

        LocalDateTime now =
                LocalDateTime.of(2026, 10, 30, 12, 0);

        post = new CommunityPostResponse(
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
    @DisplayName("게시글 작성 성공 시 201을 반환한다")
    void create_success() throws Exception {
        when(service.create(any(), eq(1L))).thenReturn(post);

        mockMvc.perform(post("/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "오늘 러닝 완료",
                                  "content": "5km 달렸습니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "http://localhost/community/posts/10"
                ))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("오늘 러닝 완료"))
                .andExpect(jsonPath("$.authorNickname").value("러닝왕"));

        verify(service).create(any(), eq(1L));
    }

    @Test
    @DisplayName("게시글 제목이 비어 있으면 400을 반환한다")
    void create_blankTitle() throws Exception {
        mockMvc.perform(post("/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "content": "운동 완료"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("게시글 제목이 50자를 초과하면 400을 반환한다")
    void create_titleTooLong() throws Exception {
        String title = "가".repeat(51);

        String body = objectMapper.writeValueAsString(
                new com.kdongdexample.norunnolifeexample.dto
                        .CommunityPostCreateRequest(title, "본문")
        );

        mockMvc.perform(post("/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("게시글 전체 조회 시 200과 페이지 데이터를 반환한다")
    void getPosts_success() throws Exception {
        when(service.findAll(any())).thenReturn(
                new PageImpl<>(
                        List.of(post),
                        PageRequest.of(0, 10,
                                Sort.by(Sort.Direction.DESC, "createdAt")),
                        1
                )
        );

        mockMvc.perform(get("/community/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].title")
                        .value("오늘 러닝 완료"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("게시글 상세 조회 성공 시 200을 반환한다")
    void getPost_success() throws Exception {
        when(service.findById(10L)).thenReturn(post);

        mockMvc.perform(get("/community/posts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.authorId").value(1))
                .andExpect(jsonPath("$.content")
                        .value("5km 달렸습니다."));
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 시 404를 반환한다")
    void getPost_notFound() throws Exception {
        when(service.findById(999L))
                .thenThrow(new CommunityPostNotFoundException(999L));

        mockMvc.perform(get("/community/posts/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("제목으로 게시글을 검색한다")
    void search_title() throws Exception {
        when(service.searchByTitle(eq("러닝"), any()))
                .thenReturn(new PageImpl<>(List.of(post)));

        mockMvc.perform(get("/community/posts/search")
                        .param("type", "title")
                        .param("keyword", "러닝"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title")
                        .value("오늘 러닝 완료"));

        verify(service).searchByTitle(eq("러닝"), any());
    }

    @Test
    @DisplayName("작성자 닉네임으로 게시글을 검색한다")
    void search_nickname() throws Exception {
        when(service.searchByNickname(eq("러닝왕"), any()))
                .thenReturn(new PageImpl<>(List.of(post)));

        mockMvc.perform(get("/community/posts/search")
                        .param("type", "nickname")
                        .param("keyword", "러닝왕"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].authorNickname")
                        .value("러닝왕"));

        verify(service).searchByNickname(eq("러닝왕"), any());
    }

    @Test
    @DisplayName("잘못된 검색 타입이면 400을 반환한다")
    void search_invalidType() throws Exception {
        mockMvc.perform(get("/community/posts/search")
                        .param("type", "invalid")
                        .param("keyword", "러닝"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("검색어가 공백이면 400을 반환한다")
    void search_blankKeyword() throws Exception {
        mockMvc.perform(get("/community/posts/search")
                        .param("type", "title")
                        .param("keyword", "   "))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("내가 작성한 게시글을 조회한다")
    void getMyPosts_success() throws Exception {
        when(service.findByAuthor(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(post)));

        mockMvc.perform(get("/community/posts/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].authorId").value(1));

        verify(service).findByAuthor(eq(1L), any());
    }

    @Test
    @DisplayName("특정 회원이 작성한 게시글을 조회한다")
    void getUserPosts_success() throws Exception {
        when(service.findByAuthor(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(post)));

        mockMvc.perform(get("/community/posts/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].authorNickname")
                        .value("러닝왕"));
    }

    @Test
    @DisplayName("게시글 수정 성공 시 200을 반환한다")
    void update_success() throws Exception {
        CommunityPostResponse updated = new CommunityPostResponse(
                10L, 1L, "러닝왕",
                "수정된 제목", "수정된 내용",
                post.createdAt(), post.updatedAt()
        );

        when(service.update(eq(10L), any(), eq(1L)))
                .thenReturn(updated);

        mockMvc.perform(put("/community/posts/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정된 제목",
                                  "content": "수정된 내용"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 제목"))
                .andExpect(jsonPath("$.content").value("수정된 내용"));

        verify(service).update(eq(10L), any(), eq(1L));
    }

    @Test
    @DisplayName("게시글 수정 입력값이 잘못되면 400을 반환한다")
    void update_invalidRequest() throws Exception {
        mockMvc.perform(put("/community/posts/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "content": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("게시글 삭제 성공 시 204를 반환한다")
    void delete_success() throws Exception {
        mockMvc.perform(delete("/community/posts/10"))
                .andExpect(status().isNoContent());

        verify(service).delete(10L, 1L);
    }

    @Test
    @DisplayName("삭제 권한이 없으면 404를 반환한다")
    void delete_otherUser() throws Exception {
        doThrow(new CommunityPostNotFoundException(10L))
                .when(service).delete(10L, 1L);

        mockMvc.perform(delete("/community/posts/10"))
                .andExpect(status().isNotFound());
    }
}
