-- #111 나의 지도 공유 링크 키 컬럼 추가 (운영 DB 전용, 서버 재빌드 전 실행)

ALTER TABLE users
    ADD COLUMN share_key VARCHAR(32) NULL COMMENT '나의 지도 공유 링크 키, 공유 안 하면 NULL'
        AFTER location_agreed_at;

ALTER TABLE users
    ADD UNIQUE KEY uk_users_share_key (share_key);
