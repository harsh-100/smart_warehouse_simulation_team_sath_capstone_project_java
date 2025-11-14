# Smart Warehouse Simulation

A team project simulating a warehouse with robots, storage units, orders and a task manager. The UI is implemented with JavaFX and the project is built with Maven.

## What this project does

Smart Warehouse Simulation models core warehouse components and processes:
- Robots and charging stations
- Tasks and a Task Manager that creates and assigns tasks based on orders
- Orders and Items lifecycle
- Storage units that hold items
- Logging and exception handling across modules

The project includes a JavaFX UI to visualize and interact with the simulation and a set of unit tests for core modules.

## Team & contributions

- Artem  — worked on Robots and ChargingStation related code.
- Tommi — worked on Tasks and TaskManager related code.
- Saba — worked on Orders and some UI-related features.
- Harsh — worked on StorageUnit, Item and LogManager related code.

Notes:
- Each team member managed exceptions in their area of the codebase.
- Each member added unit tests covering their modules.
- The UI is created using JavaFX and the project uses Maven for building and dependency management.

## Demo & Presentation

We have recorded a demo video and prepared a presentation for the project.

- Demo (YouTube): https://youtu.be/KJV_wGJLeCE
- Project PPT / Slides: https://docs.google.com/presentation/d/1p_9e43ztQ-rOSPFsf9Qwl8jNUqAZ4ECo/edit?usp=sharing&ouid=116256811549930036629&rtpof=true&sd=true

## Project structure (high level)

- Simulation/
  - src/main/java — application code
  - src/main/resources/fxml — JavaFX UI layout files
  - src/test/java — unit tests
  - pom.xml — Maven build configuration



## Project setup — Run using Docker + VNC viewer

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

_Note : You can check setup.md file if you face any issues here.


## Quick start

Prerequisites:
- Java 17 JDK
- Maven
- Docker (optional, for containerized runs)

Clone the repo and go to the project root (where this README sits). The main Maven module is `Simulation/`.

Build and run locally (recommended for development):

```bash
cd Simulation
mvn clean javafx:run -DskipTests
```

Run tests locally:

```bash
mvn clean test
```


## Notes and recommendations

- Log files and runtime-generated data should generally be ignored from version control. If you see log-related merge conflicts during `git pull`, stash or commit only the important changes and avoid committing log files.
- If you encounter Java class version errors (UnsupportedClassVersionError), run a clean build (`mvn clean`) and ensure your `java` and `javac` are Java 17.

## Contact

For questions about a particular module, contact the team member who worked on that module (see the Team section above).
