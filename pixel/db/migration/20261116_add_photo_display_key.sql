-- #154 크게 보기용 축소 이미지(긴 변 2048px 이하 JPEG) S3 키 컬럼 추가 (운영 DB 전용, 서버 재빌드 전에 실행)
-- 새 서버는 ddl-auto: validate라 이 컬럼이 없으면 뜨지 않는다. 구 서버는 이 컬럼을 모르고 NULL로 두므로 영향 없다.
-- 기존 사진과 긴 변이 2048px 이하인 JPEG은 NULL이다 (상세 응답 displayUrl = null → FE는 원본 imageUrl을 씀).
-- 다시 실행해도 안전하다: 컬럼이 이미 있으면 건너뛴다 (MySQL 8은 ADD COLUMN IF NOT EXISTS가 없어 직접 확인).

SET @has_display_key := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'photos'
      AND COLUMN_NAME = 'display_key'
);

SET @add_display_key := IF(@has_display_key = 0,
    'ALTER TABLE photos
        ADD COLUMN display_key VARCHAR(255) NULL
            COMMENT ''크게 보기용 축소 이미지 S3 키 (긴 변 2048px 이하 JPEG). NULL이면 원본을 씀''
            AFTER thumbnail_key',
    'SELECT ''display_key 컬럼이 이미 있어 건너뜀''');

PREPARE add_display_key FROM @add_display_key;
EXECUTE add_display_key;
DEALLOCATE PREPARE add_display_key;
