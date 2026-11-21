package com.kdongd.nrnl.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@DiscriminatorValue("RUNNING")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RunningWorkout extends Workout {

    private Double distanceKm;
    private String place;
    private Integer caloriesBurned;

    private RunningWorkout(User owner, Integer durationMinutes, String memo, LocalDateTime workoutDateTime,
                           Double distanceKm, String place, Integer caloriesBurned) {
        super(owner, WorkoutType.RUNNING, durationMinutes, memo, workoutDateTime);
        this.distanceKm = distanceKm;
        this.place = place;
        this.caloriesBurned = caloriesBurned;
    }

    public static RunningWorkout create(User owner, Integer durationMinutes, String memo, LocalDateTime workoutDateTime,
                                        Double distanceKm, String place, Integer caloriesBurned) {
        return new RunningWorkout(owner, durationMinutes, memo, workoutDateTime, distanceKm, place, caloriesBurned);
    }

    public void update(Integer durationMinutes, String memo, LocalDateTime workoutDateTime,
                       Double distanceKm, String place, Integer caloriesBurned) {
        updateCommon(durationMinutes, memo, workoutDateTime);
        this.distanceKm = distanceKm;
        this.place = place;
        this.caloriesBurned = caloriesBurned;
    }
}
