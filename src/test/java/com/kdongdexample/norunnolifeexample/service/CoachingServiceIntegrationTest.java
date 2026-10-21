
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
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

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

    // 실제 OpenAI API를 호출하지 않고 호출 횟수만 검증한다.
    @MockitoBean
    private LlmCoachingClient llmClient;

    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(
                User.create(
                        "coaching-owner@test.com",
                        "encoded-password"
                )
        );

        otherUser = userRepository.save(
                User.create(
                        "coaching-other@test.com",
                        "encoded-password"
                )
        );
    }

    /**
     * 운동 기록과 컨디션을 저장한다.
     *
     * rpe: 운동 자각 강도 (1~5)
     * body: 신체 컨디션 (1~5)
     * daysAgo: 오늘 기준 며칠 전 운동인지
     *
     * 반환값: 생성된 운동 기록 ID
     */
    private Long recordCondition(
            User user,
            int daysAgo,
            int rpe,
            int body,
            List<PainRequest> pains
    ) {
        Workout workout = jpaWorkoutRepository.save(
                RunningWorkout.create(
                        user,
                        30,
                        null,
                        LocalDate.now()
                                .minusDays(daysAgo)
                                .atTime(9, 0),
                        5.0,
                        "한강",
                        300
                )
        );

        conditionService.upsert(
                workout.getId(),
                new WorkoutConditionRequest(
                        rpe,
                        body,
                        pains,
                        null
                ),
                user.getId()
        );

        return workout.getId();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("최근 입력일 3개 모두 통증 강도 2 이상이면 REST로 판정된다")
    void painPersistent_rest() {

        // RPE는 낮게 설정해 HIGH_RPE_REPEATED 규칙이 발동하지 않도록 한다.
        recordCondition(
                owner,
                0,
                3,
                4,
                List.of(new PainRequest(BodyPart.KNEE, 2))
        );

        recordCondition(
                owner,
                2,
                3,
                4,
                List.of(new PainRequest(BodyPart.KNEE, 2))
        );

        recordCondition(
                owner,
                4,
                3,
                4,
                List.of(new PainRequest(BodyPart.ANKLE, 2))
        );

        flushAndClear();

        CoachingAssessment result =
                coachingService.assess(owner.getId());

        assertThat(result.level())
                .isEqualTo(CoachingLevel.REST);

        assertThat(result.reasons())
                .extracting(CoachingReason::rule)
                .containsExactly(CoachingRule.PAIN_PERSISTENT);
    }

    @Test
    @DisplayName("같은 날 기록 2개는 하나의 입력일로 합쳐진다 (입력일 2개 → 데이터 부족)")
    void sameDayRecords_countedAsOneDay() {

        // 같은 날 운동 기록 2개
        recordCondition(
                owner,
                0,
                5,
                4,
                List.of()
        );

        recordCondition(
                owner,
                0,
                4,
                4,
                List.of()
        );

        // 다른 날짜 운동 기록 1개
        recordCondition(
                owner,
                1,
                5,
                4,
                List.of()
        );

        flushAndClear();

        CoachingAssessment result =
                coachingService.assess(owner.getId());

        // 운동 기록은 3개지만 운동한 날짜는 2일
        assertThat(result.level())
                .isEqualTo(CoachingLevel.INSUFFICIENT_DATA);
    }

    @Test
    @DisplayName("다른 회원의 기록과 14일보다 오래된 기록은 판정에 포함되지 않는다")
    void excludesOtherUsersAndOldRecords() {

        // 본인의 최근 기록 2개
        recordCondition(
                owner,
                0,
                5,
                4,
                List.of()
        );

        recordCondition(
                owner,
                1,
                5,
                4,
                List.of()
        );

        // 14일보다 오래된 기록
        recordCondition(
                owner,
                20,
                5,
                4,
                List.of()
        );

        // 다른 회원의 기록은 집계에서 제외되어야 한다.
        recordCondition(
                otherUser,
                0,
                5,
                1,
                List.of(new PainRequest(BodyPart.KNEE, 5))
        );

        recordCondition(
                otherUser,
                1,
                5,
                1,
                List.of(new PainRequest(BodyPart.KNEE, 5))
        );

        flushAndClear();

        CoachingAssessment result =
                coachingService.assess(owner.getId());

        assertThat(result.level())
                .isEqualTo(CoachingLevel.INSUFFICIENT_DATA);

        assertThat(result.reasons())
                .isEmpty();
    }

    @Test
    @DisplayName("컨디션을 PUT으로 수정하면 코칭 캐시가 무효화되어 LLM을 다시 호출한다")
    void updateCondition_invalidatesCoachingCache() {

        // 실제 LLM 호출 대신 테스트 응답 반환
        given(llmClient.generate(any()))
                .willReturn(
                        new CoachingAdvice(
                                "좋은 페이스예요",
                                "컨디션이 안정적이에요",
                                List.of("지금처럼 유지"),
                                ""
                        )
                );

        // 최근 운동일 3개 생성
        recordCondition(
                owner,
                4,
                5,
                4,
                List.of()
        );

        recordCondition(
                owner,
                2,
                5,
                4,
                List.of()
        );

        Long todayWorkoutId = recordCondition(
                owner,
                0,
                5,
                4,
                List.of(new PainRequest(BodyPart.KNEE, 1))
        );

        flushAndClear();

        // 1. 최초 코칭 생성
        coachingService.coach(owner.getId());

        flushAndClear();

        // 2. 통증 강도를 1에서 5로 수정
        //    기존 컨디션 ID는 유지
        conditionService.upsert(
                todayWorkoutId,
                new WorkoutConditionRequest(
                        5,
                        4,
                        List.of(
                                new PainRequest(BodyPart.KNEE, 5)
                        ),
                        null
                ),
                owner.getId()
        );

        flushAndClear();

        // 3. 수정된 데이터를 기준으로 코칭 재생성
        coachingService.coach(owner.getId());

        // 최초 1회 + 수정 후 1회 = 총 2회
        verify(llmClient, times(2))
                .generate(any());
    }
}
