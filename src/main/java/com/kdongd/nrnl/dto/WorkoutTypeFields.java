package com.kdongd.nrnl.dto;

import com.kdongd.nrnl.domain.BoxingWorkout;
import com.kdongd.nrnl.domain.RunningWorkout;
import com.kdongd.nrnl.domain.TechniqueType;
import com.kdongd.nrnl.domain.Workout;

public record WorkoutTypeFields(
        Double distanceKm,
        String place,
        Integer caloriesBurned,
        Integer rounds,
        String sparringPartner,
        TechniqueType techniqueType
) {
    public static WorkoutTypeFields from(Workout workout) {
        if (workout instanceof RunningWorkout running) {
            return new WorkoutTypeFields(
                    running.getDistanceKm(), running.getPlace(), running.getCaloriesBurned(),
                    null, null, null);
        } else if (workout instanceof BoxingWorkout boxing) {
            return new WorkoutTypeFields(
                    null, null, null,
                    boxing.getRounds(), boxing.getSparringPartner(), boxing.getTechniqueType());
        }
        return new WorkoutTypeFields(null, null, null, null, null, null);
    }
}
