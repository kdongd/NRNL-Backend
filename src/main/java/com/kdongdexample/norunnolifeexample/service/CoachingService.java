package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.CoachingAdvice;
import com.kdongdexample.norunnolifeexample.domain.CoachingAssessment;
import com.kdongdexample.norunnolifeexample.domain.CoachingContext;
import com.kdongdexample.norunnolifeexample.domain.CoachingLevel;
import com.kdongdexample.norunnolifeexample.domain.CoachingResult;
import com.kdongdexample.norunnolifeexample.domain.CoachingStatus;
import com.kdongdexample.norunnolifeexample.exception.LlmUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


// coach()는 외부 LLM 호출(최대 수십 초) 동안 DB 커넥션을 잡고 있지 않도록,
// 데이터 조회는 CoachingDataReader의 읽기 전용 트랜잭션에서 끝내고 LLM 호출은 트랜잭션 밖에서 합니다.
@Slf4j
@Service
public class CoachingService {

    private final CoachingDataReader dataReader;
    private final CoachingPromptBuilder promptBuilder;
    private final LlmCoachingClient llmClient;

    public CoachingService(CoachingDataReader dataReader,
                           CoachingPromptBuilder promptBuilder,
                           LlmCoachingClient llmClient) {
        this.dataReader = dataReader;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
    }

    public CoachingAssessment assess(Long userId) {
        return dataReader.assess(userId);
    }

    public CoachingResult coach(Long userId) {
        CoachingContext context = dataReader.readContext(userId);
        CoachingAssessment assessment = context.assessment();

        // 판단할 데이터가 없으면 비용을 들여 LLM을 부를 이유가 없다.
        if (assessment.level() == CoachingLevel.INSUFFICIENT_DATA) {
            return new CoachingResult(assessment, null, CoachingStatus.SKIPPED_INSUFFICIENT_DATA);
        }

        try {
            CoachingAdvice advice = llmClient.generate(promptBuilder.build(context));
            return new CoachingResult(assessment, advice, CoachingStatus.GENERATED);
        } catch (LlmUnavailableException e) {
            // LLM이 실패해도 안전 판정(룰 결과)은 항상 사용자에게 전달.
            log.warn("AI 코칭 생성 실패, 룰 판정만 반환합니다: {}", e.getMessage());
            return new CoachingResult(assessment, null, CoachingStatus.UNAVAILABLE);
        }
    }
}
