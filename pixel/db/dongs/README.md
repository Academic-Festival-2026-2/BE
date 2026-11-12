# 경기도 지역별 동 경계

부천(3105)을 뺀 경기도 30개 시·군의 행정동 경계. 부천은 `../02_dongs.sql`에 있다.

- 파일 하나 = 지역 하나: `regions` 1행 + 그 지역 `dongs` INSERT
- 출처: 국가데이터처 SGIS 행정구역 경계, 기준일 20250630 (`02_dongs.sql`과 같은 자료)
- 지역 코드 = 행정동 코드 앞 4자리 (수원 3101, 광명 3106, 화성 3124 …)
- Docker 첫 실행 때 자동으로 들어가지 않는다 (`db/` 바로 아래 파일만 실행). 필요할 때 직접 넣는다.

## 넣기

`01` → `02` 다음에 실행한다. 순서는 상관없고, 이미 넣은 지역을 다시 넣으면 PK 중복으로 실패한다.

```bash
# pixel 폴더에서. 지역 하나
mysql -u root -p --default-character-set=utf8mb4 pixel -e "source db/dongs/3106.sql"
```

```bash
# 전부 (Git Bash). Docker DB면 docker exec -i <컨테이너> mysql ... 로 바꾼다
cat db/dongs/*.sql | mysql -u root -p --default-character-set=utf8mb4 pixel
```

확인: `SELECT COUNT(*) FROM regions;` → 31, `SELECT COUNT(*) FROM dongs;` → 638

## 만든 방법

SGIS 경계 zip(`국가데이터처_SGIS 행정구역 통계 및 경계_20250630.zip`, 공공데이터포털)에서 레포 밖 변환 스크립트로 만들었다.
`02_dongs.sql`과 같은 변환이다. 행정동이 개편되면(SGIS 반기 갱신) 새 자료로 다시 만든다.

## 확인한 것 (2026-11-12, 로컬 MySQL)

- 31개 지역 모두 동 면적 합 = 시군구 면적 (동끼리 겹치거나 빈 곳 없음)
- 같은 변환으로 부천을 만들면 `02_dongs.sql`과 INSERT 38줄이 같다
- 무작위 좌표 6010개 + 대부도·서울시청: 동 판정 쿼리(#128 수정 후) 결과가 원본 경계와 모두 일치
- `ST_IsValid`(SRID 4326)가 거짓인 동이 55개(부천 6개 포함) 있지만 평면(SRID 0)으로는 모두 유효하다. 판정은 평면으로 한다 (#128)
