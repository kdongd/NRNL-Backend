package com.kdongdexample.norunnolifeexample.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 하루에 운동 기록(컨디션)이 여러 개일 수 있어서 날짜 단위로 하나로 합친 값.
// 위험 신호를 놓치지 않도록 그날 가장 나빴던 값을 대표값으로 씁니다.
// 통증: 부위별 최댓값
// RPE: 최댓값
// 몸 상태: 최솟값
public record DailyCondition(
        LocalDate date,
        Map<BodyPart, Integer> maxPainByPart,
        int maxRpe,
        int minBodyCondition
) {
    public DailyCondition {
        maxPainByPart = Collections.unmodifiableMap(new EnumMap<>(
                maxPainByPart.isEmpty() ? new EnumMap<>(BodyPart.class) : maxPainByPart));
    }

    public boolean hasPain() {
        return !maxPainByPart.isEmpty();
    }

    public int maxPain() {
        return maxPainByPart.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    // 최신 날짜가 앞에 오도록 정렬해서 반환
    public static List<DailyCondition> aggregate(List<WorkoutCondition> conditions) {
        Map<LocalDate, List<WorkoutCondition>> byDate = new LinkedHashMap<>();
        for (WorkoutCondition condition : conditions) {
            LocalDate date = condition.getWorkout().getWorkoutDateTime().toLocalDate();
            byDate.computeIfAbsent(date, d -> new ArrayList<>()).add(condition);
        }

        List<DailyCondition> result = new ArrayList<>();
        byDate.forEach((date, sameDay) -> result.add(merge(date, sameDay)));
        result.sort(Comparator.comparing(DailyCondition::date).reversed());
        return result;
    }

    private static DailyCondition merge(LocalDate date, List<WorkoutCondition> sameDay) {
        Map<BodyPart, Integer> pains = new EnumMap<>(BodyPart.class);
        int maxRpe = Integer.MIN_VALUE;
        int minBodyCondition = Integer.MAX_VALUE;

        for (WorkoutCondition condition : sameDay) {
            condition.getPains().forEach((part, intensity) -> pains.merge(part, intensity, Math::max));
            maxRpe = Math.max(maxRpe, condition.getRpe());
            minBodyCondition = Math.min(minBodyCondition, condition.getBodyCondition());
        }
        return new DailyCondition(date, pains, maxRpe, minBodyCondition);
    }
}
