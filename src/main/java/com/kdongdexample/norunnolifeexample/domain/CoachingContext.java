package com.kdongdexample.norunnolifeexample.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

// LLM 프롬프트에 들어갈 데이터 모음.
// 이메일 정보는 담지 않았습니다.
public record CoachingContext(
        CoachingAssessment assessment,
        ProfileSnapshot profile,
        List<DailyCondition> dailyConditions,
        List<WorkoutSnapshot> recentWorkouts,
        List<MemoSnapshot> memos
) {
    public record ProfileSnapshot(
            Gender gender,
            Integer age,
            Double heightCm,
            Double weightKg,
            ExperienceLevel experienceLevel,
            Set<WorkoutType> primaryExercises,
            String goal
    ) {
        public static ProfileSnapshot from(UserProfile profile) {
            return new ProfileSnapshot(
                    profile.getGender(),
                    profile.getAge(),
                    profile.getHeightCm(),
                    profile.getWeightKg(),
                    profile.getExperienceLevel(),
                    Set.copyOf(profile.getPrimaryExercises()),
                    profile.getGoal()
            );
        }
    }

    public record WorkoutSnapshot(
            LocalDate date,
            WorkoutType type,
            Integer durationMinutes,
            Double distanceKm,
            Integer rounds,
            TechniqueType techniqueType
    ) {
        public static WorkoutSnapshot from(Workout workout) {
            Double distanceKm = null;
            Integer rounds = null;
            TechniqueType techniqueType = null;
            if (workout instanceof RunningWorkout running) {
                distanceKm = running.getDistanceKm();
            } else if (workout instanceof BoxingWorkout boxing) {
                rounds = boxing.getRounds();
                techniqueType = boxing.getTechniqueType();
            }
            return new WorkoutSnapshot(
                    workout.getWorkoutDateTime().toLocalDate(),
                    workout.getType(),
                    workout.getDurationMinutes(),
                    distanceKm,
                    rounds,
                    techniqueType
            );
        }
    }

    public record MemoSnapshot(
            LocalDate date,
            String text
    ) {}
}
