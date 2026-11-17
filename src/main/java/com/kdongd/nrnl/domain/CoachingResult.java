package com.kdongd.nrnl.domain;

public record CoachingResult(
        CoachingAssessment assessment,
        CoachingAdvice advice,
        CoachingStatus status
) {}
