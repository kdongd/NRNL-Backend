package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.BodyPart;
import com.kdongdexample.norunnolifeexample.domain.CoachingAssessment;
import com.kdongdexample.norunnolifeexample.domain.CoachingLevel;
import com.kdongdexample.norunnolifeexample.domain.RunningWorkout;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.domain.WorkoutCondition;
import com.kdongdexample.norunnolifeexample.repository.UserProfileRepository;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import com.kdongdexample.norunnolifeexample.repository.WorkoutConditionRepository;
import com.kdongdexample.norunnolifeexample.repository.WorkoutQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoachingDataReaderTest {

    private static final Long USER_ID = 1L;

    // UTC 2026-10-12 22:23 = KST 2026-10-13 07:23
    private static final Instant SERVER_INSTANT = Instant.parse("2026-10-12T22:23:00Z");

    @Mock
    private WorkoutConditionRepository conditionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private WorkoutQueryRepository workoutQueryRepository;

    @Test
    @DisplayName("서버 시각이 UTC 밤 10시여도 서비스 타임존(Asia/Seoul) 기준 오늘 기록으로 판정해 통증 5면 STOP")
    void assess_serverUtcNight_usesServiceZoneToday() {
        Clock clock = Clock.fixed(SERVER_INSTANT, ZoneId.of("Asia/Seoul"));
        CoachingDataReader reader = new CoachingDataReader(
                conditionRepository, userRepository, userProfileRepository, workoutQueryRepository,
                new CoachingRuleEngine(), clock);

        // 사용자가 한국 시각 오늘(10/13) 오전 7시에 운동하고 통증 5를 기록
        User owner = User.create("clock@test.com", "encoded-password");
        RunningWorkout workout = RunningWorkout.create(
                owner, 30, null, LocalDateTime.of(2026, 10, 13, 7, 0), 5.0, "한강", 300);
        WorkoutCondition condition = WorkoutCondition.create(workout, 5, 7, Map.of(BodyPart.KNEE, 5), null);

        when(userRepository.existsById(USER_ID)).thenReturn(true);
        // 오늘 = 10/13 기준 최근 14일: 9/30 00:00 이상 ~ 10/14 00:00 미만으로 조회해야 한다
        when(conditionRepository.findAllByOwnerAndWorkoutDateRange(
                USER_ID,
                LocalDateTime.of(2026, 9, 30, 0, 0),
                LocalDateTime.of(2026, 10, 14, 0, 0)))
                .thenReturn(List.of(condition));

        CoachingAssessment result = reader.assess(USER_ID);

        assertThat(result.evaluatedTo()).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(result.level()).isEqualTo(CoachingLevel.STOP);
    }
}
