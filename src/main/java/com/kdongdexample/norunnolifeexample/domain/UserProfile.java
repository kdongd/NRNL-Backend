
package com.kdongdexample.norunnolifeexample.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Getter
@Entity
@Table(name = "user_profiles")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 인증 정보(User)와 운동 프로필(UserProfile)을 분리한다.
    // 일반 회원가입 시에는 두 데이터를 함께 생성한다.
    // 기존 OAuth 회원 등은 프로필이 없는 상태일 수 있다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Integer age;
    private Double heightCm;
    private Double weightKg;

    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    // AI 코칭에서 참고할 주요 운동 종목
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_profile_primary_exercises",
            joinColumns = @JoinColumn(name = "user_profile_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type")
    private Set<WorkoutType> primaryExercises =
            EnumSet.noneOf(WorkoutType.class);

    private String goal;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private UserProfile(
            User user,
            Gender gender,
            Integer age,
            Double heightCm,
            Double weightKg,
            ExperienceLevel experienceLevel,
            Set<WorkoutType> primaryExercises,
            String goal
    ) {
        this.user = user;
        this.gender = gender;
        this.age = age;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.experienceLevel = experienceLevel;
        this.primaryExercises = copyExercises(primaryExercises);
        this.goal = goal;
    }

    public static UserProfile create(
            User user,
            Gender gender,
            Integer age,
            Double heightCm,
            Double weightKg,
            ExperienceLevel experienceLevel,
            Set<WorkoutType> primaryExercises,
            String goal
    ) {
        return new UserProfile(
                user,
                gender,
                age,
                heightCm,
                weightKg,
                experienceLevel,
                primaryExercises,
                goal
        );
    }

    // [추가] 기존 운동 프로필 수정
    public void update(
            Gender gender,
            Integer age,
            Double heightCm,
            Double weightKg,
            ExperienceLevel experienceLevel,
            Set<WorkoutType> primaryExercises,
            String goal
    ) {
        this.gender = gender;
        this.age = age;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.experienceLevel = experienceLevel;

        // JPA가 관리하는 컬렉션을 교체하지 않고 내부 요소만 변경한다.
        this.primaryExercises.clear();
        this.primaryExercises.addAll(copyExercises(primaryExercises));

        this.goal = goal;
    }

    private static Set<WorkoutType> copyExercises(
            Set<WorkoutType> exercises
    ) {
        if (exercises == null || exercises.isEmpty()) {
            return EnumSet.noneOf(WorkoutType.class);
        }

        return EnumSet.copyOf(exercises);
    }
}
