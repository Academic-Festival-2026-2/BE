-- 고정 사계절 + 지역·계절·시간대별 빈 지도 셀 (부천 37개 동 기준 592칸)
-- 02_dongs.sql로 행정동을 넣은 다음 실행한다. 날짜/연도는 사진의 taken_at·shot_date에 남긴다.
SET NAMES utf8mb4;

INSERT INTO seasons (season)
VALUES ('SPRING'), ('SUMMER'), ('AUTUMN'), ('WINTER');

-- 빈 칸: 사진 0장, 색 분포 40칸 모두 0, 대표색 없음
INSERT INTO color_cells (dong_code, season_id, time_band, bins_sum)
SELECT d.dong_code, s.season_id, t.band, CAST(CONCAT('[', REPEAT('0,', 39), '0]') AS JSON)
FROM dongs d
CROSS JOIN seasons s
CROSS JOIN (SELECT 'DAWN' AS band UNION ALL SELECT 'DAY' UNION ALL SELECT 'SUNSET' UNION ALL SELECT 'NIGHT') AS t;
