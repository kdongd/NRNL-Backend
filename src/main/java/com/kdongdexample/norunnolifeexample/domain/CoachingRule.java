package com.kdongdexample.norunnolifeexample.domain;

public enum CoachingRule {
    SEVERE_PAIN(CoachingLevel.STOP),             // 최근 3일 내 마지막 입력일에 통증 강도 5
    MODERATE_PAIN(CoachingLevel.REDUCE),         // 최근 3일 내 마지막 입력일에 통증 강도 4
    PAIN_PERSISTENT(CoachingLevel.REST),         // 최근 입력일 3개 모두 통증 있음
    HIGH_RPE_REPEATED(CoachingLevel.REDUCE),     // 최근 입력일 5개 중 3개 이상 RPE 9 이상
    LOW_BODY_CONDITION(CoachingLevel.REST);      // 최근 입력일 3개 모두 몸 상태 3 이하

    private final CoachingLevel level;

    CoachingRule(CoachingLevel level) {
        this.level = level;
    }

    public CoachingLevel level() {
        return level;
    }
}
