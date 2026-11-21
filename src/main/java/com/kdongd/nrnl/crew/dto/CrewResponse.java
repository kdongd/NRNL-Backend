package com.kdongd.nrnl.crew.dto;

import com.kdongd.nrnl.crew.domain.Crew;
import com.kdongd.nrnl.crew.domain.CrewStatus;
import com.kdongd.nrnl.domain.WorkoutType;

import java.time.Instant;
import java.util.Set;

public record CrewResponse(
        Long id,
        String name,
        String description,
        Set<WorkoutType> sports,
        int capacity,
        CrewStatus status,
        long memberCount,
        Instant createdAt
) {
    public static CrewResponse from(Crew crew, long memberCount) {
        return new CrewResponse(crew.getId(), crew.getName(), crew.getDescription(),
                Set.copyOf(crew.getSports()), crew.getCapacity(), crew.getStatus(),
                memberCount, crew.getCreatedAt());
    }
}
