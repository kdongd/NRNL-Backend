package com.kdongd.nrnl.crew.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kdongd.nrnl.config.SecurityConfig;
import com.kdongd.nrnl.crew.domain.CrewStatus;
import com.kdongd.nrnl.crew.dto.CrewMemberResponse;
import com.kdongd.nrnl.crew.dto.CrewResponse;
import com.kdongd.nrnl.crew.exception.CrewException;
import com.kdongd.nrnl.crew.service.CrewService;
import com.kdongd.nrnl.domain.WorkoutType;
import com.kdongd.nrnl.security.CustomAuthenticationEntryPoint;
import com.kdongd.nrnl.security.JwtTokenProvider;
import com.kdongd.nrnl.security.TokenStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CrewController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class})
class CrewSecurityTest {

    private static final String TOKEN = "valid-test-token";
    private static final String VALID_REQUEST = """
            {"name":"러닝Crew","description":"러닝을 함께 하는 크루입니다","sports":["RUNNING","BOXING"],"capacity":10}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrewService service;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void anonymousListAndDetailExposeOnlyMemberCount() throws Exception {
        when(service.search(eq("crew"), eq(WorkoutType.RUNNING), eq(CrewStatus.OPEN), any()))
                .thenReturn(new PageImpl<>(List.of(crew())));
        when(service.findById(10L)).thenReturn(crew());

        mockMvc.perform(get("/crews").param("name", "crew").param("sport", "RUNNING").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].memberCount").value(1))
                .andExpect(jsonPath("$.content[0].members").doesNotExist())
                .andExpect(jsonPath("$.content[0].leaderId").doesNotExist());
        mockMvc.perform(get("/crews/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.members").doesNotExist())
                .andExpect(jsonPath("$.leaderId").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource({
            "GET,/crews/10/members", "GET,/crews/10/applications",
            "POST,/crews", "PUT,/crews/10", "DELETE,/crews/10",
            "POST,/crews/10/applications", "POST,/crews/10/applications/20/cancel",
            "POST,/crews/10/applications/20/approve", "POST,/crews/10/applications/20/reject",
            "PUT,/crews/10/leader", "DELETE,/crews/10/members/me", "DELETE,/crews/10/members/2"
    })
    void allOtherEndpointsRequireAuthentication(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void authenticatedUsersCanReadMembersAndCreateCrew() throws Exception {
        mockToken();
        when(service.findMembers(10L)).thenReturn(List.of(new CrewMemberResponse(2L, "러닝왕", true)));
        when(service.create(any(), eq(1L))).thenReturn(crew());

        mockMvc.perform(get("/crews/10/members").header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nickname").value("러닝왕"));
        mockMvc.perform(post("/crews").header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/crews/10"))
                .andExpect(jsonPath("$.status").value("OPEN"));
        verify(service).create(any(), eq(1L));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"name\":\"A\"}", "{\"name\":\"한 글\"}", "{\"name\":\"Crew!\"}",
            "{\"name\":\"123456789012345678901\"}", "{\"description\":\"짧은 소개\"}",
            "{\"description\":\"          \"}", "{\"sports\":[]}", "{\"sports\":[null]}",
            "{\"sports\":[\"SWIMMING\"]}", "{\"capacity\":null}", "{\"capacity\":10.5}", "{\"capacity\":\"10\"}"
    })
    void rejectsInvalidInputBeforeCallingService(String overrides) throws Exception {
        mockToken();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode body = (ObjectNode) mapper.readTree(VALID_REQUEST);
        body.setAll((ObjectNode) mapper.readTree(overrides));

        mockMvc.perform(post("/crews").header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void businessConflictsUseExistingErrorResponse() throws Exception {
        mockToken();
        when(service.apply(10L, 1L)).thenThrow(CrewException.conflict("모집이 마감되었습니다"));
        mockMvc.perform(post("/crews/10/applications").header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("모집이 마감되었습니다"));
    }

    private void mockToken() {
        when(jwtTokenProvider.validate(TOKEN)).thenReturn(TokenStatus.VALID);
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(1L);
    }

    private CrewResponse crew() {
        return new CrewResponse(10L, "러닝Crew", "러닝을 함께 하는 크루입니다",
                Set.of(WorkoutType.RUNNING), 10, CrewStatus.OPEN, 1, Instant.EPOCH);
    }
}
