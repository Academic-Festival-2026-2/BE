# Pixel (픽셀) — Backend

> **사진의 색으로 그리는 동네 여행 지도**
> 2026 컴퓨터정보공학부 학술제 CLUEDAY · 팀 CS24

"모두의 사진으로 동네의 색을 칠해, 동네를 놀러 갈 곳으로 만드는 지도"

사진에 담긴 색·촬영 시각·위치를 행정동 단위로 모아, 이름난 관광지뿐 아니라 동네 자체를 "언제 가면 어떤 색을 볼 수 있는 곳"으로 보여 주는 서비스입니다.

- **MVP 범위:** 부천시 37개 행정동 × 가을(9–11월) × 시간 4구간(새벽·낮·노을·밤) = 148칸
- **프론트엔드 레포지토리:** https://github.com/Academic-Festival-2026-2/FE

> ℹ️ 초기 기획명이 `BINTEUM`이어서 패키지명(`com.binteum`)에 이전 이름이 남아 있습니다. 이후 리팩터링할 예정입니다.

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
- MySQL 8 (`binteum` 데이터베이스 생성)

```sql
CREATE DATABASE binteum DEFAULT CHARACTER SET utf8mb4;
```

### 2. 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `JWT_SECRET` | (없음, **필수**) | JWT 서명 키 (Base64, 256bit 이상) |
| `DB_URL` | `jdbc:mysql://localhost:3306/binteum...` | DB 접속 URL |
| `DB_USERNAME` | `root` | DB 사용자 |
| `DB_PASSWORD` | `password` | DB 비밀번호 |
| `SERVER_PORT` | `8080` | 서버 포트 |

### 3. 실행

```bash
cd pixel
./gradlew bootRun
```

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- 헬스 체크: http://localhost:8080/health

### 4. 테스트

```bash
cd pixel
./gradlew test
```

## 프로젝트 구조

```
pixel/src/main/java/com/binteum
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

- 브랜치: `{기능}/{이슈번호}-{내용}` (예: `feat/9-login`), PR은 `dev` 브랜치로
- 커밋: `[feat] 로그인 구현 #9`
- 코드 스타일: Google Java Style
