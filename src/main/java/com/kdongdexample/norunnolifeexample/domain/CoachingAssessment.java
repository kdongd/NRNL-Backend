package com.kdongdexample.norunnolifeexample.domain;

import java.time.LocalDate;
import java.util.List;

public record CoachingAssessment(
        CoachingLevel level,
        List<CoachingReason> reasons,
        LocalDate evaluatedFrom,
        LocalDate evaluatedTo
) {}
