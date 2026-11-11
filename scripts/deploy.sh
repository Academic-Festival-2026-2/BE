#!/usr/bin/env bash
# 운영 서버(EC2) 배포 스크립트: main 최신 코드로 백엔드를 다시 빌드하고 정상 동작까지 확인한다.
#
# 사용법 (EC2 서버 안):
#   ~/BE/scripts/deploy.sh              # main에 새 커밋이 있으면 배포
#   ~/BE/scripts/deploy.sh --migrated   # 마이그레이션 확인 끝 (추가·변경은 RDS 적용 후, 삭제는 배포 후 적용)
#   ~/BE/scripts/deploy.sh --force      # 새 커밋이 없어도 다시 빌드
#
# 처음 한 번은 지금 서버에서 돌고 있는 커밋을 직접 기록해야 한다(스크립트가 짐작하지 않음):
#   git -C ~/BE rev-parse HEAD > ~/pixel-deployed-commit
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
# health OK 뒤 이 시간 동안 컨테이너가 멈추거나 재시작되지 않는지 지켜봄
STABLE_WAIT_SEC="${STABLE_WAIT_SEC:-15}"
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
# 빌드 전(git 단계 등)에 실패하면 서버는 그대로라 "멈춤"(2), 빌드부터는 "실패"(1)
DEPLOY_STARTED=false
on_error() {
  # $(...) 안에서 난 실패는 바깥 줄에서 한 번만 처리
  (( BASH_SUBSHELL > 0 )) && return 0
  if [[ "$DEPLOY_STARTED" == true ]]; then
    fail_deploy "$1번째 줄 명령이 실패했어요"
  else
    stop_before_deploy "$1번째 줄 명령이 실패했어요"
  fi
}
trap 'on_error "$LINENO"' ERR

for arg in "$@"; do
  case "$arg" in
    --migrated) MIGRATED=true ;;
    --force) FORCE=true ;;
    -h|--help) sed -n '2,13p' "$0"; exit 0 ;;
    *) echo "알 수 없는 옵션: $arg (--migrated, --force만 가능)"; exit 2 ;;
  esac
done

# 1. 동시에 두 번 실행되지 않게 잠금
exec 9>"$LOCK_FILE"
flock -n 9 || { say "다른 배포가 진행 중이에요."; exit 2; }

cd "$REPO_DIR"

# 2. 배포 전 점검
[[ -f "$APP_DIR/.env" ]] || stop_before_deploy "pixel/.env가 없어요"
# 수정·스테이징된 파일과 Git이 모르는 새 파일까지 확인 (.env처럼 .gitignore에 있는 파일은 제외)
DIRTY=$(git status --porcelain)
if [[ -n "$DIRTY" ]]; then
  echo "$DIRTY" | head -10 | sed 's/^/[deploy]   /'
  stop_before_deploy "서버에 수정된 파일이나 Git이 모르는 파일이 있어요 (git status로 확인)"
fi

DISK_USED=$(df --output=pcent / | tail -1 | tr -dc '0-9')
if (( DISK_USED >= 90 )); then
  # image prune은 이름 없는 이미지만 지워서, 빌드 캐시는 계속 쌓임
  stop_before_deploy "디스크 사용량 ${DISK_USED}% (docker system df로 확인 후 docker builder prune -f 등으로 정리)"
fi

# 3. GitHub의 최신 main 확인
git fetch --quiet origin "$BRANCH"
HEAD_NOW=$(git rev-parse HEAD)
TARGET=$(git rev-parse "origin/$BRANCH")
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
# 기준은 "마지막 성공 배포". 기록이 없으면 지금 코드가 실제로 돌고 있는지 알 수 없어서 멈춤
if [[ ! -s "$DEPLOYED_FILE" ]]; then
  stop_before_deploy "배포 기록이 없어요. 지금 돌고 있는 커밋을 먼저 기록하세요: git -C $REPO_DIR rev-parse HEAD > $DEPLOYED_FILE"
fi
PREV=$(tr -d '[:space:]' < "$DEPLOYED_FILE")
git cat-file -e "${PREV}^{commit}" 2>/dev/null \
  || stop_before_deploy "배포 기록의 커밋(${PREV:0:7})을 찾을 수 없어요 ($DEPLOYED_FILE 확인)"

if [[ "$PREV" == "$TARGET" && "$HEAD_NOW" == "$TARGET" && "$CURRENT_BRANCH" == "$BRANCH" \
      && "$FORCE" == false ]]; then
  say "이미 최신이에요 (${TARGET:0:7}). 다시 빌드하려면 --force"
  exit 0
fi

say "마지막 배포 ${PREV:0:7} → 이번 배포 ${TARGET:0:7} ($BRANCH, 서버 코드는 지금 ${HEAD_NOW:0:7} $CURRENT_BRANCH)"
git --no-pager log --oneline "$PREV..$TARGET" 2>/dev/null | sed 's/^/[deploy]   /' || true

# 4. 새 마이그레이션이 있으면 사람이 확인할 때까지 멈춤
#    추가·변경(ADD·MODIFY)은 빌드 전에 적용해야 새 서버가 뜨고,
#    삭제(DROP)는 옛 서버가 아직 그 컬럼을 쓰므로 새 서버 배포 후에 적용 (README "스키마를 바꿀 때")
# 이미 나간 마이그레이션 파일이 수정된 경우(M)도 사람이 확인하도록 멈춤
NEW_MIGRATIONS=$(git diff --name-only --diff-filter=AM "$PREV" "$TARGET" -- pixel/db/migration/)
DROP_MIGRATIONS=""
if [[ -n "$NEW_MIGRATIONS" ]]; then
  say "이번 배포에 새 마이그레이션이 있어요:"
  echo "$NEW_MIGRATIONS" | sed 's/^/[deploy]   /'
  while IFS= read -r file; do
    # SQL 주석(--) 줄은 빼고 검사. 파이프 대신 변수에 담아 pipefail 오판을 피함
    sql=$(git show "$TARGET:$file" | grep -v '^[[:space:]]*--' || true)
    if grep -qiE '\bDROP\b' <<< "$sql"; then
      DROP_MIGRATIONS+="$file"$'\n'
    fi
  done <<< "$NEW_MIGRATIONS"
  if [[ -n "$DROP_MIGRATIONS" ]]; then
    say "주의: 아래 파일에 DROP(삭제)이 있어요. 삭제는 배포가 끝난 뒤에 적용하세요."
    printf '%s' "$DROP_MIGRATIONS" | sed 's/^/[deploy]   /'
  fi
  if [[ "$MIGRATED" == false ]]; then
    stop_before_deploy "추가·변경 마이그레이션은 RDS에 먼저 적용하고(삭제는 배포 후), --migrated를 붙여 다시 실행하세요"
  fi
  say "--migrated 확인: 추가·변경은 RDS에 적용했다고 보고 계속해요."
fi

# 5. 코드 반영 (main으로 전환, 빨리 감기만 허용). 갈라졌으면 전환하기 전에 멈춤
git merge-base --is-ancestor "refs/heads/$BRANCH" "origin/$BRANCH" \
  || stop_before_deploy "서버의 main이 GitHub main과 갈라졌어요 (git log로 확인)"
git switch --quiet "$BRANCH"
git merge --quiet --ff-only "origin/$BRANCH"

# 6. 새 이미지 빌드 후 컨테이너 교체 (여기부터 실패하면 "배포 실패")
DEPLOY_STARTED=true
say "빌드 시작 (몇 분 걸려요)"
cd "$APP_DIR"
# --force일 때는 이미지가 같아도 컨테이너를 새로 만듦 (그냥 up은 바뀐 게 없으면 그대로 둠)
UP_ARGS=(-d --build)
[[ "$FORCE" == true ]] && UP_ARGS+=(--force-recreate)
docker compose -f "$COMPOSE_FILE" up "${UP_ARGS[@]}" \
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

# 7-1. 컨테이너가 계속 떠 있는지 확인 (health 직후 죽거나 재시작을 반복하는 경우 잡기)
container_state() {
  # 결과 예: "running 0" (상태, 재시작 횟수). 컨테이너가 없으면 "missing -"
  local cid
  cid=$(docker compose -f "$COMPOSE_FILE" ps -q "$SERVICE" 2>/dev/null || true)
  [[ -n "$cid" ]] || { echo "missing -"; return 0; }
  docker inspect -f '{{.State.Status}} {{.RestartCount}}' "$cid" 2>/dev/null || echo "missing -"
}
STATE_BEFORE=$(container_state)
sleep "$STABLE_WAIT_SEC"
STATE_AFTER=$(container_state)
if [[ "${STATE_BEFORE%% *}" != running || "$STATE_AFTER" != "$STATE_BEFORE" ]]; then
  fail_deploy "health 뒤 컨테이너가 멈췄거나 재시작됐어요 (상태·재시작 횟수: $STATE_BEFORE → $STATE_AFTER)"
fi
say "컨테이너 안정 확인 (${STABLE_WAIT_SEC}초 동안 running, 재시작 없음)"

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
if [[ -n "$DROP_MIGRATIONS" ]]; then
  say "남은 일: 삭제 마이그레이션을 이제 RDS에 적용하세요."
  printf '%s' "$DROP_MIGRATIONS" | sed 's/^/[deploy]   /'
fi
