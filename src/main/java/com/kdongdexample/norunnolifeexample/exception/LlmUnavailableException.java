package com.kdongdexample.norunnolifeexample.exception;

// LLM 호출 실패, 타임아웃, 응답 형식 오류 등 AI 코칭을 생성할 수 없는 모든 경우.
// CoachingService가 잡아서 룰 판정만 반환하므로 API 에러로 나가지 않습니다.
public class LlmUnavailableException extends RuntimeException {
    public LlmUnavailableException(String message) {
        super(message);
    }
}
