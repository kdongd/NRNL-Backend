package com.kdongd.nrnl.repository;

import com.kdongd.nrnl.domain.Workout;

import java.util.Optional;

public interface WorkoutRepository {
    Workout save(Workout workout);
    Optional<Workout> findById(Long id);
    void delete(Workout workout);
}
