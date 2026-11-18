# 배포 (운영 서버)

운영 서버 구조, DB 변경 적용, 배포 스크립트, 서버 설정을 정리한 문서입니다. 전체 구조 그림은 [README의 아키텍처](../README.md#아키텍처)를 참고하세요.

## 운영 DB(RDS)에 SQL 적용

운영 DB(RDS)에는 `pixel/db/`의 SQL이 자동 실행되지 않습니다. RDS는 인터넷에서 접속할 수 없어서(퍼블릭 액세스 없음) **EC2 서버 안에서 관리자 계정 `admin`으로** 실행합니다. 앱 계정 `pixel`은 데이터 읽기·쓰기 권한만 있어서 테이블을 만들거나 바꿀 수 없습니다.

```bash
# EC2 서버 안, ~/BE/pixel 에서 실행. <RDS 엔드포인트>는 서버 .env의 DB_URL에 있고, 비밀번호는 입력 창에서 넣습니다.
docker run --rm -it -v "$PWD/db:/db:ro" mysql:8.4 \
  mysql -h <RDS 엔드포인트> -u admin -p --ssl-mode=REQUIRED --default-character-set=utf8mb4 pixel \
  -e "source /db/01_schema.sql"
```


## 스키마를 바꿀 때

1. `01_schema.sql`과 엔티티를 같은 PR에서 고칩니다.
2. 같은 PR에 **운영에 적용할 바뀐 부분만** 담은 SQL을 `db/migration/`에 추가합니다. 파일 이름은 `날짜_내용.sql`로 합니다 (예: `db/migration/20261108_add_photo_memo.sql`). `01`을 고쳐도 이미 만들어진 운영 DB에는 반영되지 않기 때문입니다.
3. 배포할 때 위 명령에서 파일 이름만 바꿔(`source /db/migration/파일.sql`) RDS에 실행합니다.
    - 컬럼·테이블 **추가/변경**: 서버를 다시 빌드하기 **전에** 실행합니다. 순서가 바뀌면 `ddl-auto: validate`가 불일치로 서버 시작을 막습니다.
    - 컬럼·테이블 **삭제**: 새 코드가 배포된 **후에** 실행합니다. 옛 코드가 아직 그 컬럼을 쓰고 있을 수 있습니다.
4. `db/migration/`의 파일은 로컬 Docker에서도 자동 실행되지 않습니다(맨 위 폴더의 SQL만 실행). 로컬은 `01`이 이미 최신이라 따로 할 일이 없습니다.

## 구조

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

## 포트와 보안 그룹 (`pixel-ec2-sg`)

| 포트 | 허용 범위 | 용도 |
|---|---|---|
| 22 | 관리자 IP만 | SSH |
| 80 | 전체 | HTTP → HTTPS 이동, 인증서 자동 갱신 (**닫으면 갱신 실패**) |
| 443 | 전체 | HTTPS (Nginx) |
| 8080 | 열지 않음 | 백엔드는 `127.0.0.1:8080`에만 열려 있어 Nginx만 접근 |

확인: `sudo ss -tlnp | grep 8080` → `127.0.0.1:8080`만 보여야 합니다.

## Nginx

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

## HTTPS 인증서 (Let's Encrypt)

- certbot으로 발급합니다. 인증서는 90일마다 만료되고, 만료 전에 자동으로 갱신됩니다.
- 처음 발급: `sudo certbot --nginx -d pixel-cs24.duckdns.org --redirect`
- 자동 갱신 확인: `sudo certbot renew --dry-run`

## 도메인 (DuckDNS)

- `pixel-cs24.duckdns.org` → EC2 탄력적 IP `13.209.160.196`
- 탄력적 IP라 EC2를 재시작해도 바뀌지 않아 따로 갱신할 필요가 없습니다. 탄력적 IP를 바꾸면 duckdns.org에서 IP를 수정합니다.
- FE `vercel.json`의 `/api` 목적지가 이 도메인입니다.

