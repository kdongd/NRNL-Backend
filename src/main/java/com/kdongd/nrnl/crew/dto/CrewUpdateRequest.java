package com.kdongd.nrnl.crew.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.kdongd.nrnl.crew.domain.CrewStatus;
import com.kdongd.nrnl.domain.WorkoutType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.CodePointLength;

import java.util.Set;

public record CrewUpdateRequest(
        @NotBlank @Pattern(regexp = "[가-힣A-Za-z0-9]{2,20}") String name,
        @NotBlank @CodePointLength(min = 10, max = 500) String description,
        @NotEmpty Set<@NotNull WorkoutType> sports,
        @NotNull @JsonDeserialize(using = CrewCapacityDeserializer.class) Integer capacity,
        @NotNull CrewStatus status
) {
}
