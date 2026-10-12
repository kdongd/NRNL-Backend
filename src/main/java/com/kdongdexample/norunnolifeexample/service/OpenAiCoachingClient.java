package com.kdongdexample.norunnolifeexample.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdongdexample.norunnolifeexample.config.OpenAiProperties;
import com.kdongdexample.norunnolifeexample.domain.CoachingAdvice;
import com.kdongdexample.norunnolifeexample.exception.LlmUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiCoachingClient implements LlmCoachingClient {

    static final int MAX_RECOMMENDATIONS = 5;

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

    public OpenAiCoachingClient(@Qualifier("openAiRestClient") RestClient restClient,
                                OpenAiProperties properties,
                                ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public CoachingAdvice generate(LlmPrompt prompt) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new LlmUnavailableException("OpenAI API 키가 설정되지 않았습니다");
        }

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(prompt))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            // 응답 본문에 요청 내용(사용자 데이터)이 섞일 수 있어서 상태 코드만 남깁니다.
            log.warn("OpenAI 응답 오류 status={}", e.getStatusCode().value());
            throw new LlmUnavailableException("OpenAI 응답 오류: " + e.getStatusCode().value());
        } catch (RestClientException e) {
            log.warn("OpenAI 호출 실패: {}", e.getClass().getSimpleName());
            throw new LlmUnavailableException("OpenAI 호출 실패");
        }

        return validate(parse(response));
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
