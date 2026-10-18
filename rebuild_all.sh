#!/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd "$SCRIPT_DIR"

GREEN='\033[32m'
RED='\033[31m'
NC='\033[0m'

ok() {
  printf "${GREEN}OK${NC}\n"
}

fail() {
  printf "${RED}FAILED${NC}\n"
  if [ -n "${1:-}" ]; then
    printf "${RED}%s${NC}\n" "$1"
  fi
}

run_quiet() {
  local log
  log=$(mktemp)
  if "$@" >"$log" 2>&1; then
    rm -f "$log"
    return 0
  fi

  local rc=$?
  tail -n 50 "$log" >&2
  rm -f "$log"
  return $rc
}

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

write_build_info() {
  local dest="$1"
  mkdir -p "$(dirname "$dest")"
  cat > "$dest" <<EOF
{
  "branch": "$(json_escape "$BUILD_BRANCH")",
  "commit": "$(json_escape "$BUILD_COMMIT")",
  "dirty": ${BUILD_DIRTY},
  "buildTime": "$(json_escape "$BUILD_TIME")"
}
EOF
}

BUILD_BRANCH=$(git rev-parse --abbrev-ref HEAD)
BUILD_COMMIT=$(git rev-parse --short=8 HEAD)
if [ -n "$(git status --porcelain)" ]; then
  BUILD_DIRTY=true
else
  BUILD_DIRTY=false
fi
BUILD_TIME=$(date -u '+%Y-%m-%d %H:%M:%S UTC')

write_build_info "$SCRIPT_DIR/qa_hub_frontend/public/build-info.json"
write_build_info "$SCRIPT_DIR/qa_hub_backend/src/main/resources/build-info.json"

printf 'Сборка образа ... '
if run_quiet docker compose build; then
  ok
else
  fail "Не удалось собрать образы backend/frontend. Старые контейнеры не тронуты."
  exit 1
fi

printf 'Деплой образа ... '
if run_quiet docker compose up -d --remove-orphans; then
  ok
else
  fail "Не удалось запустить новые контейнеры."
  exit 1
fi

printf 'Удаление старого ... '
if run_quiet docker image prune -f; then
  ok
else
  fail "Новые контейнеры запущены, но старые образы не удалились."
  exit 1
fi
