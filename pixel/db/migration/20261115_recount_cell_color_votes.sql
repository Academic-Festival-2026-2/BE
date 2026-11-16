-- #151 지도 칸 색을 사진 대표색(palette[0]) 표 수로 다시 계산 (로컬·운영 DB)
-- 컬럼은 바뀌지 않아서 서버가 이 SQL 없이도 뜬다. 새 BE를 배포한 뒤 실행하면 기존 칸 색이 새 규칙으로 맞춰진다.
-- 다시 실행해도 안전하다: 사진 표 수와 칸 분포(bins_sum)에서 매번 같은 결과를 계산한다.
-- 규칙(ColorBins.votedColor와 같음): 칸에 들어간 사진(exif_verified AND ai_verdict = 'PUBLIC') 한 장 = palette[0] 한 표(가중치 weight).
--   표가 가장 많은 색 → 동점이면 bins_sum 값이 큰 색 → 그래도 같으면 작은 인덱스.
--   팔레트에 없는 색만 있는 칸은 그대로 둔다(기존 15% 규칙 색 유지, 서버의 대체 규칙과 같음).

START TRANSACTION;

-- 40색 (인덱스, 헥스). ColorBins.HEX와 같은 순서
CREATE TEMPORARY TABLE palette_hex (idx INT PRIMARY KEY, hex CHAR(7) NOT NULL UNIQUE);
INSERT INTO palette_hex VALUES
  (0, '#D4E6F5'), (1, '#9CC8EC'), (2, '#5A8FD0'), (3, '#A9B6CC'), (4, '#B2A6D6'),
  (5, '#F2AEB8'), (6, '#F39A52'), (7, '#F7BD6E'), (8, '#5B5592'), (9, '#1E2850'),
  (10, '#3C8FB5'), (11, '#4FA7A3'), (12, '#2A5E78'), (13, '#5C9A55'), (14, '#36603D'),
  (15, '#A9CF72'), (16, '#C2AE72'), (17, '#EDD23A'), (18, '#E2783A'), (19, '#C9432F'),
  (20, '#F6C6D3'), (21, '#E27BA6'), (22, '#9B7BC8'), (23, '#EAE2D2'), (24, '#D9C7A6'),
  (25, '#A9573F'), (26, '#8A6245'), (27, '#A8DCCF'), (28, '#F8DD98'), (29, '#DA5A95'),
  (30, '#4F63D6'), (31, '#43BFC4'), (32, '#E8384F'), (33, '#7A3FA0'), (34, '#23262D'),
  (35, '#3E434E'), (36, '#6C7078'), (37, '#A2A5AA'), (38, '#C9CDD3'), (39, '#F0F2F4');

UPDATE color_cells c
JOIN (
  SELECT cell_id, hex
  FROM (
    SELECT c2.cell_id, h.hex,
           ROW_NUMBER() OVER (
             PARTITION BY c2.cell_id
             ORDER BY v.votes DESC,
                      CAST(JSON_EXTRACT(c2.bins_sum, CONCAT('$[', h.idx, ']')) AS DOUBLE) DESC,
                      h.idx
           ) AS rn
    FROM color_cells c2
    JOIN (
      SELECT p.dong_code, p.season_id, p.time_band,
             JSON_UNQUOTE(JSON_EXTRACT(p.palette, '$[0]')) AS hex,
             SUM(p.weight) AS votes
      FROM photos p
      WHERE p.exif_verified AND p.ai_verdict = 'PUBLIC'
      GROUP BY p.dong_code, p.season_id, p.time_band, JSON_UNQUOTE(JSON_EXTRACT(p.palette, '$[0]'))
    ) v ON v.dong_code = c2.dong_code AND v.season_id = c2.season_id AND v.time_band = c2.time_band
    JOIN palette_hex h ON h.hex = v.hex
    WHERE c2.photo_count > 0 AND v.votes > 0
  ) ranked
  WHERE rn = 1
) t ON t.cell_id = c.cell_id
SET c.dominant_color = t.hex;

DROP TEMPORARY TABLE palette_hex;

COMMIT;

-- 확인: 사진이 있는 칸의 색
-- SELECT dong_code, time_band, photo_count, dominant_color FROM color_cells WHERE photo_count > 0;
