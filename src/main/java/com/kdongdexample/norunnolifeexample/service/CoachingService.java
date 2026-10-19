package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.CoachingAdvice;
import com.kdongdexample.norunnolifeexample.domain.CoachingAssessment;
import com.kdongdexample.norunnolifeexample.domain.CoachingContext;
import com.kdongdexample.norunnolifeexample.domain.CoachingLevel;
import com.kdongdexample.norunnolifeexample.domain.CoachingResult;
import com.kdongdexample.norunnolifeexample.domain.CoachingStatus;
import com.kdongdexample.norunnolifeexample.exception.LlmUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Optional;


// coach()는 외부 LLM 호출(최대 수십 초) 동안 DB 커넥션을 잡고 있지 않도록,
// 데이터 조회는 CoachingDataReader의 읽기 전용 트랜잭션에서 끝내고 LLM 호출은 트랜잭션 밖에서 합니다.
@Slf4j
@Service
public class CoachingService {

    private final CoachingDataReader dataReader;
    private final CoachingPromptBuilder promptBuilder;
    private final LlmCoachingClient llmClient;
    private final CoachingCacheStore cacheStore;

    public CoachingService(CoachingDataReader dataReader,
                           CoachingPromptBuilder promptBuilder,
                           LlmCoachingClient llmClient,
                           CoachingCacheStore cacheStore) {
        this.dataReader = dataReader;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.cacheStore = cacheStore;
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

        // 새 컨디션을 저장하기 전까지는 코칭 입력이 같으므로, 마지막 컨디션 id가 같으면 저장된 코칭을 돌려준다.
        Long lastConditionId = cacheStore.findLastConditionId(userId).orElse(null);
        if (lastConditionId != null) {
            Optional<CoachingAdvice> cached = cacheStore.find(userId, lastConditionId);
            if (cached.isPresent()) {
                return new CoachingResult(assessment, cached.get(), CoachingStatus.GENERATED);
            }
        }

        CoachingAdvice advice;
        try {
            advice = llmClient.generate(promptBuilder.build(context));
        } catch (LlmUnavailableException e) {
            // LLM이 실패해도 안전 판정(룰 결과)은 항상 사용자에게 전달. 실패 결과는 캐시하지 않는다.
            log.warn("AI 코칭 생성 실패, 룰 판정만 반환합니다: {}", e.getMessage());
            return new CoachingResult(assessment, null, CoachingStatus.UNAVAILABLE);
        }

        if (lastConditionId != null) {
            saveCache(userId, lastConditionId, advice);
        }
        return new CoachingResult(assessment, advice, CoachingStatus.GENERATED);
    }

    // 캐시 저장에 실패해도 이미 생성한 코칭은 사용자에게 그대로 돌려준다.
    private void saveCache(Long userId, Long lastConditionId, CoachingAdvice advice) {
        try {
            cacheStore.save(userId, lastConditionId, advice);
        } catch (DataAccessException e) {
            log.warn("코칭 캐시 저장 실패 (userId={}): {}", userId, e.getClass().getSimpleName());
        }
    }
}
