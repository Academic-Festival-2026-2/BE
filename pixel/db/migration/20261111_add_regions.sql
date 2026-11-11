-- #119 지역(regions) 추가 (운영 DB 전용, 서버 재빌드 전에 실행)
-- 지금 동은 전부 부천이라 region_code는 동 코드 앞 4자리로 채운다.

-- ① 테이블 생성
CREATE TABLE regions (
    region_code  CHAR(4)      NOT NULL COMMENT '동 코드 앞 4자리 (부천 3105)',
    sido_name    VARCHAR(10)  NOT NULL,
    name         VARCHAR(10)  NOT NULL COMMENT '시/군 이름 (부천시)',
    PRIMARY KEY (region_code),
    CONSTRAINT ck_regions_code CHECK (REGEXP_LIKE(region_code, '^[0-9]{4}$'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ② 부천 1행
INSERT INTO regions (region_code, sido_name, name) VALUES ('3105', '경기도', '부천시');

-- ③ 컬럼 추가: 아직 값이 없으니 NULL 허용으로
ALTER TABLE dongs
    ADD COLUMN region_code CHAR(4) NULL COMMENT '지역 코드 (동 코드 앞 4자리)'
        AFTER dong_code;

-- ④ 값 채우기
UPDATE dongs SET region_code = LEFT(dong_code, 4);

-- ⑤ 이제 다 채워졌으니 NOT NULL + FK, 그리고 구 제한 풀기
ALTER TABLE dongs
    MODIFY COLUMN region_code CHAR(4) NOT NULL COMMENT '지역 코드 (동 코드 앞 4자리)',
    ADD CONSTRAINT fk_dongs_region FOREIGN KEY (region_code) REFERENCES regions (region_code),
    DROP CHECK ck_dongs_gu,
    MODIFY COLUMN gu_name VARCHAR(10) NULL;
