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

backend_built=0
frontend_built=0
had_error=0

printf 'Сборка образа backend ... '
if run_quiet docker compose build backend; then
  ok
  backend_built=1
else
  fail "Не удалось собрать образ backend. Контейнер backend не тронут."
  had_error=1
fi

printf 'Сборка образа frontend ... '
if run_quiet docker compose build frontend; then
  ok
  frontend_built=1
else
  fail "Не удалось собрать образ frontend. Контейнер frontend не тронут."
  had_error=1
fi

if [ "$backend_built" -eq 1 ]; then
  printf 'Деплой backend ... '
  if run_quiet docker compose up -d --no-build backend; then
    ok
  else
    fail "Не удалось запустить контейнер backend."
    had_error=1
  fi
fi

if [ "$frontend_built" -eq 1 ]; then
  printf 'Деплой frontend ... '
  if run_quiet docker compose up -d --no-build --remove-orphans frontend; then
    ok
  else
    fail "Не удалось запустить контейнер frontend."
    had_error=1
  fi
fi

if [ "$backend_built" -eq 1 ] || [ "$frontend_built" -eq 1 ]; then
  printf 'Удаление старого ... '
  if run_quiet docker image prune -f; then
    ok
  else
    fail "Старые образы не удалились."
    had_error=1
  fi
fi

if [ "$had_error" -ne 0 ]; then
  exit 1
fi

printf "${GREEN}Деплой успешно завершен${NC}\n"
