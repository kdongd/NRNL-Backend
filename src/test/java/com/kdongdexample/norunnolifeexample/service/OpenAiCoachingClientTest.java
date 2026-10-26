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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// 실제 OpenAI를 호출하지 않고, 가짜 서버(MockRestServiceServer)로 요청 형식과 응답 처리를 검증한다
class OpenAiCoachingClientTest {

    private static final String BASE_URL = "https://api.openai.com/v1";
    private static final String RESPONSES_URL = BASE_URL + "/responses";

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
        // 테스트가 느려지지 않게 재시도 대기 시간은 0
        client = new OpenAiCoachingClient(builder.build(), properties("test-key"), objectMapper, Duration.ZERO);
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

    private String refusalJson() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "status", "completed",
                "output", List.of(
                        Map.of("type", "message", "content", List.of(
                                Map.of("type", "refusal", "refusal", "도와드릴 수 없습니다")))
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
        server.expect(requestTo(RESPONSES_URL))
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
    @DisplayName("응답이 완료되지 않았으면(토큰 한도 초과 등) 재시도 없이 실패로 처리한다")
    void incompleteResponse_throwsWithoutRetry() throws Exception {
        server.expect(once(), requestTo(RESPONSES_URL))
                .andRespond(withSuccess(responseJson("incomplete", "{}"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generate(prompt)).isInstanceOf(LlmUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("5xx가 한 번 나고 재시도에서 성공하면 코칭을 반환한다")
    void serverErrorThenSuccess_retriesOnce() throws Exception {
        server.expect(once(), requestTo(RESPONSES_URL)).andRespond(withServerError());
        server.expect(once(), requestTo(RESPONSES_URL))
                .andRespond(withSuccess(
                        responseJson("completed", adviceJson(List.of("상체 스트레칭 10분"))),
                        MediaType.APPLICATION_JSON));

        CoachingAdvice advice = client.generate(prompt);

        assertThat(advice.summary()).isEqualTo("오늘은 쉬는 게 좋아요");
        server.verify();
    }

    @Test
    @DisplayName("5xx가 재시도에서도 나면 2번 호출 후 LlmUnavailableException으로 변환된다")
    void serverErrorTwice_throwsAfterRetry() {
        server.expect(times(2), requestTo(RESPONSES_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.generate(prompt)).isInstanceOf(LlmUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("429(요청 한도 초과)는 재시도 대상이다")
    void tooManyRequests_retries() {
        server.expect(times(2), requestTo(RESPONSES_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.generate(prompt)).isInstanceOf(LlmUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("타임아웃은 재시도 대상이다")
    void timeout_retries() {
        server.expect(times(2), requestTo(RESPONSES_URL)).andRespond(request -> {
            throw new SocketTimeoutException("read timed out");
        });

        assertThatThrownBy(() -> client.generate(prompt)).isInstanceOf(LlmUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("401(API 키 문제)은 다시 보내도 같아서 재시도하지 않는다")
    void unauthorized_doesNotRetry() {
        server.expect(once(), requestTo(RESPONSES_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.generate(prompt)).isInstanceOf(LlmUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("400(잘못된 요청)은 다시 보내도 같아서 재시도하지 않는다")
    void badRequest_doesNotRetry() {
        server.expect(once(), requestTo(RESPONSES_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.generate(prompt)).isInstanceOf(LlmUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("모델이 응답을 거절(refusal)하면 재시도하지 않는다")
    void refusal_doesNotRetry() throws Exception {
        server.expect(once(), requestTo(RESPONSES_URL))
                .andRespond(withSuccess(refusalJson(), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generate(prompt)).isInstanceOf(LlmUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("추천 행동이 비어 있으면 유효하지 않은 응답으로 처리하고 재시도하지 않는다 (호출 비용은 이미 나감)")
    void emptyRecommendations_throwsWithoutRetry() throws Exception {
        server.expect(once(), requestTo(RESPONSES_URL))
                .andRespond(withSuccess(responseJson("completed", adviceJson(List.of())), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generate(prompt)).isInstanceOf(LlmUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("API 키가 없으면 요청을 보내지 않고 실패한다")
    void missingApiKey_doesNotSendRequest() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer emptyServer = MockRestServiceServer.bindTo(builder).build();
        OpenAiCoachingClient noKeyClient =
                new OpenAiCoachingClient(builder.build(), properties(""), objectMapper, Duration.ZERO);

        assertThatThrownBy(() -> noKeyClient.generate(prompt)).isInstanceOf(LlmUnavailableException.class);
        emptyServer.verify();
    }
}
