#!/usr/bin/env bash
# 운영 서버(EC2) 배포 스크립트: main 최신 코드로 백엔드를 다시 빌드하고 정상 동작까지 확인한다.
#
# 사용법 (EC2 서버 안):
#   ~/BE/scripts/deploy.sh              # main에 새 커밋이 있으면 배포
#   ~/BE/scripts/deploy.sh --migrated   # 이번 배포의 DB 마이그레이션을 RDS에 이미 적용했을 때
#   ~/BE/scripts/deploy.sh --force      # 새 커밋이 없어도 다시 빌드
#
# 묻는 단계가 없어서 나중에 GitHub Actions 자동 배포에서도 그대로 호출한다.
# 종료 코드: 0 성공(또는 이미 최신), 1 배포 실패, 2 배포 전에 멈춤(아무것도 바꾸지 않음)

set -Eeuo pipefail

BRANCH="main"
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_DIR="$REPO_DIR/pixel"
COMPOSE_FILE="docker-compose.prod.yml"
SERVICE="backend"
HEALTH_URL="http://127.0.0.1:8080/health"
PUBLIC_HEALTH_URL="https://pixel-cs24.duckdns.org/health"
HEALTH_TIMEOUT_SEC="${HEALTH_TIMEOUT_SEC:-180}"
LOG_FILE="$HOME/pixel-deploy.log"
# 마지막으로 "성공한" 배포 커밋. 실패하면 갱신하지 않아서 다음 실행이 다시 배포함
DEPLOYED_FILE="$HOME/pixel-deployed-commit"
LOCK_FILE="/tmp/pixel-deploy.lock"

MIGRATED=false
FORCE=false
PREV=""
TARGET=""
START_TIME=$(date +%s)

say() { echo "[deploy] $*"; }

record() {
  # 배포 기록: 시각 | 결과 | 이전 커밋 → 새 커밋
  echo "$(date '+%F %T') | $1 | ${PREV:0:7} -> ${TARGET:0:7}" >> "$LOG_FILE"
}

# 배포 전에 멈춤: 서버 상태를 바꾸지 않았음
stop_before_deploy() {
  say "멈춤: $*"
  say "서버는 바뀌지 않았어요."
  record "STOPPED ($*)"
  exit 2
}

# 배포 중 실패: 로그와 되돌리는 방법을 보여 줌
fail_deploy() {
  say "실패: $*"
  say "----- 최근 백엔드 로그 50줄 -----"
  (cd "$APP_DIR" && docker compose -f "$COMPOSE_FILE" logs --tail 50 "$SERVICE") || true
  say "--------------------------------"
  if [[ -n "$PREV" ]]; then
    say "이전 버전(${PREV:0:7})으로 되돌리려면:"
    say "  cd $REPO_DIR && git switch --detach $PREV && cd pixel && docker compose -f $COMPOSE_FILE up -d --build"
    say "  (고친 뒤 다음 배포 때 이 스크립트가 다시 main으로 돌아가요)"
  fi
  record "FAILED ($*)"
  exit 1
}

# 예상하지 못한 명령 실패도 기록하고 끝냄
trap 'fail_deploy "${LINENO}번째 줄 명령이 실패했어요"' ERR

for arg in "$@"; do
  case "$arg" in
    --migrated) MIGRATED=true ;;
    --force) FORCE=true ;;
    -h|--help) sed -n '2,10p' "$0"; exit 0 ;;
    *) echo "알 수 없는 옵션: $arg (--migrated, --force만 가능)"; exit 2 ;;
  esac
done

# 1. 동시에 두 번 실행되지 않게 잠금
exec 9>"$LOCK_FILE"
flock -n 9 || { say "다른 배포가 진행 중이에요."; exit 2; }

cd "$REPO_DIR"

# 2. 배포 전 점검
[[ -f "$APP_DIR/.env" ]] || stop_before_deploy "pixel/.env가 없어요"
git diff --quiet && git diff --cached --quiet \
  || stop_before_deploy "서버에서 수정된 파일이 있어요 (git status로 확인)"

DISK_USED=$(df --output=pcent / | tail -1 | tr -dc '0-9')
if (( DISK_USED >= 90 )); then
  stop_before_deploy "디스크 사용량 ${DISK_USED}% (docker system df로 확인 후 정리)"
fi

# 3. GitHub의 최신 main 확인
git fetch --quiet origin "$BRANCH"
HEAD_NOW=$(git rev-parse HEAD)
TARGET=$(git rev-parse "origin/$BRANCH")
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
# 기준은 "마지막 성공 배포". 기록이 없으면(첫 실행) 지금 코드가 배포돼 있다고 보고 바로 기록
if [[ ! -s "$DEPLOYED_FILE" ]]; then
  echo "$HEAD_NOW" > "$DEPLOYED_FILE"
  say "처음 실행: 지금 코드(${HEAD_NOW:0:7})를 마지막 배포로 기록했어요."
fi
PREV=$(cat "$DEPLOYED_FILE")

if [[ "$PREV" == "$TARGET" && "$HEAD_NOW" == "$TARGET" && "$CURRENT_BRANCH" == "$BRANCH" \
      && "$FORCE" == false ]]; then
  say "이미 최신이에요 (${TARGET:0:7}). 다시 빌드하려면 --force"
  exit 0
fi

say "마지막 배포 ${PREV:0:7} → 이번 배포 ${TARGET:0:7} ($BRANCH, 서버 코드는 지금 ${HEAD_NOW:0:7} $CURRENT_BRANCH)"
git --no-pager log --oneline "$PREV..$TARGET" 2>/dev/null | sed 's/^/[deploy]   /' || true

# 4. 새 마이그레이션이 있으면 RDS 적용 여부 확인 (빌드 전에 적용해야 서버가 뜸)
NEW_MIGRATIONS=$(git diff --name-only --diff-filter=A "$PREV" "$TARGET" -- pixel/db/migration/)
if [[ -n "$NEW_MIGRATIONS" ]]; then
  say "이번 배포에 새 마이그레이션이 있어요:"
  echo "$NEW_MIGRATIONS" | sed 's/^/[deploy]   /'
  if [[ "$MIGRATED" == false ]]; then
    stop_before_deploy "마이그레이션을 RDS에 먼저 적용한 뒤 --migrated를 붙여 다시 실행하세요"
  fi
  say "--migrated 확인: RDS에 적용했다고 보고 계속해요."
fi

# 5. 코드 반영 (main으로 전환, 빨리 감기만 허용)
git switch --quiet "$BRANCH"
git merge --quiet --ff-only "origin/$BRANCH" \
  || stop_before_deploy "서버의 main이 GitHub main과 갈라졌어요 (git log로 확인)"

# 6. 새 이미지 빌드 후 컨테이너 교체
say "빌드 시작 (몇 분 걸려요)"
cd "$APP_DIR"
docker compose -f "$COMPOSE_FILE" up -d --build \
  || fail_deploy "빌드 또는 컨테이너 실행 실패 (빌드 단계에서 멈췄다면 기존 서버는 그대로 동작 중)"

# 7. 서버 안에서 health 확인 (Spring이 뜰 때까지 기다림)
say "health 확인 중 (최대 ${HEALTH_TIMEOUT_SEC}초)"
waited=0
until [[ "$(curl -fsS --max-time 3 "$HEALTH_URL" 2>/dev/null || true)" == "OK" ]]; do
  if (( waited >= HEALTH_TIMEOUT_SEC )); then
    fail_deploy "${HEALTH_TIMEOUT_SEC}초 안에 $HEALTH_URL 이 OK를 돌려주지 않았어요"
  fi
  sleep 5
  waited=$((waited + 5))
done
say "서버 안 health OK (${waited}초)"

# 8. 바깥(Nginx·HTTPS 경유) 확인: 실패해도 배포는 끝난 상태라 경고만
if [[ "$(curl -fsS --max-time 5 "$PUBLIC_HEALTH_URL" 2>/dev/null || true)" == "OK" ]]; then
  say "외부 health OK ($PUBLIC_HEALTH_URL)"
else
  say "경고: 외부 주소 health 실패. Nginx 확인 필요 (sudo systemctl status nginx)"
fi

# 9. 이름 없는 옛 이미지 정리 (빌드할 때마다 쌓여 디스크를 채움)
docker image prune -f > /dev/null || true

echo "$TARGET" > "$DEPLOYED_FILE"
record "SUCCESS"
say "배포 완료: ${TARGET:0:7} ($(( $(date +%s) - START_TIME ))초)"
