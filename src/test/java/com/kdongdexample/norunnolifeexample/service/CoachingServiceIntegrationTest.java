package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.BodyPart;
import com.kdongdexample.norunnolifeexample.domain.CoachingAdvice;
import com.kdongdexample.norunnolifeexample.domain.CoachingAssessment;
import com.kdongdexample.norunnolifeexample.domain.CoachingLevel;
import com.kdongdexample.norunnolifeexample.domain.CoachingReason;
import com.kdongdexample.norunnolifeexample.domain.CoachingRule;
import com.kdongdexample.norunnolifeexample.domain.RunningWorkout;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.domain.Workout;
import com.kdongdexample.norunnolifeexample.dto.WorkoutConditionRequest;
import com.kdongdexample.norunnolifeexample.dto.WorkoutConditionRequest.PainRequest;
import com.kdongdexample.norunnolifeexample.repository.JpaWorkoutRepository;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Testcontainers
@SpringBootTest
@Transactional
class CoachingServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private CoachingService coachingService;

    @Autowired
    private WorkoutConditionService conditionService;

    @Autowired
    private JpaWorkoutRepository jpaWorkoutRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    // 실제 OpenAI를 부르지 않고, 몇 번 호출됐는지만 센다.
    @MockitoBean
    private LlmCoachingClient llmClient;

    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.create("coaching-owner@test.com", "encoded-password"));
        otherUser = userRepository.save(User.create("coaching-other@test.com", "encoded-password"));
    }

    // daysAgo일 전 오전 9시에 운동 기록 + 컨디션 저장. 수정(PUT) 테스트용으로 운동 id를 돌려준다.
    private Long recordCondition(User user, int daysAgo, int rpe, int body, List<PainRequest> pains) {
        Workout workout = jpaWorkoutRepository.save(RunningWorkout.create(
                user, 30, null, LocalDate.now().minusDays(daysAgo).atTime(9, 0), 5.0, "한강", 300));
        conditionService.upsert(workout.getId(), new WorkoutConditionRequest(rpe, body, pains, null), user.getId());
        return workout.getId();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("최근 입력일 3개 모두 통증 강도 2 이상이면 REST로 판정된다")
    void painPersistent_rest() {
        recordCondition(owner, 0, 5, 7, List.of(new PainRequest(BodyPart.KNEE, 2)));
        recordCondition(owner, 2, 5, 7, List.of(new PainRequest(BodyPart.KNEE, 2)));
        recordCondition(owner, 4, 5, 7, List.of(new PainRequest(BodyPart.ANKLE, 2)));
        flushAndClear();

        CoachingAssessment result = coachingService.assess(owner.getId());

        assertThat(result.level()).isEqualTo(CoachingLevel.REST);
        assertThat(result.reasons()).extracting(CoachingReason::rule).containsExactly(CoachingRule.PAIN_PERSISTENT);
    }

    @Test
    @DisplayName("같은 날 기록 2개는 하나의 입력일로 합쳐진다 (입력일 2개 → 데이터 부족)")
    void sameDayRecords_countedAsOneDay() {
        recordCondition(owner, 0, 5, 7, List.of());
        recordCondition(owner, 0, 6, 6, List.of());
        recordCondition(owner, 1, 5, 7, List.of());
        flushAndClear();

        CoachingAssessment result = coachingService.assess(owner.getId());

        assertThat(result.level()).isEqualTo(CoachingLevel.INSUFFICIENT_DATA);
    }

    @Test
    @DisplayName("다른 회원의 기록과 14일보다 오래된 기록은 판정에 포함되지 않는다")
    void excludesOtherUsersAndOldRecords() {
        recordCondition(owner, 0, 5, 7, List.of());
        recordCondition(owner, 1, 5, 7, List.of());
        recordCondition(owner, 20, 5, 7, List.of());
        recordCondition(otherUser, 0, 10, 1, List.of(new PainRequest(BodyPart.KNEE, 5)));
        recordCondition(otherUser, 1, 10, 1, List.of(new PainRequest(BodyPart.KNEE, 5)));
        flushAndClear();

        CoachingAssessment result = coachingService.assess(owner.getId());

        assertThat(result.level()).isEqualTo(CoachingLevel.INSUFFICIENT_DATA);
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    @DisplayName("컨디션을 PUT으로 수정하면 코칭 캐시가 무효화되어 LLM을 다시 호출한다")
    void updateCondition_invalidatesCoachingCache() {
        given(llmClient.generate(any())).willReturn(
                new CoachingAdvice("좋은 페이스예요", "컨디션이 안정적이에요", List.of("지금처럼 유지"), ""));

        recordCondition(owner, 4, 5, 7, List.of());
        recordCondition(owner, 2, 5, 7, List.of());
        Long todayWorkoutId = recordCondition(owner, 0, 5, 7, List.of(new PainRequest(BodyPart.KNEE, 1)));
        flushAndClear();

        // 1) 첫 코칭 → LLM 1회 호출, 결과가 캐시에 저장된다
        coachingService.coach(owner.getId());
        flushAndClear();

        // 2) 같은 컨디션을 PUT으로 수정 (통증 1 → 5). 컨디션 id는 그대로다.
        conditionService.upsert(todayWorkoutId,
                new WorkoutConditionRequest(5, 7, List.of(new PainRequest(BodyPart.KNEE, 5)), null),
                owner.getId());
        flushAndClear();

        // 3) 다시 코칭 → 입력이 바뀌었으므로 캐시를 쓰지 않고 LLM을 다시 불러야 한다
        coachingService.coach(owner.getId());

        verify(llmClient, times(2)).generate(any());
    }
}
