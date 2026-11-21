package com.kdongd.nrnl.repository;

import com.kdongd.nrnl.domain.Workout;
import com.kdongd.nrnl.domain.WorkoutType;
import com.kdongd.nrnl.dto.WorkoutMonthlyStat;
import com.kdongd.nrnl.dto.WorkoutStatByType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkoutQueryRepository {
    Page<Workout> search(Long ownerId, WorkoutType type, LocalDateTime from, LocalDateTime to, Pageable pageable);
    List<WorkoutStatByType> statsByType(Long ownerId, LocalDateTime from, LocalDateTime to);
    List<WorkoutMonthlyStat> statsByMonth(Long ownerId, LocalDateTime from, LocalDateTime to);
}
