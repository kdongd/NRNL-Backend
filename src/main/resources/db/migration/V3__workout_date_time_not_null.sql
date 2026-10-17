-- 운동 일시는 룰 엔진(DailyCondition.aggregate), 코칭, 통계에서 null이 아니라고 가정하고 쓰는 값이라 DB에서도 not null로 강제.
-- API(WorkoutForm.workoutDateTime @NotNull)로는 null이 들어올 수 없지만 V1에서 nullable로 만들어져 있었다.
alter table workout alter column workout_date_time set not null;
