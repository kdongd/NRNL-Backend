package com.kdongdexample.norunnolifeexample.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdongdexample.norunnolifeexample.config.OpenAiProperties;
import com.kdongdexample.norunnolifeexample.domain.CoachingAdvice;
import com.kdongdexample.norunnolifeexample.exception.LlmUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiCoachingClient implements LlmCoachingClient {

    static final int MAX_RECOMMENDATIONS = 5;

    // 첫 호출 + 재시도 1회. 재시도도 비용이 들어서 1회로 제한한다.
    static final int MAX_ATTEMPTS = 2;
    private static final Duration DEFAULT_RETRY_BACKOFF = Duration.ofSeconds(1);

    // Structured Outputs: 모델이 이 스키마와 정확히 일치하는 JSON만 생성하도록 강제
    private static final Map<String, Object> ADVICE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "summary", Map.of("type", "string"),
                    "explanation", Map.of("type", "string"),
                    "recommendations", Map.of("type", "array", "items", Map.of("type", "string")),
                    "caution", Map.of("type", "string")
            ),
            "required", List.of("summary", "explanation", "recommendations", "caution"),
            "additionalProperties", false
    );

    private final RestClient restClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final Duration retryBackoff;

    @Autowired
    public OpenAiCoachingClient(@Qualifier("openAiRestClient") RestClient restClient,
                                OpenAiProperties properties,
                                ObjectMapper objectMapper) {
        this(restClient, properties, objectMapper, DEFAULT_RETRY_BACKOFF);
    }

    // 테스트에서 재시도 대기 시간을 0으로 줄이기 위한 생성자
    OpenAiCoachingClient(RestClient restClient,
                         OpenAiProperties properties,
                         ObjectMapper objectMapper,
                         Duration retryBackoff) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.retryBackoff = retryBackoff;
    }

    @Override
    public CoachingAdvice generate(LlmPrompt prompt) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new LlmUnavailableException("OpenAI API 키가 설정되지 않았습니다");
        }

        JsonNode response = callWithRetry(prompt);

        // 응답을 받은 뒤의 실패(미완료, refusal, 형식 오류, 추천 비어 있음)는 재시도하지 않는다.
        // 이미 호출 비용이 나갔고, 같은 프롬프트로 다시 보내도 비슷한 결과가 나올 가능성이 높다.
        return validate(parse(response));
    }

    // 일시적인 실패(타임아웃/연결 실패, 5xx, 429)만 1회 재시도한다.
    // 요청 자체가 잘못된 경우(401 키 문제, 400 잘못된 요청 등)는 다시 보내도 같은 결과라 바로 실패 처리한다.
    private JsonNode callWithRetry(LlmPrompt prompt) {
        for (int attempt = 1; ; attempt++) {
            try {
                return callOnce(prompt);
            } catch (RestClientResponseException e) {
                // 응답 본문에 요청 내용(사용자 데이터)이 섞일 수 있어서 상태 코드만 남깁니다.
                int status = e.getStatusCode().value();
                if (!isRetryableStatus(status) || attempt >= MAX_ATTEMPTS) {
                    log.warn("OpenAI 응답 오류 status={} attempt={}", status, attempt);
                    throw new LlmUnavailableException("OpenAI 응답 오류: " + status);
                }
                log.warn("OpenAI 응답 오류 status={}, 재시도합니다 (attempt={})", status, attempt);
            } catch (ResourceAccessException e) {
                // 타임아웃, 연결 실패 등 네트워크 I/O 오류
                if (attempt >= MAX_ATTEMPTS) {
                    log.warn("OpenAI 호출 실패: {} attempt={}", e.getClass().getSimpleName(), attempt);
                    throw new LlmUnavailableException("OpenAI 호출 실패");
                }
                log.warn("OpenAI 호출 실패: {}, 재시도합니다 (attempt={})", e.getClass().getSimpleName(), attempt);
            } catch (RestClientException e) {
                log.warn("OpenAI 호출 실패: {}", e.getClass().getSimpleName());
                throw new LlmUnavailableException("OpenAI 호출 실패");
            }
            sleepBeforeRetry();
        }
    }

    private JsonNode callOnce(LlmPrompt prompt) {
        return restClient.post()
                .uri("/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody(prompt))
                .retrieve()
                .body(JsonNode.class);
    }

    // 429(요청 한도 초과)와 5xx(서버 오류)만 일시적인 실패로 보고 재시도한다.
    private boolean isRetryableStatus(int status) {
        return status == 429 || status >= 500;
    }

    // 429는 바로 다시 보내면 또 거절되므로 잠깐 쉬었다가 재시도한다(백오프).
    private void sleepBeforeRetry() {
        if (retryBackoff.isZero()) {
            return;
        }
        try {
            Thread.sleep(retryBackoff.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmUnavailableException("OpenAI 재시도 대기 중 중단되었습니다");
        }
    }

    private Map<String, Object> requestBody(LlmPrompt prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("input", List.of(
                Map.of("role", "system", "content", prompt.system()),
                Map.of("role", "user", "content", prompt.user())
        ));
        body.put("text", Map.of("format", Map.of(
                "type", "json_schema",
                "name", "coaching_advice",
                "strict", true,
                "schema", ADVICE_SCHEMA
        )));
        body.put("max_output_tokens", properties.maxOutputTokens());
        // 사용자 운동 데이터가 OpenAI 쪽에 저장되지 않도록 합니다.
        body.put("store", false);
        if (StringUtils.hasText(properties.reasoningEffort())) {
            body.put("reasoning", Map.of("effort", properties.reasoningEffort()));
        }
        return body;
    }

    private CoachingAdvice parse(JsonNode response) {
        if (response == null) {
            throw new LlmUnavailableException("OpenAI 응답이 비어 있습니다");
        }

        String status = response.path("status").asText();
        if (!"completed".equals(status)) {
            log.warn("OpenAI 응답 미완료 status={}", status);
            throw new LlmUnavailableException("응답이 완료되지 않았습니다: " + status);
        }

        // output 배열에는 reasoning, message 등 여러 항목이 올 수 있어서 message의 output_text만 읽음
        for (JsonNode item : response.path("output")) {
            if (!"message".equals(item.path("type").asText())) {
                continue;
            }
            for (JsonNode content : item.path("content")) {
                String type = content.path("type").asText();
                if ("refusal".equals(type)) {
                    throw new LlmUnavailableException("모델이 응답을 거절했습니다");
                }
                if ("output_text".equals(type)) {
                    try {
                        return objectMapper.readValue(content.path("text").asText(), CoachingAdvice.class);
                    } catch (JsonProcessingException e) {
                        throw new LlmUnavailableException("코칭 응답 JSON 형식이 올바르지 않습니다");
                    }
                }
            }
        }
        throw new LlmUnavailableException("응답에 코칭 내용이 없습니다");
    }

    private CoachingAdvice validate(CoachingAdvice advice) {
        if (advice == null || !StringUtils.hasText(advice.summary()) || !StringUtils.hasText(advice.explanation())) {
            throw new LlmUnavailableException("코칭 요약 또는 설명이 비어 있습니다");
        }

        List<String> recommendations = advice.recommendations() == null
                ? List.of()
                : advice.recommendations().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .limit(MAX_RECOMMENDATIONS)
                .toList();
        if (recommendations.isEmpty()) {
            throw new LlmUnavailableException("추천 행동이 비어 있습니다");
        }

        String caution = advice.caution() == null ? "" : advice.caution().trim();
        return new CoachingAdvice(advice.summary().trim(), advice.explanation().trim(), recommendations, caution);
    }
}
