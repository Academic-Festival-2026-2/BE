-- 3) 시드: 2026 가을 판 + 모두의 지도 148칸 (37개 동 × 4구간)
-- 02_dongs.sql로 dongs 37행을 넣은 뒤 실행한다.
SET NAMES utf8mb4;

INSERT INTO seasons (`year`, season, starts_on, ends_on)
VALUES (2026, 'AUTUMN', '2026-09-01', '2026-11-30');

-- 빈 칸: 사진 0장, 색 분포 40칸 모두 0, 대표색 없음
INSERT INTO color_cells (dong_code, season_id, time_band, bins_sum)
SELECT d.dong_code, s.season_id, t.band, CAST(CONCAT('[', REPEAT('0,', 39), '0]') AS JSON)
FROM dongs d
CROSS JOIN seasons s
CROSS JOIN (SELECT 'DAWN' AS band UNION ALL SELECT 'DAY' UNION ALL SELECT 'SUNSET' UNION ALL SELECT 'NIGHT') AS t
WHERE s.`year` = 2026 AND s.season = 'AUTUMN';
