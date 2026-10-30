-- 1) 픽셀 테이블 전체 (MySQL 8.0.17 이상)
-- 기준: ERD v0.3. users·refresh_token·dongs는 현재 엔티티 코드에 맞췄다(ddl-auto: validate).
-- 색 분포는 40칸 (유채색 색상 12 × 명도 3 = 36칸 + 무채색 4칸)
-- 시간은 KST(Asia/Seoul)로 저장한다.
-- 실행 순서: 01_schema.sql → 02_dongs.sql → 03_seed.sql
SET NAMES utf8mb4;

CREATE TABLE users (
    user_id             BIGINT       NOT NULL AUTO_INCREMENT,
    email               VARCHAR(255) NOT NULL,
    password_hash       VARCHAR(100) NOT NULL COMMENT 'BCrypt 해시',
    nickname            VARCHAR(16)  NOT NULL,
    status              VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    terms_agreed_at     DATETIME(6)  NOT NULL,
    privacy_agreed_at   DATETIME(6)  NOT NULL,
    location_agreed_at  DATETIME(6)  NULL,
    created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)  NULL,
    deleted_at          DATETIME(6)  NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_nickname (nickname),
    CONSTRAINT ck_users_nickname_len CHECK (CHAR_LENGTH(nickname) BETWEEN 2 AND 16),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 사용자당 Refresh Token 1개 (로그인하면 덮어쓰고, 재발급할 때 새 토큰으로 바꾼다)
CREATE TABLE refresh_token (
    token_id       BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    refresh_token  VARCHAR(512) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'JWT 원문, 대소문자 구분',
    expired_at     DATETIME(6)  NOT NULL,
    created_at     DATETIME(6)  NULL,
    updated_at     DATETIME(6)  NULL,
    PRIMARY KEY (token_id),
    UNIQUE KEY uk_refresh_token_user (user_id),
    UNIQUE KEY uk_refresh_token_token (refresh_token),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE dongs (
    dong_code         CHAR(10)     NOT NULL COMMENT '통계청(SGIS) 행정동 코드 8자리',
    name              VARCHAR(20)  NOT NULL,
    gu_name           VARCHAR(10)  NOT NULL,
    boundary          MULTIPOLYGON NOT NULL SRID 4326 COMMENT 'WGS84 위경도',
    boundary_version  VARCHAR(8)   NOT NULL COMMENT '경계 데이터 기준일 YYYYMMDD',
    PRIMARY KEY (dong_code),
    SPATIAL INDEX idx_dongs_boundary (boundary),
    CONSTRAINT ck_dongs_code CHECK (REGEXP_LIKE(dong_code, '^[0-9]{8}$')),
    CONSTRAINT ck_dongs_gu CHECK (gu_name IN ('원미구', '소사구', '오정구'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE seasons (
    season_id  SMALLINT   NOT NULL AUTO_INCREMENT,
    `year`     SMALLINT   NOT NULL,
    season     VARCHAR(6) NOT NULL,
    starts_on  DATE       NOT NULL,
    ends_on    DATE       NOT NULL,
    PRIMARY KEY (season_id),
    UNIQUE KEY uk_seasons_year_season (`year`, season),
    CONSTRAINT ck_seasons_season CHECK (season IN ('SPRING', 'SUMMER', 'AUTUMN', 'WINTER')),
    CONSTRAINT ck_seasons_range CHECK (ends_on >= starts_on)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE photos (
    photo_id        BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    dong_code       CHAR(10)     NOT NULL,
    season_id       SMALLINT     NOT NULL,
    time_band       VARCHAR(6)   NOT NULL COMMENT 'DAWN 05-08 / DAY 08-16 / SUNSET 16-19 / NIGHT 19-05',
    taken_at        DATETIME(6)  NULL COMMENT 'EXIF 촬영 시각(KST)',
    shot_date       DATE         NOT NULL COMMENT '같은 사용자·같은 날 묶음 기준',
    location        POINT        SRID 4326 NULL COMMENT '촬영 좌표',
    exif_verified   BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '시각·좌표가 있고 좌표가 동 경계 안 → 나의 지도 반영',
    palette         JSON         NOT NULL COMMENT '대표색 5개 ["#RRGGBB", ...], 첫 번째가 점 색',
    bins            JSON         NOT NULL COMMENT '색 분포 40칸 숫자 배열, 합 1.0',
    weight          FLOAT        NOT NULL DEFAULT 1.0 COMMENT '0-1, 이상치 사진은 낮춤',
    image_key       VARCHAR(255) NOT NULL COMMENT '원본 S3 키',
    thumbnail_key   VARCHAR(255) NOT NULL COMMENT '썸네일 S3 키',
    image_hash      CHAR(64)     CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '서버가 받은 파일로 계산한 SHA-256',
    is_public       BOOLEAN      NOT NULL DEFAULT TRUE,
    nearby_only     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (photo_id),
    UNIQUE KEY uk_photos_image_hash (image_hash),
    KEY idx_photos_cell (dong_code, season_id, time_band),
    KEY idx_photos_user (user_id, created_at DESC),
    -- MySQL은 부분 인덱스(WHERE exif_verified)가 없어 exif_verified를 키에 포함
    KEY idx_photos_my_map (user_id, exif_verified, season_id, dong_code, time_band),
    CONSTRAINT fk_photos_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_photos_dong FOREIGN KEY (dong_code) REFERENCES dongs (dong_code),
    CONSTRAINT fk_photos_season FOREIGN KEY (season_id) REFERENCES seasons (season_id),
    CONSTRAINT ck_photos_time_band CHECK (time_band IN ('DAWN', 'DAY', 'SUNSET', 'NIGHT')),
    CONSTRAINT ck_photos_palette CHECK (JSON_SCHEMA_VALID(
        '{"type":"array","minItems":5,"maxItems":5,"items":{"type":"string","pattern":"^#[0-9A-F]{6}$"}}', palette)),
    CONSTRAINT ck_photos_bins CHECK (JSON_SCHEMA_VALID(
        '{"type":"array","minItems":40,"maxItems":40,"items":{"type":"number","minimum":0,"maximum":1}}', bins)),
    CONSTRAINT ck_photos_weight CHECK (weight >= 0 AND weight <= 1),
    CONSTRAINT ck_photos_exif CHECK (NOT exif_verified OR (taken_at IS NOT NULL AND location IS NOT NULL)),
    CONSTRAINT ck_photos_nearby CHECK (NOT nearby_only OR location IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE color_cells (
    cell_id         BIGINT      NOT NULL AUTO_INCREMENT,
    dong_code       CHAR(10)    NOT NULL,
    season_id       SMALLINT    NOT NULL,
    time_band       VARCHAR(6)  NOT NULL,
    photo_count     INT         NOT NULL DEFAULT 0,
    shot_count      INT         NOT NULL DEFAULT 0 COMMENT '같은 사용자·같은 날을 1건으로 묶은 수 → 칸 신뢰도',
    bins_sum        JSON        NOT NULL COMMENT '가중 누적 분포 40칸',
    dominant_color  CHAR(7)     NULL,
    first_photo_id  BIGINT      NULL COMMENT '업로드 응답의 firstInCell',
    updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (cell_id),
    UNIQUE KEY uk_color_cells_cell (dong_code, season_id, time_band),
    CONSTRAINT fk_color_cells_dong FOREIGN KEY (dong_code) REFERENCES dongs (dong_code),
    CONSTRAINT fk_color_cells_season FOREIGN KEY (season_id) REFERENCES seasons (season_id),
    CONSTRAINT fk_color_cells_first_photo FOREIGN KEY (first_photo_id) REFERENCES photos (photo_id) ON DELETE SET NULL,
    CONSTRAINT ck_color_cells_time_band CHECK (time_band IN ('DAWN', 'DAY', 'SUNSET', 'NIGHT')),
    CONSTRAINT ck_color_cells_counts CHECK (photo_count >= 0 AND shot_count >= 0 AND shot_count <= photo_count),
    CONSTRAINT ck_color_cells_bins CHECK (JSON_SCHEMA_VALID(
        '{"type":"array","minItems":40,"maxItems":40,"items":{"type":"number","minimum":0}}', bins_sum)),
    CONSTRAINT ck_color_cells_color CHECK (dominant_color IS NULL OR REGEXP_LIKE(dominant_color, '^#[0-9A-F]{6}$', 'c')),
    CONSTRAINT ck_color_cells_empty CHECK ((photo_count = 0) = (dominant_color IS NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
