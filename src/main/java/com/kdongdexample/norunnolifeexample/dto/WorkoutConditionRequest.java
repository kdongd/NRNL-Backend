package com.kdongdexample.norunnolifeexample.dto;

import com.kdongdexample.norunnolifeexample.domain.BodyPart;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record WorkoutConditionRequest(
        @Schema(description = "운동 강도 체감(RPE). 1 = 매우 쉬움, 10 = 최대로 힘듦", example = "7")
        @NotNull @Min(1) @Max(10)
        Integer rpe,

        @Schema(description = "운동 직후 몸 상태. 1 = 매우 나쁨, 10 = 매우 좋음", example = "6")
        @NotNull @Min(1) @Max(10)
        Integer bodyCondition,

        @Schema(description = "통증 부위별 강도 목록. 통증이 없으면 비우거나 생략")
        List<@NotNull @Valid PainRequest> pains,

        @Schema(description = "메모", example = "운동 중간에 무릎 통증이 생겼다.")
        @Size(max = 255)
        String memo
) {
    public record PainRequest(
            @Schema(description = "통증 부위", example = "KNEE")
            @NotNull
            BodyPart bodyPart,

            @Schema(description = "통증 강도. 1 = 약간 불편, 5 = 극심한 통증", example = "3")
            @NotNull @Min(1) @Max(5)
            Integer intensity
    ) {}

    @AssertTrue(message = "같은 통증 부위를 중복으로 보낼 수 없습니다")
    public boolean isNoDuplicateBodyPart() {
        if (pains == null) {
            return true;
        }
        List<BodyPart> parts = pains.stream()
                .filter(p -> p != null && p.bodyPart() != null)
                .map(PainRequest::bodyPart)
                .toList();
        return parts.size() == parts.stream().distinct().count();
    }

    public Map<BodyPart, Integer> toPainMap() {
        Map<BodyPart, Integer> map = new EnumMap<>(BodyPart.class);
        if (pains != null) {
            pains.forEach(p -> map.put(p.bodyPart(), p.intensity()));
        }
        return map;
    }
}
