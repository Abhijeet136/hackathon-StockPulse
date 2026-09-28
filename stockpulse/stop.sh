#!/usr/bin/env bash
# Stop the local StockPulse services without Docker.
set -euo pipefail
cd "$(dirname "$0")"

pkill -f "spring-boot:run" || true
pkill -f "vite --host 0.0.0.0" || true
pkill -f "node.*vite" || true

echo "StockPulse local processes stopped."
