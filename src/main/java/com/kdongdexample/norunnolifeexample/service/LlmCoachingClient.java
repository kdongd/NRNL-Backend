package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.CoachingAdvice;

// LLM 제공사를 바꾸거나 테스트에서 Mock으로 대체할 수 있도록 인터페이스로 분리
public interface LlmCoachingClient {
    CoachingAdvice generate(LlmPrompt prompt);
}
