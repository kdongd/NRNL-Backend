package com.kdongd.nrnl.crew.dto;

import com.kdongd.nrnl.crew.domain.CrewApplication;
import com.kdongd.nrnl.crew.domain.CrewApplicationStatus;

import java.time.Instant;

public record CrewApplicationResponse(
        Long id,
        Long crewId,
        Long userId,
        CrewApplicationStatus status,
        Instant createdAt,
        Instant resolvedAt
) {
    public static CrewApplicationResponse from(CrewApplication application) {
        return new CrewApplicationResponse(application.getId(), application.getCrewId(),
                application.getUser().getId(), application.getStatus(),
                application.getCreatedAt(), application.getResolvedAt());
    }
}
