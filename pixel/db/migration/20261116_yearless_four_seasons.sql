-- 연도 독립형 사계절 변경. 적용 전 DB 전체 백업 필수.
-- 운영 순서: #147(20261115_add_photo_seasons.sql) 선행 적용
--           -> 새 BE 코드 배포 -> 이 마이그레이션 실행.
-- 반드시 터미널에서 mysql < 파일 형태로 실행한다. --force 사용 금지.
-- IntelliJ SQL 콘솔 등 오류 후 다음 SQL을 계속 실행하는 방식 사용 금지.
-- DROP COLUMN이 포함되므로 적용 전 백업하고 재실행하지 않는다.
-- 정확히 네 개의 계절 행이 한 개씩 존재해야만 진행한다.
-- 이미 여러 연도의 같은 계절 행이 있으면 자동 병합하지 않고 중단:
-- photos/ color_cells를 안전하게 재집계하는 별도 마이그레이션이 필요하다.
-- season_id 기본키는 그대로 유지하므로 기존 사진·집계·외래키를 변경하지 않는다.

CREATE TEMPORARY TABLE season_migration_guard (
  row_count INT NOT NULL,
  distinct_count INT NOT NULL,
  CONSTRAINT ck_season_migration_guard CHECK (row_count = 4 AND distinct_count = 4)
);
INSERT INTO season_migration_guard
SELECT COUNT(*), COUNT(DISTINCT season) FROM seasons;
DROP TEMPORARY TABLE season_migration_guard;

ALTER TABLE seasons
    DROP CHECK ck_seasons_range,
    DROP INDEX uk_seasons_year_season,
    DROP COLUMN `year`,
    DROP COLUMN starts_on,
    DROP COLUMN ends_on,
    ADD UNIQUE KEY uk_seasons_season (season);

-- 각 지역의 네 계절×4시간대 빈 칸 추가. 이미 색이 집계된 칸은 건드리지 않는다.
INSERT IGNORE INTO color_cells (dong_code, season_id, time_band, bins_sum)
SELECT d.dong_code, s.season_id, t.band, CAST(CONCAT('[', REPEAT('0,', 39), '0]') AS JSON)
FROM dongs d CROSS JOIN seasons s
CROSS JOIN (SELECT 'DAWN' AS band UNION ALL SELECT 'DAY' UNION ALL SELECT 'SUNSET' UNION ALL SELECT 'NIGHT') AS t;
