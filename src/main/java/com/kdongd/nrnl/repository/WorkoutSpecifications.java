package com.kdongd.nrnl.repository;

import com.kdongd.nrnl.domain.Workout;
import com.kdongd.nrnl.domain.WorkoutType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class WorkoutSpecifications {

    private WorkoutSpecifications() {}

    public static Specification<Workout> hasOwner(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<Workout> hasType(WorkoutType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Workout> fromDate(LocalDateTime from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("workoutDateTime"), from);
    }

    public static Specification<Workout> toDate(LocalDateTime to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("workoutDateTime"), to);
    }
}
