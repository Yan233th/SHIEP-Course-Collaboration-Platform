#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
runtime_root="$project_root/.runtime"
socket="course-platform"
session="course-platform"
compose=(docker compose -p course-collab -f "$project_root/deploy/docker-compose.yml")

command -v bun >/dev/null || { echo "bun is required" >&2; exit 1; }
command -v docker >/dev/null || { echo "docker is required for local infrastructure" >&2; exit 1; }
command -v tmux >/dev/null || { echo "tmux is required" >&2; exit 1; }

if tmux -L "$socket" has-session -t "$session" 2>/dev/null; then
  echo "Local services are already running in tmux -L $socket." >&2
  exit 1
fi

wait_for() {
  local timeout="$1"
  local description="$2"
  shift 2
  local elapsed=0
  until "$@" >/dev/null 2>&1; do
    if (( elapsed >= timeout )); then
      echo "Timed out waiting for $description" >&2
      exit 1
    fi
    sleep 1
    ((elapsed += 1))
  done
}

mkdir -p "$runtime_root"
"${compose[@]}" up -d mysql redis nacos zipkin
wait_for 90 "MySQL" "${compose[@]}" exec -T mysql mysqladmin ping -h localhost -proot123 --silent
wait_for 90 "Nacos HTTP" curl -fsS http://localhost:8848/nacos/v1/ns/operator/metrics
wait_for 30 "Nacos gRPC" bash -c 'exec 3<>/dev/tcp/127.0.0.1/9848; exec 3>&-'

if [[ ! -x "$project_root/frontend/node_modules/.bin/vite" ]]; then
  (cd "$project_root/frontend" && bun install --frozen-lockfile)
fi

start_service() {
  local service="$1"
  local working_directory="$2"
  local runtime_dir="$runtime_root/$service"
  local command
  printf -v command 'exec setsid %q %q %q' "$project_root/scripts/run-local-service.sh" "$service" "$runtime_dir"

  if ! tmux -L "$socket" has-session -t "$session" 2>/dev/null; then
    tmux -L "$socket" new-session -d -s "$session" -n "$service" -c "$working_directory" "$command"
  else
    tmux -L "$socket" new-window -t "$session" -n "$service" -c "$working_directory" "$command"
  fi
}

start_service user-service "$project_root/backend/user-service"
start_service course-service "$project_root/backend/course-service"
start_service collaboration-service "$project_root/backend/collaboration-service"
start_service file-service "$project_root/backend/file-service"
start_service gateway "$project_root/backend/gateway"
start_service frontend "$project_root/frontend"

echo "Local platform started in tmux -L $socket. Use scripts/local-dev-down.sh to stop it."
