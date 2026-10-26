
package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.CoachingAssessment;
import com.kdongdexample.norunnolifeexample.domain.CoachingLevel;
import com.kdongdexample.norunnolifeexample.domain.CoachingReason;
import com.kdongdexample.norunnolifeexample.domain.CoachingRule;
import com.kdongdexample.norunnolifeexample.domain.DailyCondition;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CoachingRuleEngine {

    static final int WINDOW_DAYS = 14;
    static final int MIN_DAYS = 3;

    static final int SEVERE_PAIN = 5;
    static final int MODERATE_PAIN = 4;
    static final int PAIN_RECENT_DAYS = 3;

    static final int PAIN_STREAK = 3;
    static final int PERSISTENT_PAIN_MIN_INTENSITY = 2;

    static final int RPE_LOOKBACK = 5;

    // 5점 척도에서 가장 높은 강도
    static final int HIGH_RPE = 5;
    static final int HIGH_RPE_COUNT = 3;

    // 5점 척도에서 나쁨 이하
    static final int LOW_BODY_CONDITION = 2;
    static final int LOW_BODY_STREAK = 3;

    public LocalDate windowStart(LocalDate today) {
        return today.minusDays(WINDOW_DAYS - 1);
    }

    public CoachingAssessment evaluate(
            List<DailyCondition> dailyConditions,
            LocalDate today
    ) {
        LocalDate from = windowStart(today);

        List<DailyCondition> days = dailyConditions.stream()
                .filter(d ->
                        !d.date().isBefore(from)
                                && !d.date().isAfter(today)
                )
                .sorted(
                        Comparator.comparing(
                                DailyCondition::date
                        ).reversed()
                )
                .toList();

        List<CoachingReason> reasons = new ArrayList<>();

        // 최근 심한 통증은 기록 부족과 관계없이 판정
        if (isLatestRecent(days, today)
                && days.get(0).maxPain() >= SEVERE_PAIN) {

            reasons.add(new CoachingReason(
                    CoachingRule.SEVERE_PAIN,
                    painDetail(days.get(0))
            ));
        }

        if (days.size() < MIN_DAYS) {
            CoachingLevel level = reasons.isEmpty()
                    ? CoachingLevel.INSUFFICIENT_DATA
                    : highestLevel(reasons);

            return new CoachingAssessment(
                    level,
                    List.copyOf(reasons),
                    from,
                    today
            );
        }

        // 최근 통증 강도 4
        if (isLatestRecent(days, today)
                && days.get(0).maxPain() == MODERATE_PAIN) {

            reasons.add(new CoachingReason(
                    CoachingRule.MODERATE_PAIN,
                    painDetail(days.get(0))
            ));
        }

        // 최근 기록일 3번 모두 통증 강도 2 이상
        List<DailyCondition> painWindow =
                days.subList(0, PAIN_STREAK);

        if (painWindow.stream().allMatch(
                d -> d.maxPain() >= PERSISTENT_PAIN_MIN_INTENSITY
        )) {
            reasons.add(new CoachingReason(
                    CoachingRule.PAIN_PERSISTENT,
                    "최근 입력일 " + PAIN_STREAK
                            + "번 모두 통증 강도 "
                            + PERSISTENT_PAIN_MIN_INTENSITY
                            + " 이상 기록 ("
                            + datesOf(painWindow) + ")"
            ));
        }

        // 최근 기록일 최대 5번 중 RPE 5가 3번 이상
        List<DailyCondition> rpeWindow =
                days.subList(0, Math.min(RPE_LOOKBACK, days.size()));

        long highRpeDays = rpeWindow.stream()
                .filter(d -> d.maxRpe() >= HIGH_RPE)
                .count();

        if (highRpeDays >= HIGH_RPE_COUNT) {
            reasons.add(new CoachingReason(
                    CoachingRule.HIGH_RPE_REPEATED,
                    "최근 입력일 " + rpeWindow.size()
                            + "번 중 " + highRpeDays
                            + "번 RPE " + HIGH_RPE + " 이상"
            ));
        }

        // 최근 기록일 3번 모두 몸 상태 2 이하
        List<DailyCondition> bodyWindow =
                days.subList(0, LOW_BODY_STREAK);

        if (bodyWindow.stream().allMatch(
                d -> d.minBodyCondition() <= LOW_BODY_CONDITION
        )) {
            reasons.add(new CoachingReason(
                    CoachingRule.LOW_BODY_CONDITION,
                    "최근 입력일 " + LOW_BODY_STREAK
                            + "번 모두 몸 상태 "
                            + LOW_BODY_CONDITION
                            + " 이하 ("
                            + datesOf(bodyWindow) + ")"
            ));
        }

        CoachingLevel level = reasons.isEmpty()
                ? CoachingLevel.NORMAL
                : highestLevel(reasons);

        return new CoachingAssessment(
                level,
                List.copyOf(reasons),
                from,
                today
        );
    }

    private boolean isLatestRecent(
            List<DailyCondition> days,
            LocalDate today
    ) {
        return !days.isEmpty()
                && !days.get(0).date().isBefore(
                today.minusDays(PAIN_RECENT_DAYS - 1)
        );
    }

    private CoachingLevel highestLevel(
            List<CoachingReason> reasons
    ) {
        return reasons.stream()
                .map(r -> r.rule().level())
                .max(Comparator.comparingInt(CoachingLevel::severity))
                .orElse(CoachingLevel.NORMAL);
    }

    private String painDetail(DailyCondition day) {
        int max = day.maxPain();

        String parts = day.maxPainByPart()
                .entrySet()
                .stream()
                .filter(e -> e.getValue() == max)
                .map(e -> e.getKey().name())
                .sorted()
                .collect(Collectors.joining(", "));

        return day.date()
                + " 통증 강도 " + max
                + " (" + parts + ")";
    }

    private String datesOf(List<DailyCondition> days) {
        return days.stream()
                .map(d -> d.date().toString())
                .collect(Collectors.joining(", "));
    }
}
