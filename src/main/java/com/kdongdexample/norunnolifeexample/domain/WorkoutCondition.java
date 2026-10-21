
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id", nullable = false, unique = true)
    private Workout workout;

    // 운동 체감 강도 1~5
    @Column(nullable = false)
    private Integer rpe;

    // 운동 직후 몸 상태 1~5
    @Column(nullable = false)
    private Integer bodyCondition;

    // 통증 부위별 강도 1~5
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "workout_condition_pain",
            joinColumns = @JoinColumn(name = "workout_condition_id")
    )
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

    private WorkoutCondition(
            Workout workout,
            Integer rpe,
            Integer bodyCondition,
            Map<BodyPart, Integer> pains,
            String memo
    ) {
        this.workout = workout;
        this.rpe = rpe;
        this.bodyCondition = bodyCondition;
        this.pains.putAll(pains);
        this.memo = memo;
    }

    public static WorkoutCondition create(
            Workout workout,
            Integer rpe,
            Integer bodyCondition,
            Map<BodyPart, Integer> pains,
            String memo
    ) {
        return new WorkoutCondition(
                workout, rpe, bodyCondition, pains, memo
        );
    }

    public void update(
            Integer rpe,
            Integer bodyCondition,
            Map<BodyPart, Integer> pains,
            String memo
    ) {
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
