package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.CoachingAssessment;
import com.kdongdexample.norunnolifeexample.domain.DailyCondition;
import com.kdongdexample.norunnolifeexample.domain.WorkoutCondition;
import com.kdongdexample.norunnolifeexample.exception.AuthenticatedUserNotFoundException;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import com.kdongdexample.norunnolifeexample.repository.WorkoutConditionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CoachingService {

    private final WorkoutConditionRepository conditionRepository;
    private final UserRepository userRepository;
    private final CoachingRuleEngine ruleEngine;

    public CoachingService(WorkoutConditionRepository conditionRepository,
                           UserRepository userRepository,
                           CoachingRuleEngine ruleEngine) {
        this.conditionRepository = conditionRepository;
        this.userRepository = userRepository;
        this.ruleEngine = ruleEngine;
    }

    public CoachingAssessment assess(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new AuthenticatedUserNotFoundException(userId);
        }

        LocalDate today = LocalDate.now();
        List<WorkoutCondition> conditions = conditionRepository.findAllByOwnerAndWorkoutDateRange(
                userId,
                ruleEngine.windowStart(today).atStartOfDay(),
                today.plusDays(1).atStartOfDay());

        return ruleEngine.evaluate(DailyCondition.aggregate(conditions), today);
    }
}
