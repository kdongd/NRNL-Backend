package com.kdongdexample.norunnolifeexample.domain;

public record CoachingResult(
        CoachingAssessment assessment,
        CoachingAdvice advice,
        CoachingStatus status
) {}
