-- #114 회원 프로필 이미지 컬럼 추가 (운영 DB 전용, 서버 재빌드 전에 실행)
-- 기존 회원은 캐릭터 16개 중 랜덤으로 채운다. 새 회원은 서버가 가입 시 캐릭터 중 랜덤으로 정한다.
-- DEFAULT는 남겨 둔다. 마이그레이션 후 새 서버가 뜨기 전까지 도는 구 서버는
-- profile_image 없이 INSERT하는데, 이때도 회원가입이 실패하지 않아야 한다.
-- 캐릭터를 추가하면 ProfileImage enum과 ck_users_profile_image를 같이 바꾼다.

ALTER TABLE users
    ADD COLUMN profile_image VARCHAR(30) NOT NULL DEFAULT 'COLOR_ORANGE'
        COMMENT '프로필 이미지 키. COLOR_* 단색 / 그 외 캐릭터. 가입 시 캐릭터 중 랜덤'
        AFTER nickname;

UPDATE users
SET profile_image = ELT(FLOOR(1 + RAND() * 16),
    'BUNNY_GRAY', 'BUNNY_MOCHA', 'BUNNY_STRAWBERRY', 'BUNNY_WHITE',
    'CAT_BLUEGRAY', 'CAT_CHEESE', 'CAT_CHOCO', 'CAT_WHITE',
    'DINO_LAVENDER', 'DINO_MINT', 'DINO_PINK', 'DINO_SKY',
    'DOG_CREAM', 'DOG_GRAY', 'DOG_SHIBA', 'DOG_WHITE');

ALTER TABLE users
    ADD CONSTRAINT ck_users_profile_image CHECK (profile_image IN (
        'COLOR_ORANGE', 'COLOR_SKY', 'COLOR_NAVY', 'COLOR_GRAY',
        'BUNNY_GRAY', 'BUNNY_MOCHA', 'BUNNY_STRAWBERRY', 'BUNNY_WHITE',
        'CAT_BLUEGRAY', 'CAT_CHEESE', 'CAT_CHOCO', 'CAT_WHITE',
        'DINO_LAVENDER', 'DINO_MINT', 'DINO_PINK', 'DINO_SKY',
        'DOG_CREAM', 'DOG_GRAY', 'DOG_SHIBA', 'DOG_WHITE'));
