package com.kdongdexample.norunnolifeexample.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@Getter
@Entity
@Table(name = "workout_condition")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 운동 기록 1건당 컨디션은 최대 1개. 운동 기록을 먼저 저장하고, 운동이 끝난 뒤 선택적으로 입력하는 값이라서
    // Workout 테이블에 컬럼을 추가하지 않고 테이블로 분리했습니다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id", nullable = false, unique = true)
    private Workout workout;

    // 운동 강도 체감(RPE). 1 = 매우 쉬움, 10 = 최대로 힘듦
    @Column(nullable = false)
    private Integer rpe;

    // 운동을 마친 직후의 몸 상태. 1 = 매우 나쁨, 10 = 매우 좋음
    @Column(nullable = false)
    private Integer bodyCondition;

    // 통증 부위별 강도(1~5). 비어 있으면 통증 없음.
    // 같은 부위가 두 번 들어갈 수 없도록 부위를 key로 하는 Map으로 매핑했습니다.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "workout_condition_pain", joinColumns = @JoinColumn(name = "workout_condition_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "body_part")
    @Column(name = "intensity", nullable = false)
    private Map<BodyPart, Integer> pains = new EnumMap<>(BodyPart.class);

    private String memo;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private WorkoutCondition(Workout workout, Integer rpe, Integer bodyCondition,
                             Map<BodyPart, Integer> pains, String memo) {
        this.workout = workout;
        this.rpe = rpe;
        this.bodyCondition = bodyCondition;
        this.pains.putAll(pains);
        this.memo = memo;
    }

    public static WorkoutCondition create(Workout workout, Integer rpe, Integer bodyCondition,
                                          Map<BodyPart, Integer> pains, String memo) {
        return new WorkoutCondition(workout, rpe, bodyCondition, pains, memo);
    }

    public void update(Integer rpe, Integer bodyCondition, Map<BodyPart, Integer> pains, String memo) {
        this.rpe = rpe;
        this.bodyCondition = bodyCondition;
        this.pains.clear();
        this.pains.putAll(pains);
        this.memo = memo;
    }

    public boolean hasPain() {
        return !pains.isEmpty();
    }

    public Map<BodyPart, Integer> getPains() {
        return Collections.unmodifiableMap(pains);
    }
}
