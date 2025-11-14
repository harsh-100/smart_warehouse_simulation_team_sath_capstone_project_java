# Project setup — Run using Docker + VNC viewer

Follow these 6 steps to get the project running inside Docker and view the GUI using a VNC viewer on your host machine.

### 1) Clone the repository

```bash
git clone https://github.com/harsh-100/smart_warehouse_simulation_team_sath_capstone_project_java/
```

### 2) Change into the project directory

```bash
cd smart_warehouse_simulation_team_sath_capstone_project_java
```

### 3) Build the Docker image

```bash
docker build -t smart-warehouse-sim .
```

### 4) Run the container and forward the VNC port (5900)

```bash
docker run --rm -p 5900:5900 smart-warehouse-sim
```

Note: This image/container exposes a VNC server listening on container port 5900 and maps it to your host's port 5900. Make sure no other VNC server is already running on your host while you do this.

### 5) Install a VNC viewer on your host (only the viewer is needed)

- Windows: TightVNC (Download link: https://www.tightvnc.com/download.php)
- Linux: install TigerVNC viewer

```bash
sudo apt install tigervnc-viewer
```

- macOS: install TigerVNC via Homebrew

```bash
brew install tigervnc
```

### 6) Connect with the VNC viewer

Open your VNC viewer and connect to:

```
localhost:5900
```

7) You're done — you should see the application's GUI through the VNC viewer.

Troubleshooting / notes
- If the VNC viewer fails to connect, confirm the container is running and that port 5900 is forwarded by Docker.
- If you prefer running the JavaFX UI on your host X server instead of via VNC, you can run the container with DISPLAY forwarding (requires X server on host):

```bash
# allow X access first if needed: xhost +local:docker
docker run --rm -it -e DISPLAY=$DISPLAY -v /tmp/.X11-unix:/tmp/.X11-unix smart-warehouse-sim mvn -f Simulation/pom.xml javafx:run -DskipTests
```

- The main Maven module is `Simulation/`. If you need to run tests inside the container instead of launching the GUI, use the helper script or run:

```bash
docker run --rm -it smart-warehouse-sim mvn -f Simulation/pom.xml test
```
