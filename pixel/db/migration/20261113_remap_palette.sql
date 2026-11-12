-- #135 지도 색 40개를 확정 팔레트로 교체 (운영 DB 전용, 한 번만 실행)
-- 서버를 멈춘 뒤 새 BE를 배포하기 전에 실행한다. 두 번 실행하면 다시 옮겨져 색이 틀어진다.
-- 옛 40색의 각 칸을 Lab 거리로 가장 가까운 새 색으로 옮긴다. 옛 무채색(36~39)은 새 무채색(34~39) 안에서만 고른다.
-- 새 순서: 0~33 유채색, 34~39 무채색(먹색 → 그림자 → 아스팔트 → 회색 건물 → 안개 → 구름). ColorBins.HEX와 같다.

START TRANSACTION;

-- 1) 색 분포: 새 칸 = 그 칸으로 옮겨 온 옛 칸의 합 (합계는 그대로)
UPDATE photos SET bins = JSON_ARRAY(
      0,
      CAST(JSON_EXTRACT(bins, '$[26]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[25]') AS DOUBLE),
      0,
      0,
      0,
      CAST(JSON_EXTRACT(bins, '$[5]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[8]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[24]') AS DOUBLE),
      0,
      0,
      CAST(JSON_EXTRACT(bins, '$[21]') AS DOUBLE),
      0,
      CAST(JSON_EXTRACT(bins, '$[15]') AS DOUBLE) + CAST(JSON_EXTRACT(bins, '$[18]') AS DOUBLE),
      0,
      CAST(JSON_EXTRACT(bins, '$[17]') AS DOUBLE) + CAST(JSON_EXTRACT(bins, '$[19]') AS DOUBLE) + CAST(JSON_EXTRACT(bins, '$[20]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[12]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[10]') AS DOUBLE) + CAST(JSON_EXTRACT(bins, '$[13]') AS DOUBLE) + CAST(JSON_EXTRACT(bins, '$[16]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[7]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[0]') AS DOUBLE) + CAST(JSON_EXTRACT(bins, '$[4]') AS DOUBLE),
      0,
      CAST(JSON_EXTRACT(bins, '$[2]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[32]') AS DOUBLE),
      0,
      0,
      CAST(JSON_EXTRACT(bins, '$[3]') AS DOUBLE) + CAST(JSON_EXTRACT(bins, '$[6]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[9]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[23]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[11]') AS DOUBLE) + CAST(JSON_EXTRACT(bins, '$[14]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[33]') AS DOUBLE) + CAST(JSON_EXTRACT(bins, '$[34]') AS DOUBLE) + CAST(JSON_EXTRACT(bins, '$[35]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[28]') AS DOUBLE) + CAST(JSON_EXTRACT(bins, '$[29]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[22]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[1]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[27]') AS DOUBLE) + CAST(JSON_EXTRACT(bins, '$[30]') AS DOUBLE) + CAST(JSON_EXTRACT(bins, '$[31]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[36]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins, '$[37]') AS DOUBLE),
      0,
      CAST(JSON_EXTRACT(bins, '$[38]') AS DOUBLE),
      0,
      CAST(JSON_EXTRACT(bins, '$[39]') AS DOUBLE));

UPDATE color_cells SET bins_sum = JSON_ARRAY(
      0,
      CAST(JSON_EXTRACT(bins_sum, '$[26]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[25]') AS DOUBLE),
      0,
      0,
      0,
      CAST(JSON_EXTRACT(bins_sum, '$[5]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[8]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[24]') AS DOUBLE),
      0,
      0,
      CAST(JSON_EXTRACT(bins_sum, '$[21]') AS DOUBLE),
      0,
      CAST(JSON_EXTRACT(bins_sum, '$[15]') AS DOUBLE) + CAST(JSON_EXTRACT(bins_sum, '$[18]') AS DOUBLE),
      0,
      CAST(JSON_EXTRACT(bins_sum, '$[17]') AS DOUBLE) + CAST(JSON_EXTRACT(bins_sum, '$[19]') AS DOUBLE) + CAST(JSON_EXTRACT(bins_sum, '$[20]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[12]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[10]') AS DOUBLE) + CAST(JSON_EXTRACT(bins_sum, '$[13]') AS DOUBLE) + CAST(JSON_EXTRACT(bins_sum, '$[16]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[7]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[0]') AS DOUBLE) + CAST(JSON_EXTRACT(bins_sum, '$[4]') AS DOUBLE),
      0,
      CAST(JSON_EXTRACT(bins_sum, '$[2]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[32]') AS DOUBLE),
      0,
      0,
      CAST(JSON_EXTRACT(bins_sum, '$[3]') AS DOUBLE) + CAST(JSON_EXTRACT(bins_sum, '$[6]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[9]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[23]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[11]') AS DOUBLE) + CAST(JSON_EXTRACT(bins_sum, '$[14]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[33]') AS DOUBLE) + CAST(JSON_EXTRACT(bins_sum, '$[34]') AS DOUBLE) + CAST(JSON_EXTRACT(bins_sum, '$[35]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[28]') AS DOUBLE) + CAST(JSON_EXTRACT(bins_sum, '$[29]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[22]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[1]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[27]') AS DOUBLE) + CAST(JSON_EXTRACT(bins_sum, '$[30]') AS DOUBLE) + CAST(JSON_EXTRACT(bins_sum, '$[31]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[36]') AS DOUBLE),
      CAST(JSON_EXTRACT(bins_sum, '$[37]') AS DOUBLE),
      0,
      CAST(JSON_EXTRACT(bins_sum, '$[38]') AS DOUBLE),
      0,
      CAST(JSON_EXTRACT(bins_sum, '$[39]') AS DOUBLE));

-- 새 40색 (인덱스, 헥스)
CREATE TEMPORARY TABLE palette_hex (idx INT PRIMARY KEY, hex CHAR(7) NOT NULL);
INSERT INTO palette_hex VALUES
  (0, '#D4E6F5'),
  (1, '#9CC8EC'),
  (2, '#5A8FD0'),
  (3, '#A9B6CC'),
  (4, '#B2A6D6'),
  (5, '#F2AEB8'),
  (6, '#F39A52'),
  (7, '#F7BD6E'),
  (8, '#5B5592'),
  (9, '#1E2850'),
  (10, '#3C8FB5'),
  (11, '#4FA7A3'),
  (12, '#2A5E78'),
  (13, '#5C9A55'),
  (14, '#36603D'),
  (15, '#A9CF72'),
  (16, '#C2AE72'),
  (17, '#EDD23A'),
  (18, '#E2783A'),
  (19, '#C9432F'),
  (20, '#F6C6D3'),
  (21, '#E27BA6'),
  (22, '#9B7BC8'),
  (23, '#EAE2D2'),
  (24, '#D9C7A6'),
  (25, '#A9573F'),
  (26, '#8A6245'),
  (27, '#A8DCCF'),
  (28, '#F8DD98'),
  (29, '#DA5A95'),
  (30, '#4F63D6'),
  (31, '#43BFC4'),
  (32, '#E8384F'),
  (33, '#7A3FA0'),
  (34, '#23262D'),
  (35, '#3E434E'),
  (36, '#6C7078'),
  (37, '#A2A5AA'),
  (38, '#C9CDD3'),
  (39, '#F0F2F4');

-- 2) 사진 대표색 5개: 새 bins에서 많은 순(같으면 작은 인덱스) 상위 5색, 5개가 안 되면 첫 색으로 채운다 (FE 분석과 같은 규칙)
UPDATE photos p
JOIN (
  SELECT photo_id,
         MAX(CASE WHEN rn = 1 THEN hex END) AS c1,
         MAX(CASE WHEN rn = 2 THEN hex END) AS c2,
         MAX(CASE WHEN rn = 3 THEN hex END) AS c3,
         MAX(CASE WHEN rn = 4 THEN hex END) AS c4,
         MAX(CASE WHEN rn = 5 THEN hex END) AS c5
  FROM (
    SELECT p2.photo_id, h.hex,
           ROW_NUMBER() OVER (PARTITION BY p2.photo_id ORDER BY jt.v DESC, jt.idx) AS rn
    FROM photos p2
    JOIN JSON_TABLE(p2.bins, '$[*]' COLUMNS (idx FOR ORDINALITY, v DOUBLE PATH '$')) jt
    JOIN palette_hex h ON h.idx = jt.idx - 1
    WHERE jt.v > 0
  ) ranked
  WHERE rn <= 5
  GROUP BY photo_id
) t ON t.photo_id = p.photo_id
SET p.palette = JSON_ARRAY(t.c1, COALESCE(t.c2, t.c1), COALESCE(t.c3, t.c1), COALESCE(t.c4, t.c1), COALESCE(t.c5, t.c1));

-- 3) 칸 대표색: 유채색(0~33) 비율이 15% 이상이면 유채색 중, 아니면 무채색(34~39) 중 가장 많은 색 (ColorBins.dominantColor와 같은 규칙)
UPDATE color_cells c
JOIN (
  SELECT cell_id, hex
  FROM (
    SELECT c2.cell_id, h.hex,
           ROW_NUMBER() OVER (PARTITION BY c2.cell_id ORDER BY jt.v DESC, jt.idx) AS rn
    FROM color_cells c2
    JOIN JSON_TABLE(c2.bins_sum, '$[*]' COLUMNS (idx FOR ORDINALITY, v DOUBLE PATH '$')) jt
    JOIN palette_hex h ON h.idx = jt.idx - 1
    JOIN (
      SELECT c3.cell_id,
             SUM(CASE WHEN jt3.idx <= 34 THEN jt3.v ELSE 0 END) / SUM(jt3.v) >= 0.15 AS chromatic
      FROM color_cells c3
      JOIN JSON_TABLE(c3.bins_sum, '$[*]' COLUMNS (idx FOR ORDINALITY, v DOUBLE PATH '$')) jt3
      WHERE c3.photo_count > 0
      GROUP BY c3.cell_id
      HAVING SUM(jt3.v) > 0
    ) g ON g.cell_id = c2.cell_id
    WHERE (g.chromatic AND jt.idx <= 34) OR (NOT g.chromatic AND jt.idx > 34)
  ) ranked
  WHERE rn = 1
) t ON t.cell_id = c.cell_id
SET c.dominant_color = t.hex;

DROP TEMPORARY TABLE palette_hex;

COMMIT;
