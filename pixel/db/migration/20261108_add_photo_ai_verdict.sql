-- #93 사진 AI 검수 결과 컬럼 추가 (운영 DB 전용, 서버 재빌드 전에 실행)
-- 기존 사진은 검수 전부터 모두의 지도에 반영되어 있으므로 PUBLIC으로 채운다.
-- DEFAULT 'PUBLIC'은 남겨 둔다. 마이그레이션 후 새 서버가 뜨기 전까지 도는 구 서버는
-- ai_verdict 없이 INSERT하는데, 이때도 지금처럼 PUBLIC으로 저장되어야 업로드가 실패하지 않는다.

ALTER TABLE photos
    ADD COLUMN ai_verdict VARCHAR(13) NOT NULL DEFAULT 'PUBLIC'
        COMMENT 'AI 검수 결과. PUBLIC 모두의 지도 가능 / PERSONAL_ONLY 풍경 아님·얼굴·번호판 / UNCHECKED 판정 실패'
        AFTER exif_verified;

ALTER TABLE photos
    ADD CONSTRAINT ck_photos_ai_verdict CHECK (ai_verdict IN ('PUBLIC', 'PERSONAL_ONLY', 'UNCHECKED'));
