package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.CoachingAdvice;
import com.kdongdexample.norunnolifeexample.domain.CoachingAssessment;
import com.kdongdexample.norunnolifeexample.domain.CoachingContext;
import com.kdongdexample.norunnolifeexample.domain.CoachingLevel;
import com.kdongdexample.norunnolifeexample.domain.CoachingResult;
import com.kdongdexample.norunnolifeexample.domain.CoachingStatus;
import com.kdongdexample.norunnolifeexample.exception.LlmUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CoachingServiceTest {

    @Mock
    private CoachingDataReader dataReader;

    @Mock
    private CoachingPromptBuilder promptBuilder;

    @Mock
    private LlmCoachingClient llmClient;

    @InjectMocks
    private CoachingService coachingService;

    private CoachingContext context(CoachingLevel level) {
        CoachingAssessment assessment = new CoachingAssessment(
                level, List.of(), LocalDate.of(2026, 9, 29), LocalDate.of(2026, 10, 12));
        return new CoachingContext(assessment, null, List.of(), List.of(), List.of());
    }

    @Test
    @DisplayName("데이터가 부족하면 LLM을 호출하지 않는다")
    void insufficientData_doesNotCallLlm() {
        given(dataReader.readContext(1L)).willReturn(context(CoachingLevel.INSUFFICIENT_DATA));

        CoachingResult result = coachingService.coach(1L);

        assertThat(result.status()).isEqualTo(CoachingStatus.SKIPPED_INSUFFICIENT_DATA);
        assertThat(result.advice()).isNull();
        then(promptBuilder).shouldHaveNoInteractions();
        then(llmClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("LLM 호출에 성공하면 룰 판정과 AI 코칭을 함께 반환한다")
    void llmSuccess_returnsAdviceWithAssessment() {
        CoachingContext context = context(CoachingLevel.REST);
        LlmPrompt prompt = new LlmPrompt("system", "user");
        CoachingAdvice advice = new CoachingAdvice("오늘은 쉬어요", "최근 통증이 이어졌어요", List.of("스트레칭 10분"), "");
        given(dataReader.readContext(1L)).willReturn(context);
        given(promptBuilder.build(context)).willReturn(prompt);
        given(llmClient.generate(prompt)).willReturn(advice);

        CoachingResult result = coachingService.coach(1L);

        assertThat(result.status()).isEqualTo(CoachingStatus.GENERATED);
        assertThat(result.assessment().level()).isEqualTo(CoachingLevel.REST);
        assertThat(result.advice()).isEqualTo(advice);
    }

    @Test
    @DisplayName("LLM 호출이 실패해도 룰 판정은 그대로 반환한다")
    void llmFailure_returnsAssessmentOnly() {
        CoachingContext context = context(CoachingLevel.STOP);
        LlmPrompt prompt = new LlmPrompt("system", "user");
        given(dataReader.readContext(1L)).willReturn(context);
        given(promptBuilder.build(context)).willReturn(prompt);
        given(llmClient.generate(prompt)).willThrow(new LlmUnavailableException("timeout"));

        CoachingResult result = coachingService.coach(1L);

        assertThat(result.status()).isEqualTo(CoachingStatus.UNAVAILABLE);
        assertThat(result.assessment().level()).isEqualTo(CoachingLevel.STOP);
        assertThat(result.advice()).isNull();
    }
}
