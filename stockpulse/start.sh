#!/usr/bin/env bash
# StockPulse local development startup without Docker.
set -euo pipefail
cd "$(dirname "$0")"

echo "Run the backend and frontend in separate terminals."
echo

echo "Backend:"
echo "  cd backend"
echo "  export DB_URL='jdbc:postgresql://localhost:5432/stockpulse'"
echo "  export DB_USER='root'"
echo "  export DB_PASSWORD='root'"
echo "  export GEMINI_API_KEY='${GEMINI_API_KEY:-AQ.Ab8RN6JPa_-RdhB4U_O3auzZxUGnrZeMN5H-6_DZ14iWaDRyjg}' # optional"
echo "  export LLM_MODEL='gemini-3.5-flash-lite'"
echo "  ./mvnw spring-boot:run"
echo

echo "Frontend:"
echo "  cd frontend"
echo "  npm install"
echo "  npm run dev -- --host 0.0.0.0"
echo

echo "Open: http://localhost:5173"
