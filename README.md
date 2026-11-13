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
├── dongs/          # 경기도 30개 시·군 경계 (자동 실행 안 함, dongs/README.md 참고)
├── migration/      # 운영 DB에 적용할 변경 SQL (필요할 때 추가, 자동 실행 안 함)
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

**운영 DB(RDS)** 도 자동 실행되지 않습니다. RDS는 인터넷에서 접속할 수 없어서(퍼블릭 액세스 없음) **EC2 서버 안에서 관리자 계정 `admin`으로** 실행합니다. 앱 계정 `pixel`은 데이터 읽기·쓰기 권한만 있어서 테이블을 만들거나 바꿀 수 없습니다.

```bash
# EC2 서버 안, ~/BE/pixel 에서 실행. <RDS 엔드포인트>는 서버 .env의 DB_URL에 있고, 비밀번호는 입력 창에서 넣습니다.
docker run --rm -it -v "$PWD/db:/db:ro" mysql:8.4 \
  mysql -h <RDS 엔드포인트> -u admin -p --ssl-mode=REQUIRED --default-character-set=utf8mb4 pixel \
  -e "source /db/01_schema.sql"
```

**스키마를 바꿀 때**

1. `01_schema.sql`과 엔티티를 같은 PR에서 고칩니다.
2. 같은 PR에 **운영에 적용할 바뀐 부분만** 담은 SQL을 `db/migration/`에 추가합니다. 파일 이름은 `날짜_내용.sql`로 합니다 (예: `db/migration/20261108_add_photo_memo.sql`). `01`을 고쳐도 이미 만들어진 운영 DB에는 반영되지 않기 때문입니다.
3. 배포할 때 위 명령에서 파일 이름만 바꿔(`source /db/migration/파일.sql`) RDS에 실행합니다.
    - 컬럼·테이블 **추가/변경**: 서버를 다시 빌드하기 **전에** 실행합니다. 순서가 바뀌면 `ddl-auto: validate`가 불일치로 서버 시작을 막습니다.
    - 컬럼·테이블 **삭제**: 새 코드가 배포된 **후에** 실행합니다. 옛 코드가 아직 그 컬럼을 쓰고 있을 수 있습니다.
4. `db/migration/`의 파일은 로컬 Docker에서도 자동 실행되지 않습니다(맨 위 폴더의 SQL만 실행). 로컬은 `01`이 이미 최신이라 따로 할 일이 없습니다.

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

## 배포 (운영 서버)

### 구조

브라우저 → Vercel(FE, HTTPS) → `https://pixel-cs24.duckdns.org/api/*` → EC2 Nginx(HTTPS) → `127.0.0.1:8080` 백엔드 컨테이너 → RDS(MySQL) · S3

- 운영 브랜치는 `main`입니다. dev → main 릴리스 PR(`[deploy] 배포용 dev, main 머지`)을 머지한 뒤 서버에 반영합니다.
- 마이그레이션이 필요한 PR이 있으면 위 "스키마를 바꿀 때" 순서를 따릅니다. 컬럼·테이블 **추가/변경은 서버를 다시 빌드하기 전에**, **삭제는 새 코드를 배포한 후에** RDS에 적용합니다.

서버 반영은 `scripts/deploy.sh`로 합니다. 최신 main을 받아 다시 빌드하고 health까지 확인하며, 위험한 상황이면 서버를 바꾸지 않고 멈춥니다.

```bash
# EC2 서버 안
~/BE/scripts/deploy.sh              # main에 새 커밋이 있으면 배포
~/BE/scripts/deploy.sh --migrated   # 마이그레이션 확인 끝 (추가·변경은 RDS 적용 후, 삭제는 배포 후 적용)
~/BE/scripts/deploy.sh --force      # 새 커밋이 없어도 다시 빌드
```

- 새 마이그레이션 파일이 있으면 `--migrated` 없이는 멈춥니다. 이때 `cd ~/BE && git switch main && git pull --ff-only`로 파일을 받은 뒤, 위 "스키마를 바꿀 때" 규칙대로 진행합니다.
  - 추가·변경: RDS에 먼저 적용하고 `--migrated`로 다시 실행합니다.
  - 삭제만 있으면: 적용하지 않고 `--migrated`로 먼저 배포한 뒤, 배포가 끝나면 적용합니다. 스크립트가 `DROP`이 든 파일을 따로 알려 줍니다.
  - 둘 다 있으면: 추가·변경만 적용 → `--migrated`로 배포 → 삭제 적용. 그래서 추가와 삭제는 마이그레이션 파일을 나눠 만듭니다.
- 종료 코드: 0 성공·이미 최신 / 1 배포 실패(로그와 되돌리는 명령 출력) / 2 배포 전에 멈춤(서버 그대로)
- 기록: `~/pixel-deploy.log`(배포 이력), `~/pixel-deployed-commit`(마지막 성공 배포 커밋)
- 처음 한 번은 돌고 있는 커밋을 직접 기록합니다: `git -C ~/BE rev-parse HEAD > ~/pixel-deployed-commit`
- 빌드할 때마다 쌓이는 옛 이미지는 스크립트가 정리합니다(`docker image prune -f`).

### 포트와 보안 그룹 (`pixel-ec2-sg`)

| 포트 | 허용 범위 | 용도 |
|---|---|---|
| 22 | 관리자 IP만 | SSH |
| 80 | 전체 | HTTP → HTTPS 이동, 인증서 자동 갱신 (**닫으면 갱신 실패**) |
| 443 | 전체 | HTTPS (Nginx) |
| 8080 | 열지 않음 | 백엔드는 `127.0.0.1:8080`에만 열려 있어 Nginx만 접근 |

확인: `sudo ss -tlnp | grep 8080` → `127.0.0.1:8080`만 보여야 합니다.

### Nginx

- 설정 파일: `/etc/nginx/sites-available/pixel` (`/etc/nginx/sites-enabled/pixel`에 링크, 기본 사이트 `default`는 삭제)
- 443으로 받은 요청을 `http://127.0.0.1:8080`으로 전달합니다. 업로드 크기 제한 `client_max_body_size 12m`는 백엔드 요청 최대 크기(12MB)와 맞춥니다.
- 서버를 새로 만들 때 처음 넣는 설정입니다. 443 블록과 HTTPS 이동은 아래 certbot이 자동으로 추가합니다.

```nginx
server {
    listen 80;
    server_name pixel-cs24.duckdns.org;
    client_max_body_size 12m;
    server_tokens off;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

설정을 고친 뒤:

```bash
sudo nginx -t                 # 문법 확인
sudo systemctl reload nginx   # 적용
```

### HTTPS 인증서 (Let's Encrypt)

- certbot으로 발급합니다. 인증서는 90일마다 만료되고, 만료 전에 자동으로 갱신됩니다.
- 처음 발급: `sudo certbot --nginx -d pixel-cs24.duckdns.org --redirect`
- 자동 갱신 확인: `sudo certbot renew --dry-run`

### 도메인 (DuckDNS)

- `pixel-cs24.duckdns.org` → EC2 탄력적 IP `13.209.160.196`
- 탄력적 IP라 EC2를 재시작해도 바뀌지 않아 따로 갱신할 필요가 없습니다. 탄력적 IP를 바꾸면 duckdns.org에서 IP를 수정합니다.
- FE `vercel.json`의 `/api` 목적지가 이 도메인입니다.

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
