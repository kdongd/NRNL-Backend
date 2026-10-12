package com.kdongdexample.norunnolifeexample.domain;

public enum CoachingStatus {
    GENERATED,                  // AI 코칭 생성 성공
    SKIPPED_INSUFFICIENT_DATA,  // 데이터 부족으로 LLM을 호출하지 않음
    UNAVAILABLE                 // LLM 호출 실패 — 룰 판정만 제공
}
