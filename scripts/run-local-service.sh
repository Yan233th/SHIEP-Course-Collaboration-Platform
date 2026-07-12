#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <service> <runtime-dir>" >&2
  exit 64
fi

service="$1"
runtime_dir="$2"
project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mkdir -p "$runtime_dir"
printf '%s\n' "$$" > "$runtime_dir/process-group.pid"

if [[ "$service" == "frontend" ]]; then
  cd "$project_root/frontend"
  exec env VITE_API_PROXY="http://localhost:42180" bun run dev -- --host 0.0.0.0 --port 5173
fi

service_dir="$project_root/backend/$service"
if [[ ! -d "$service_dir" ]]; then
  echo "Unknown local service: $service" >&2
  exit 64
fi

common_env=(
  "NACOS_ADDR=localhost:8848"
  "ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans"
)

case "$service" in
  user-service) extra_env=("MYSQL_PORT=42033" "MYSQL_PASSWORD=root123" "SERVER_PORT=8081") ;;
  course-service) extra_env=("MYSQL_PORT=42033" "MYSQL_PASSWORD=root123" "SERVER_PORT=8082") ;;
  collaboration-service) extra_env=("MYSQL_PORT=42033" "MYSQL_PASSWORD=root123" "SERVER_PORT=8083") ;;
  file-service) extra_env=("MYSQL_PORT=42033" "MYSQL_PASSWORD=root123" "SERVER_PORT=8084" "FILE_STORAGE_DIR=$runtime_dir/uploads") ;;
  gateway) extra_env=("SERVER_PORT=42180") ;;
  *) echo "Unknown local service: $service" >&2; exit 64 ;;
esac

cd "$service_dir"
exec env "${common_env[@]}" "${extra_env[@]}" mvn -q spring-boot:run
