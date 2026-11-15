-- #142 사진 좋아요 테이블 추가 (운영 DB 전용, 재빌드 전에 실행)
-- 새 테이블이라 옛 서버는 영향을 받지 않는다. 한 번만 실행한다.
-- 중간에 오류로 멈추면 DROP TABLE photo_likes; 로 지우고 원인을 고친 뒤 처음부터 다시 실행한다.

CREATE TABLE photo_likes (
    photo_like_id   BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    photo_id        BIGINT       NOT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (photo_like_id),
    UNIQUE KEY uk_photo_likes_user_photo (user_id, photo_id),
    KEY idx_photo_likes_user (user_id, created_at DESC),
    KEY idx_photo_likes_photo (photo_id),
    CONSTRAINT fk_photo_likes_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_photo_likes_photo FOREIGN KEY (photo_id) REFERENCES photos (photo_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
