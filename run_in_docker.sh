#!/usr/bin/env bash
set -euo pipefail

# Simple helper to build the Docker image and run the Smart Warehouse Simulation
# - Builds the Docker image `smart-warehouse-sim`
# - If the host X11 display is available, runs the container forwarding the display
# - Otherwise falls back to the container's default (which runs JavaFX in Xvfb)
# - If called with the argument `test` it runs the Maven tests inside the container

IMAGE_NAME="smart-warehouse-sim"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "[run_in_docker] Repository root: ${SCRIPT_DIR}"

echo "[run_in_docker] Building Docker image: ${IMAGE_NAME} (this may take a while)"
docker build -t ${IMAGE_NAME} .

MODE="app"
if [[ ${#} -ge 1 ]]; then
  MODE="$1"
fi

if [[ "$MODE" == "test" ]]; then
  echo "[run_in_docker] Running tests inside container"
  docker run --rm ${IMAGE_NAME} mvn -f Simulation/pom.xml test
  exit $?
fi

# Try to run with host X11 display if available
if [[ -n "${DISPLAY:-}" && -d /tmp/.X11-unix ]]; then
  echo "[run_in_docker] Detected DISPLAY=${DISPLAY} and /tmp/.X11-unix socket. Attempting host X11 forwarding."
  # Attempt to allow local docker containers to connect to X server temporarily
  XHOST_ADDED=0
  if command -v xhost >/dev/null 2>&1; then
    echo "[run_in_docker] Running: xhost +local:docker"
    xhost +local:docker || true
    XHOST_ADDED=1
  else
    echo "[run_in_docker] Warning: xhost not found, proceeding without modifying X access control. If the container cannot connect, consider running 'xhost +local:docker' manually."
  fi

  echo "[run_in_docker] Launching container with DISPLAY forwarded"
  docker run --rm -it \
    -e DISPLAY=${DISPLAY} \
    -v /tmp/.X11-unix:/tmp/.X11-unix \
    ${IMAGE_NAME} \
    mvn -f Simulation/pom.xml javafx:run -DskipTests

  # Revoke the xhost change if we added it
  if [[ ${XHOST_ADDED} -eq 1 ]]; then
    echo "[run_in_docker] Revoking X access: xhost -local:docker"
    xhost -local:docker || true
  fi
  exit $?
fi

echo "[run_in_docker] No host DISPLAY detected, running container default (xvfb-based) entrypoint"
docker run --rm -it ${IMAGE_NAME}
