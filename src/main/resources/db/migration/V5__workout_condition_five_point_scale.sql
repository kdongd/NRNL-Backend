
-- 운동 체감 강도(RPE)와 운동 직후 몸 상태를
-- 기존 1~10점 척도에서 1~5점 척도로 변경한다.
--
-- 기존 데이터 변환:
-- 1~2  -> 1
-- 3~4  -> 2
-- 5~6  -> 3
-- 7~8  -> 4
-- 9~10 -> 5
--
-- 적용 전 데이터 백업 필요.


-- 기존 10점 척도 CHECK 제약 제거
ALTER TABLE workout_condition
    DROP CONSTRAINT IF EXISTS workout_condition_rpe_check;

ALTER TABLE workout_condition
    DROP CONSTRAINT IF EXISTS workout_condition_body_condition_check;

-- 기존 저장 데이터를 5점 척도로 변환
UPDATE workout_condition
SET
    rpe = CEIL(rpe / 2.0)::integer,
    body_condition = CEIL(body_condition / 2.0)::integer;

-- 새 5점 척도 CHECK 제약 추가
ALTER TABLE workout_condition
    ADD CONSTRAINT chk_workout_condition_rpe_1_5
    CHECK (rpe BETWEEN 1 AND 5);

ALTER TABLE workout_condition
    ADD CONSTRAINT chk_workout_condition_body_condition_1_5
    CHECK (body_condition BETWEEN 1 AND 5);
