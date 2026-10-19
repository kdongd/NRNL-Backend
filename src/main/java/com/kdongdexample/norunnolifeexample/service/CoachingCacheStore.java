package com.kdongdexample.norunnolifeexample.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdongdexample.norunnolifeexample.domain.CoachingAdvice;
import com.kdongdexample.norunnolifeexample.domain.CoachingCache;
import com.kdongdexample.norunnolifeexample.repository.CoachingCacheRepository;
import com.kdongdexample.norunnolifeexample.repository.WorkoutConditionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// AI 코칭 결과 캐시 조회/저장. 키는 user_id + 마지막 컨디션 id.
@Slf4j
@Component
public class CoachingCacheStore {

    private final CoachingCacheRepository cacheRepository;
    private final WorkoutConditionRepository conditionRepository;
    private final ObjectMapper objectMapper;

    public CoachingCacheStore(CoachingCacheRepository cacheRepository,
                              WorkoutConditionRepository conditionRepository,
                              ObjectMapper objectMapper) {
        this.cacheRepository = cacheRepository;
        this.conditionRepository = conditionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Optional<Long> findLastConditionId(Long userId) {
        return conditionRepository.findLastConditionIdByOwner(userId);
    }

    // 저장된 코칭이 있고, 저장 당시 마지막 컨디션 id가 지금과 같을 때만 돌려준다.
    @Transactional(readOnly = true)
    public Optional<CoachingAdvice> find(Long userId, Long lastConditionId) {
        return cacheRepository.findByUserId(userId)
                .filter(cache -> cache.isFor(lastConditionId))
                .flatMap(cache -> fromJson(cache.getAdvice()));
    }

    // 회원당 1행이라 이미 있으면 덮어쓰고, 없으면 새로 만든다.
    @Transactional
    public void save(Long userId, Long lastConditionId, CoachingAdvice advice) {
        String json = toJson(advice);
        Optional<CoachingCache> existing = cacheRepository.findByUserId(userId);
        if (existing.isPresent()) {
            existing.get().update(lastConditionId, json);
            return;
        }
        cacheRepository.saveAndFlush(CoachingCache.create(userId, lastConditionId, json));
    }

    private String toJson(CoachingAdvice advice) {
        try {
            return objectMapper.writeValueAsString(advice);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("코칭 캐시 저장용 JSON 변환에 실패했습니다", e);
        }
    }

    // 저장된 JSON을 읽지 못하면 캐시가 없는 것으로 보고 새로 생성하게 한다.
    private Optional<CoachingAdvice> fromJson(String json) {
        try {
            return Optional.of(objectMapper.readValue(json, CoachingAdvice.class));
        } catch (JsonProcessingException e) {
            log.warn("코칭 캐시 JSON을 읽지 못해 새로 생성합니다");
            return Optional.empty();
        }
    }
}
