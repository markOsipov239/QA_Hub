#/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

cd $SCRIPT_DIR

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

# Capture repo state before generated files are written
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

echo "Build info: branch=${BUILD_BRANCH} commit=${BUILD_COMMIT} dirty=${BUILD_DIRTY} time=${BUILD_TIME}"

docker stop qa_hub_backend
docker rmi qa_hub_backend --force

docker stop qa_hub_frontend
docker rmi qa_hub_frontend --force

docker compose up -d --build