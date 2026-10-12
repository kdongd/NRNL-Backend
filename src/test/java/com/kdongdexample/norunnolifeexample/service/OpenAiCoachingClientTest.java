package com.kdongdexample.norunnolifeexample.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdongdexample.norunnolifeexample.config.OpenAiProperties;
import com.kdongdexample.norunnolifeexample.domain.CoachingAdvice;
import com.kdongdexample.norunnolifeexample.exception.LlmUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// 실제 OpenAI를 호출하지 않고, 가짜 서버(MockRestServiceServer)로 요청 형식과 응답 처리를 검증한다
class OpenAiCoachingClientTest {

    private static final String BASE_URL = "https://api.openai.com/v1";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmPrompt prompt = new LlmPrompt("system prompt", "user data");

    private MockRestServiceServer server;
    private OpenAiCoachingClient client;

    private OpenAiProperties properties(String apiKey) {
        return new OpenAiProperties(apiKey, "gpt-6-luna", BASE_URL, 5, "low", 4000);
    }

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenAiCoachingClient(builder.build(), properties("test-key"), objectMapper);
    }

    private String responseJson(String status, String adviceText) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "status", status,
                "output", List.of(
                        Map.of("type", "reasoning"),
                        Map.of("type", "message", "content", List.of(
                                Map.of("type", "output_text", "text", adviceText)))
                )));
    }

    private String adviceJson(List<String> recommendations) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "summary", " 오늘은 쉬는 게 좋아요 ",
                "explanation", "최근 세 번 모두 무릎 통증을 기록했어요.",
                "recommendations", recommendations,
                "caution", "통증이 계속되면 진료를 받아보세요."));
    }

    @Test
    @DisplayName("JSON 스키마를 강제하는 요청을 보내고, 응답을 코칭 객체로 변환한다")
    void success_sendsStructuredRequestAndParsesAdvice() throws Exception {
        server.expect(requestTo(BASE_URL + "/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("gpt-6-luna"))
                .andExpect(jsonPath("$.store").value(false))
                .andExpect(jsonPath("$.reasoning.effort").value("low"))
                .andExpect(jsonPath("$.text.format.type").value("json_schema"))
                .andExpect(jsonPath("$.text.format.strict").value(true))
                .andRespond(withSuccess(
                        responseJson("completed", adviceJson(List.of("상체 스트레칭 10분", " ", "충분한 수면"))),
                        MediaType.APPLICATION_JSON));

        CoachingAdvice advice = client.generate(prompt);

        assertThat(advice.summary()).isEqualTo("오늘은 쉬는 게 좋아요");
        assertThat(advice.recommendations()).containsExactly("상체 스트레칭 10분", "충분한 수면");
        server.verify();
    }

    @Test
    @DisplayName("응답이 완료되지 않았으면(토큰 한도 초과 등) 실패로 처리한다")
    void incompleteResponse_throws() throws Exception {
        server.expect(requestTo(BASE_URL + "/responses"))
                .andRespond(withSuccess(responseJson("incomplete", "{}"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generate(prompt)).isInstanceOf(LlmUnavailableException.class);
    }

    @Test
    @DisplayName("OpenAI 서버 오류는 LlmUnavailableException으로 변환된다")
    void serverError_throws() {
        server.expect(requestTo(BASE_URL + "/responses")).andRespond(withServerError());

        assertThatThrownBy(() -> client.generate(prompt)).isInstanceOf(LlmUnavailableException.class);
    }

    @Test
    @DisplayName("추천 행동이 비어 있으면 유효하지 않은 응답으로 처리한다")
    void emptyRecommendations_throws() throws Exception {
        server.expect(requestTo(BASE_URL + "/responses"))
                .andRespond(withSuccess(responseJson("completed", adviceJson(List.of())), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generate(prompt)).isInstanceOf(LlmUnavailableException.class);
    }

    @Test
    @DisplayName("API 키가 없으면 요청을 보내지 않고 실패한다")
    void missingApiKey_doesNotSendRequest() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer emptyServer = MockRestServiceServer.bindTo(builder).build();
        OpenAiCoachingClient noKeyClient = new OpenAiCoachingClient(builder.build(), properties(""), objectMapper);

        assertThatThrownBy(() -> noKeyClient.generate(prompt)).isInstanceOf(LlmUnavailableException.class);
        emptyServer.verify();
    }
}
