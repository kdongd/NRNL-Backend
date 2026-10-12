package com.kdongdexample.norunnolifeexample.domain;

// 룰을 판정 단계. severity가 클수록 심각한것으로 판단하고 여러 룰이 걸리면 가장 큰 것이 최종 단계가 됩니다.
public enum CoachingLevel {
    STOP(3),               // 운동 중단, 병원 진료 권유
    REST(2),               // 휴식 권고
    REDUCE(1),             // 강도 줄이기
    NORMAL(0),             // 걸린 룰 없음
    INSUFFICIENT_DATA(-1); // 판단할 데이터 부족

    private final int severity;

    CoachingLevel(int severity) {
        this.severity = severity;
    }

    public int severity() {
        return severity;
    }
}
