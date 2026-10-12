package com.kdongdexample.norunnolifeexample.domain;

import java.util.List;

// LLM이 생성한 코칭 문장. 판정 단계(level)는 포함하지 않고 서버의 룰 엔진만 결정.
public record CoachingAdvice(
        String summary,
        String explanation,
        List<String> recommendations,
        String caution
) {}
