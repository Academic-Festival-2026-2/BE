-- #146 촬영 시각 없는 사진을 위한 DB 변경
-- 운영 DB 적용 전 백업 후 실행한다.
-- 기존 사진은 삭제하거나 수정하지 않는다.

-- 촬영 날짜를 모르는 사진 저장 허용
ALTER TABLE photos
    MODIFY COLUMN shot_date DATE NULL
    COMMENT '같은 사용자·같은 날 묶음 기준';

-- 기존 가을 데이터는 유지하고 누락된 시즌만 추가한다.
INSERT INTO seasons (`year`, season, starts_on, ends_on)
SELECT 2026, 'SPRING', '2026-03-01', '2026-05-31'
WHERE NOT EXISTS (
    SELECT 1 FROM seasons
    WHERE `year` = 2026 AND season = 'SPRING'
);

INSERT INTO seasons (`year`, season, starts_on, ends_on)
SELECT 2026, 'SUMMER', '2026-06-01', '2026-08-31'
WHERE NOT EXISTS (
    SELECT 1 FROM seasons
    WHERE `year` = 2026 AND season = 'SUMMER'
);

INSERT INTO seasons (`year`, season, starts_on, ends_on)
SELECT 2026, 'WINTER', '2026-12-01', '2027-02-28'
WHERE NOT EXISTS (
    SELECT 1 FROM seasons
    WHERE `year` = 2026 AND season = 'WINTER'
);
