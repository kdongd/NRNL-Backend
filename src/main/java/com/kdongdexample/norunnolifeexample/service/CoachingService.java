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

@Slf4j
@Service
public class CoachingService {

    private final CoachingDataReader dataReader;
    private final CoachingPromptBuilder promptBuilder;
    private final LlmCoachingClient llmClient;
    private final CoachingCacheStore cacheStore;

    public CoachingService(
            CoachingDataReader dataReader,
            CoachingPromptBuilder promptBuilder,
            LlmCoachingClient llmClient,
            CoachingCacheStore cacheStore
    ) {
        this.dataReader = dataReader;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.cacheStore = cacheStore;
    }

    // 룰 기반 운동 상태 판정
    public CoachingAssessment assess(Long userId) {
        return dataReader.assess(userId);
    }

    // AI 코칭 생성 또는 기존 캐시 재사용
    public CoachingResult coach(Long userId) {

        // DB 조회 트랜잭션은 별도 빈에서 종료
        CoachingContext context = dataReader.readContext(userId);
        CoachingAssessment assessment = context.assessment();

        // 운동 기록이 부족하면 LLM을 호출하지 않는다.
        if (assessment.level() == CoachingLevel.INSUFFICIENT_DATA) {
            return new CoachingResult(
                    assessment,
                    null,
                    CoachingStatus.SKIPPED_INSUFFICIENT_DATA
            );
        }

        // 마지막 컨디션 ID 조회
        Long lastConditionId = cacheStore.findLastConditionId(userId)
                .orElse(null);

        // 유효한 캐시가 있다면 AI를 호출하지 않고 반환
        if (lastConditionId != null) {
            Optional<CoachingAdvice> cached =
                    cacheStore.find(userId, lastConditionId);

            if (cached.isPresent()) {
                return new CoachingResult(
                        assessment,
                        cached.get(),
                        CoachingStatus.GENERATED
                );
            }
        }

        // 캐시가 없을 때만 LLM 호출
        CoachingAdvice advice;

        try {
            advice = llmClient.generate(
                    promptBuilder.build(context)
            );
        } catch (LlmUnavailableException e) {

            log.warn(
                    "AI 코칭 생성 실패, 룰 판정만 반환합니다: {}",
                    e.getMessage()
            );

            return new CoachingResult(
                    assessment,
                    null,
                    CoachingStatus.UNAVAILABLE
            );
        }

        // 생성한 코칭을 DB에 저장
        if (lastConditionId != null) {
            saveCache(userId, lastConditionId, advice);
        }

        return new CoachingResult(
                assessment,
                advice,
                CoachingStatus.GENERATED
        );
    }

    // 추가: 기존 AI 코칭 조회
    // LLM 호출 없이 DB에 저장된 결과만 가져온다.
    public Optional<CoachingResult> getLatestCoaching(Long userId) {

        // 현재 운동 상태 판정
        CoachingAssessment assessment = dataReader.assess(userId);

        // 판정에 필요한 데이터가 부족하면 기존 코칭을 표시하지 않는다.
        if (assessment.level() == CoachingLevel.INSUFFICIENT_DATA) {
            return Optional.empty();
        }

        // 현재 사용자의 마지막 컨디션 ID 조회
        Optional<Long> lastConditionId =
                cacheStore.findLastConditionId(userId);

        if (lastConditionId.isEmpty()) {
            return Optional.empty();
        }

        // 마지막 컨디션 ID가 일치하는 코칭 캐시 조회
        return cacheStore.find(userId, lastConditionId.get())
                .map(advice -> new CoachingResult(
                        assessment,
                        advice,
                        CoachingStatus.GENERATED
                ));
    }

    // 캐시 저장 실패가 코칭 응답 실패로 이어지지 않도록 처리
    private void saveCache(
            Long userId,
            Long lastConditionId,
            CoachingAdvice advice
    ) {
        try {
            cacheStore.save(userId, lastConditionId, advice);
        } catch (DataAccessException e) {
            log.warn(
                    "코칭 캐시 저장 실패 (userId={}): {}",
                    userId,
                    e.getClass().getSimpleName()
            );
        }
    }
}
