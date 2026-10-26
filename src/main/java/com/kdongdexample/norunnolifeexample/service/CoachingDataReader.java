package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.CoachingAssessment;
import com.kdongdexample.norunnolifeexample.domain.CoachingContext;
import com.kdongdexample.norunnolifeexample.domain.CoachingContext.MemoSnapshot;
import com.kdongdexample.norunnolifeexample.domain.CoachingContext.ProfileSnapshot;
import com.kdongdexample.norunnolifeexample.domain.CoachingContext.WorkoutSnapshot;
import com.kdongdexample.norunnolifeexample.domain.DailyCondition;
import com.kdongdexample.norunnolifeexample.domain.WorkoutCondition;
import com.kdongdexample.norunnolifeexample.exception.AuthenticatedUserNotFoundException;
import com.kdongdexample.norunnolifeexample.repository.UserProfileRepository;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import com.kdongdexample.norunnolifeexample.repository.WorkoutConditionRepository;
import com.kdongdexample.norunnolifeexample.repository.WorkoutQueryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

// 코칭에 필요한 데이터를 읽기 전용 트랜잭션 안에서 모두 조회해 값 객체로 넘깁니다.
// LLM 호출은 이 트랜잭션이 끝난 뒤 CoachingService에서 합니다.
@Component
@Transactional(readOnly = true)
public class CoachingDataReader {

    static final int MAX_RECENT_WORKOUTS = 20;
    static final int MAX_MEMOS = 5;

    private final WorkoutConditionRepository conditionRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final WorkoutQueryRepository workoutQueryRepository;
    private final CoachingRuleEngine ruleEngine;
    // 현재 시각은 Clock으로만 읽는다. 테스트에서 고정 시각으로 바꿔 끼울 수 있습니다.
    private final Clock clock;

    public CoachingDataReader(WorkoutConditionRepository conditionRepository,
                              UserRepository userRepository,
                              UserProfileRepository userProfileRepository,
                              WorkoutQueryRepository workoutQueryRepository,
                              CoachingRuleEngine ruleEngine,
                              Clock clock) {
        this.conditionRepository = conditionRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.workoutQueryRepository = workoutQueryRepository;
        this.ruleEngine = ruleEngine;
        this.clock = clock;
    }

    public CoachingAssessment assess(Long userId) {
        validateUserExists(userId);
        LocalDate today = LocalDate.now(clock);
        return ruleEngine.evaluate(DailyCondition.aggregate(findConditions(userId, today)), today);
    }

    public CoachingContext readContext(Long userId) {
        validateUserExists(userId);
        // 현재 시각을 한 번만 읽어서 today와 조회 상한에 같이 쓴다.
        // 따로 읽으면 자정 직전에 그 사이를 넘어가 둘이 다른 날이 될 수 있다.
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        List<WorkoutCondition> conditions = findConditions(userId, today);
        List<DailyCondition> dailyConditions = DailyCondition.aggregate(conditions);
        CoachingAssessment assessment = ruleEngine.evaluate(dailyConditions, today);

        ProfileSnapshot profile = userProfileRepository.findByUserId(userId)
                .map(ProfileSnapshot::from)
                .orElse(null);

        List<WorkoutSnapshot> recentWorkouts = workoutQueryRepository.search(
                        userId, null,
                        ruleEngine.windowStart(today).atStartOfDay(),
                        now,
                        PageRequest.of(0, MAX_RECENT_WORKOUTS, Sort.by(Sort.Direction.DESC, "workoutDateTime")))
                .getContent().stream()
                .map(WorkoutSnapshot::from)
                .toList();

        List<MemoSnapshot> memos = conditions.stream()
                .filter(c -> StringUtils.hasText(c.getMemo()))
                .sorted(Comparator.comparing((WorkoutCondition c) -> c.getWorkout().getWorkoutDateTime()).reversed())
                .limit(MAX_MEMOS)
                .map(c -> new MemoSnapshot(c.getWorkout().getWorkoutDateTime().toLocalDate(), c.getMemo().trim()))
                .toList();

        return new CoachingContext(assessment, profile, dailyConditions, recentWorkouts, memos);
    }

    private List<WorkoutCondition> findConditions(Long userId, LocalDate today) {
        return conditionRepository.findAllByOwnerAndWorkoutDateRange(
                userId,
                ruleEngine.windowStart(today).atStartOfDay(),
                today.plusDays(1).atStartOfDay());
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new AuthenticatedUserNotFoundException(userId);
        }
    }
}
