#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
runtime_root="$project_root/.runtime"
socket="course-platform"
session="course-platform"

mapfile -t pid_files < <(find "$runtime_root" -name process-group.pid -type f 2>/dev/null | sort)
for pid_file in "${pid_files[@]}"; do
  pid="$(<"$pid_file")"
  if kill -0 "$pid" 2>/dev/null; then
    kill -TERM -- "-$pid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null || true
  fi
done

sleep 2
for pid_file in "${pid_files[@]}"; do
  pid="$(<"$pid_file")"
  if kill -0 "$pid" 2>/dev/null; then
    kill -KILL -- "-$pid" 2>/dev/null || kill -KILL "$pid" 2>/dev/null || true
  fi
done

tmux -L "$socket" kill-server 2>/dev/null || true
find "$runtime_root" -name process-group.pid -type f -delete 2>/dev/null || true

if [[ "${1:-}" == "--with-infra" ]]; then
  docker compose -p course-collab -f "$project_root/deploy/docker-compose.yml" down --remove-orphans
fi
