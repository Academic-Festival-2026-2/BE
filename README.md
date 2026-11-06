# Pixel (픽셀) — Backend

> **사진의 색으로 그리는 동네 여행 지도**
> 2026 컴퓨터정보공학부 학술제 CLUEDAY · 팀 CS24

"모두의 사진으로 동네의 색을 칠해, 동네를 놀러 갈 곳으로 만드는 지도"

사진에 담긴 색·촬영 시각·위치를 행정동 단위로 모아, 이름난 관광지뿐 아니라 동네 자체를 "언제 가면 어떤 색을 볼 수 있는 곳"으로 보여 주는 서비스입니다.

- **MVP 범위:** 부천시 37개 행정동 × 가을(9–11월) × 시간 4구간(새벽·낮·노을·밤) = 148칸
- **프론트엔드 레포지토리:** https://github.com/Academic-Festival-2026-2/FE

## 기술 스택

| 구분 | 현재 (1차) | 목표 |
|---|---|---|
| Language | Java 17 | |
| Framework | Spring Boot 3.5, Spring Security, Spring Data JPA, Validation | |
| 인증 | JWT (Access 30분 / Refresh 14일, Refresh Token Rotation) | + 카카오 로그인 (OAuth2, 확장 단계) |
| Database | MySQL 8 | PostgreSQL + PostGIS (행정동 경계 판정) |
| Infra | 로컬 실행 | AWS EC2 + RDS, S3, GitHub Actions 자동 배포 |
| 문서화 | springdoc-openapi (Swagger UI) | |
| Test | JUnit 5, Spring Security Test, H2 | |

## API

모든 API는 `/api` 아래에 있고, 로그인이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더를 사용합니다.

### ✅ 구현 완료

| Method | Path | 설명 | 로그인 |
|---|---|---|---|
| `POST` | `/api/auth/signup` | 이메일·비밀번호 회원가입 (중복 검사, BCrypt 암호화) | 불필요 |
| `POST` | `/api/auth/login` | 이메일 로그인, Access·Refresh Token 발급 | 불필요 |
| `POST` | `/api/auth/reissue` | 토큰 재발급 (회전 방식) | 불필요 |
| `POST` | `/api/auth/logout` | 로그아웃 (Refresh Token 폐기) | 필요 |
| `GET` | `/health` | 서버 상태 확인 | 불필요 |

### 🛠 개발 예정 (MVP)

| 구분 | Method · Path | 설명 | 로그인 |
|---|---|---|---|
| 모두의 지도 | `GET /api/map?season=&slot=` | 37개 동의 구간별 대표색·사진 수·칸 신뢰도 | 불필요 |
| 동 지도 | `GET /api/dongs/{dongId}?season=` | 4구간 대표색, 색 비율 상위 5개, 맑은 날 비율 | 불필요 |
| 동 지도 | `GET /api/dongs/{dongId}/points?season=&slot=` | 사진 지점(격자 반올림) 목록과 대표색 | 불필요 |
| 색으로 동 찾기 | `POST /api/search/color` | 색(1–2개)·테마로 검색, 일치도 상위 동·구간 | 불필요 |
| 사진 업로드 | `GET /api/dongs/locate?lat=&lng=` | 좌표가 속한 행정동 판정 | 필요 |
| 사진 업로드 | `POST /api/photos` | 대표색·40칸 분포·촬영 시각 등록, 날씨 태깅 후 지도 반영 | 필요 |
| 사진 업로드 | `GET /api/photos/{photoId}` | 사진 상세 (비공개 사진은 색 정보만) | 불필요 |
| 나의 지도 | `GET /api/me/map?season=` | 148칸 상태, 채운 칸 수, 빈칸 목록 | 필요 |
| 나의 지도 | `GET /api/me/map/share-image` | 공유용 픽셀 그림 이미지 생성 | 필요 |
| 확장 | `POST /api/auth/oauth/kakao` | 카카오 인가 코드로 로그인 | 불필요 |

### 공통 응답 형식

```json
{
  "isSuccess": true,
  "code": "USER200_2",
  "message": "로그인에 성공했습니다.",
  "result": {
    "accessToken": "...",
    "refreshToken": "..."
  }
}
```

- 실패 시 `isSuccess: false`이고 `result`는 생략됩니다.
- 에러 코드는 `{도메인}{HTTP상태}_{번호}` 형식입니다. (예: `USER401_1`, `COMMON500`)

## 실행 방법

### 1. 사전 준비

- JDK 17
- MySQL 8.0.17 이상

### 2. 데이터베이스 만들기

테이블은 `pixel/db/`의 SQL로 만들고, 서버는 엔티티와 맞는지만 검사합니다(`ddl-auto: validate`). **SQL을 먼저 실행해야 서버가 뜹니다.**

```
pixel/db/
├── 01_schema.sql   # 테이블 전체
├── 02_dongs.sql    # 부천 37개 동 경계 (SGIS, 기준일 20250630)
├── 03_seed.sql     # 2026 가을 판 + 148칸
└── legacy/         # 옛 빈틈 테이블 정리용 (자동 실행 안 함)
```

**Docker로 실행할 때** — DB가 처음 만들어질 때 세 파일이 순서대로 자동 실행되고, 끝난 뒤에 서버가 켜집니다.

```bash
cd pixel
docker compose up -d
```

이전에 Docker DB를 띄운 적이 있다면 볼륨을 한 번 지워야 SQL이 다시 실행됩니다. (로컬 DB 데이터도 지워집니다)

```bash
docker compose down -v
docker compose up -d
```

**로컬 MySQL로 실행할 때** — 데이터베이스를 만들고 세 파일을 순서대로 실행합니다. 예전에 `ddl-auto: update`로 만든 `pixel`이 있다면 지우고 다시 만드세요. (로컬 DB 데이터도 지워집니다)

`pixel` 폴더에서 실행합니다. `source` 방식이라 PowerShell·cmd·Git Bash 어디서나 동작합니다. `root`는 본인 DB 사용자로 바꾸고, 줄마다 비밀번호를 입력합니다.

```bash
cd pixel
mysql -u root -p -e "DROP DATABASE IF EXISTS pixel; CREATE DATABASE pixel DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p --default-character-set=utf8mb4 pixel -e "source db/01_schema.sql"
mysql -u root -p --default-character-set=utf8mb4 pixel -e "source db/02_dongs.sql"
mysql -u root -p --default-character-set=utf8mb4 pixel -e "source db/03_seed.sql"
```

- Windows에서 `mysql`을 찾을 수 없다고 나오면 전체 경로로 실행합니다. PowerShell: `& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" ...`
- IntelliJ Ultimate·DataGrip을 쓰면 Database 창에서 MySQL을 연결한 뒤, 각 SQL 파일을 우클릭 → Run으로 `pixel`에 순서대로 실행해도 됩니다.
- 확인: `SELECT (SELECT COUNT(*) FROM dongs), (SELECT COUNT(*) FROM color_cells);` → `37`, `148`

**운영 DB(RDS)** 도 자동 실행되지 않으므로 처음 한 번 위와 같은 순서로 실행합니다.

**스키마를 바꿀 때** — `01_schema.sql`과 엔티티를 같은 PR에서 고칩니다. 운영 DB를 띄운 뒤에는 `01`을 고쳐도 운영에 반영되지 않으니, 바뀐 부분만 담은 SQL 파일을 따로 추가합니다.

### 3. 환경 변수

로컬에서는 환경 변수 없이도 아래 기본값으로 실행됩니다. 바꾸고 싶은 값만 `pixel/src/main/resources/.env.example`을 참고해 지정하세요.

| 변수 | 기본값 | 설명 |
|---|---|---|
| `JWT_SECRET` | 로컬용 기본키 (`application.yml`) | JWT 서명 키 (Base64, 256bit 이상). **배포(`prod`) 환경에서는 기본값이 없어 반드시 지정** |
| `DB_URL` | `jdbc:mysql://localhost:3306/pixel...` | DB 접속 URL |
| `DB_USERNAME` | `root` | DB 사용자 |
| `DB_PASSWORD` | `password` | DB 비밀번호 |
| `SERVER_PORT` | `8080` | 서버 포트 |

| `AWS_S3_BUCKET` | 없음 | 사진 저장 버킷. 로컬은 `pixel-cs24-dev`. 없으면 사진 업로드만 500 |
| `AWS_REGION` | `ap-northeast-2` | S3 리전 (서울) |
| `AWS_ACCESS_KEY_ID` | 없음 | 로컬 개발용 S3 키. 팀원별로 따로 발급 |
| `AWS_SECRET_ACCESS_KEY` | 없음 | 위 키의 비밀값. **Git·단체방에 올리지 않기** |
`.env`는 Spring이 직접 읽지 않습니다. IntelliJ 실행 구성(Edit Configurations) → 환경 변수 → 폴더 아이콘으로 `.env` 파일을 연결해야 적용됩니다. `./gradlew bootRun`으로 실행할 때는 `.env`가 적용되지 않습니다.

로컬 개발용 키는 `pixel-cs24-dev` 버킷의 `photos/` 아래만 쓸 수 있습니다. 배포 서버는 키 없이 IAM 역할로 접근합니다.

### 4. 실행

```bash
cd pixel
./gradlew bootRun
```

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- 헬스 체크: http://localhost:8080/health

### 5. 테스트

```bash
cd pixel
./gradlew test
```

## 프로젝트 구조

```
pixel/src/main/java/com/pixel
├── domain
│   ├── user          # 회원 (controller, dto, entity, repository, service, exception)
│   └── refreshtoken  # Refresh Token 저장소
└── global
    ├── apiPayload    # 공통 응답, 에러/성공 코드, 예외 처리
    ├── config        # SecurityConfig 등
    └── security/jwt  # JwtTokenProvider, JwtAuthenticationFilter, EntryPoint
```

## 외부 데이터

- 통계지리정보서비스(SGIS) 행정구역 경계 — 부천 37개 행정동 경계
- 기상청 ASOS 시간자료 — 촬영 시점 날씨
- 한국관광공사 TourAPI — 동 주변 볼거리 (확장)

## 팀원

| 이름 | 역할 |
|---|---|
| 서재민 | 팀장 / Architect |
| 권도현 | Dev |
| 김세훈 | Dev |
| 김우용 | Dev |
| 임현진 | Design |

## 협업 규칙

브랜치·커밋·이슈·PR 규칙은 [CONTRIBUTING.md](.github/CONTRIBUTING.md)를 참고하세요.

- 커밋: `[타입] 내용 #이슈번호` (예: `[feat] 로그인 구현 #9`)
- 코드 스타일: Google Java Style
