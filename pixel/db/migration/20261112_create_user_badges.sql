-- #126 활동 배지 테이블 추가 (운영 DB 전용, 서버를 멈춘 뒤 재빌드 전에 실행)
-- 실행 전 확인: SELECT @@global.time_zone, CONVERT_TZ(NOW(), @@global.time_zone, '+09:00');
--   두 번째 값이 NULL이면 실행하지 않는다(시간대 이름을 DB가 모름). 지금 한국 시각이 나오면 정상.
-- 중간에 오류로 멈추면 DROP TABLE user_badges; 로 지우고 원인을 고친 뒤 처음부터 다시 실행한다.
-- 새 테이블이라 옛 서버는 영향을 받지 않지만, 실행 뒤 옛 서버에 올라온 사진은 배지가 채워지지 않는다. 한 번만 실행한다.
-- 배지 종류를 바꾸면 BadgeType enum과 ck_user_badges_type, FE 배지 목록을 같이 바꾼다.

CREATE TABLE user_badges (
    user_badge_id   BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    badge_type      VARCHAR(20)  NOT NULL,
    photo_id        BIGINT       NULL COMMENT '배지를 받게 한 사진, 사진을 지우면 NULL',
    dong_code       CHAR(10)     NULL COMMENT 'DONG_FIRST·DONG_TOP10만, 화면의 동 이름 표시용',
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_badge_id),
    UNIQUE KEY uk_user_badges_user_type (user_id, badge_type),
    CONSTRAINT fk_user_badges_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_badges_photo FOREIGN KEY (photo_id) REFERENCES photos (photo_id) ON DELETE SET NULL,
    CONSTRAINT fk_user_badges_dong FOREIGN KEY (dong_code) REFERENCES dongs (dong_code),
    CONSTRAINT ck_user_badges_type CHECK (badge_type IN (
        'FIRST_PHOTO', 'DONG_FIRST', 'DONG_TOP10', 'DONG_FIVE', 'ALL_TIME_BANDS', 'PHOTO_TEN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 기존 사진으로 배지를 미리 채운다. 서버(BadgeService)와 같은 기준이고, 받은 시각은 그 사진을 올린 시각.
-- INSERT IGNORE는 쓰지 않는다. CHECK·NOT NULL 위반까지 경고로 넘겨 잘못된 값이 조용히 들어갈 수 있어서다.
-- 회원당 한 행만 나오는 배지는 일반 INSERT, 회원이 여러 후보를 가질 수 있는 동 배지는
-- ON DUPLICATE KEY UPDATE로 중복만 넘기고 ORDER BY photo_id로 가장 먼저 올린 사진을 남긴다.
-- 갱신 대상은 user_id(값 그대로)다. 자동 증가 열 user_badge_id를 쓰면 INSERT ... SELECT에서 1869 오류가 난다.
-- photos.created_at은 DB 서버 기본 시간대(RDS는 UTC)로 들어가 있어 한국 시간으로 바꿔 넣는다.
-- 앱이 넣는 새 배지 시각(Asia/Seoul)과 맞춘다. Workbench 접속 시간대와 상관없이 서버 기본값(@@global)을 기준으로 바꾼다.

-- 첫 픽셀: 회원의 첫 사진
INSERT INTO user_badges (user_id, badge_type, photo_id, dong_code, created_at)
SELECT user_id, 'FIRST_PHOTO', photo_id, NULL, CONVERT_TZ(created_at, @@global.time_zone, '+09:00')
FROM (
    SELECT p.user_id, p.photo_id, p.created_at,
           ROW_NUMBER() OVER (PARTITION BY p.user_id ORDER BY p.photo_id) AS rn
    FROM photos p
) t
WHERE rn = 1;

-- 열 번째 픽셀: 회원의 10번째 사진
INSERT INTO user_badges (user_id, badge_type, photo_id, dong_code, created_at)
SELECT user_id, 'PHOTO_TEN', photo_id, NULL, CONVERT_TZ(created_at, @@global.time_zone, '+09:00')
FROM (
    SELECT p.user_id, p.photo_id, p.created_at,
           ROW_NUMBER() OVER (PARTITION BY p.user_id ORDER BY p.photo_id) AS rn
    FROM photos p
) t
WHERE rn = 10;

-- 동네 마실: 5번째로 새 동에 올린 사진
INSERT INTO user_badges (user_id, badge_type, photo_id, dong_code, created_at)
SELECT user_id, 'DONG_FIVE', photo_id, NULL, CONVERT_TZ(created_at, @@global.time_zone, '+09:00')
FROM (
    SELECT user_id, photo_id, created_at,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY photo_id) AS rn
    FROM (
        SELECT p.user_id, p.photo_id, p.created_at,
               ROW_NUMBER() OVER (PARTITION BY p.user_id, p.dong_code ORDER BY p.photo_id) AS dong_rn
        FROM photos p
    ) first_in_dong
    WHERE dong_rn = 1
) t
WHERE rn = 5;

-- 하루의 색: 4번째로 새 시간대에 올린 사진
INSERT INTO user_badges (user_id, badge_type, photo_id, dong_code, created_at)
SELECT user_id, 'ALL_TIME_BANDS', photo_id, NULL, CONVERT_TZ(created_at, @@global.time_zone, '+09:00')
FROM (
    SELECT user_id, photo_id, created_at,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY photo_id) AS rn
    FROM (
        SELECT p.user_id, p.photo_id, p.created_at,
               ROW_NUMBER() OVER (PARTITION BY p.user_id, p.time_band ORDER BY p.photo_id) AS band_rn
        FROM photos p
    ) first_in_band
    WHERE band_rn = 1
) t
WHERE rn = 4;

-- 동네 개척자 / 초기 탐험대: 같은 동·계절의 모두의 지도 사진 중 1번째 / 10번째까지
INSERT INTO user_badges (user_id, badge_type, photo_id, dong_code, created_at)
SELECT user_id, 'DONG_FIRST', photo_id, dong_code, CONVERT_TZ(created_at, @@global.time_zone, '+09:00')
FROM (
    SELECT p.user_id, p.photo_id, p.dong_code, p.created_at,
           ROW_NUMBER() OVER (PARTITION BY p.dong_code, p.season_id ORDER BY p.photo_id) AS rn
    FROM photos p
    WHERE p.exif_verified = TRUE AND p.ai_verdict = 'PUBLIC'
) t
WHERE rn = 1
ORDER BY photo_id
ON DUPLICATE KEY UPDATE user_id = user_badges.user_id;

INSERT INTO user_badges (user_id, badge_type, photo_id, dong_code, created_at)
SELECT user_id, 'DONG_TOP10', photo_id, dong_code, CONVERT_TZ(created_at, @@global.time_zone, '+09:00')
FROM (
    SELECT p.user_id, p.photo_id, p.dong_code, p.created_at,
           ROW_NUMBER() OVER (PARTITION BY p.dong_code, p.season_id ORDER BY p.photo_id) AS rn
    FROM photos p
    WHERE p.exif_verified = TRUE AND p.ai_verdict = 'PUBLIC'
) t
WHERE rn <= 10
ORDER BY photo_id
ON DUPLICATE KEY UPDATE user_id = user_badges.user_id;
