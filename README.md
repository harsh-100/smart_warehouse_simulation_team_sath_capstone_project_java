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

## Docker (containerized) usage

A Dockerfile and helper script are provided to build and run the project inside a container.

Build the image:

```bash
docker build -t smart-warehouse-sim .
```

Run the app using your host X display (so the JavaFX window appears on your desktop):

```bash
# may need to allow X access first: xhost +local:docker
docker run --rm -it -e DISPLAY=$DISPLAY -v /tmp/.X11-unix:/tmp/.X11-unix smart-warehouse-sim mvn -f Simulation/pom.xml javafx:run -DskipTests
```

Or use the provided helper script which builds the image and auto-forwards display when available:

```bash
chmod +x run_in_docker.sh
./run_in_docker.sh         # runs the GUI (forwards host DISPLAY if available)
./run_in_docker.sh test    # runs mvn -f Simulation/pom.xml test inside the container
```

## Project structure (high level)

- Simulation/
  - src/main/java — application code
  - src/main/resources/fxml — JavaFX UI layout files
  - src/test/java — unit tests
  - pom.xml — Maven build configuration

## Notes and recommendations

- Log files and runtime-generated data should generally be ignored from version control. If you see log-related merge conflicts during `git pull`, stash or commit only the important changes and avoid committing log files.
- If you encounter Java class version errors (UnsupportedClassVersionError), run a clean build (`mvn clean`) and ensure your `java` and `javac` are Java 17.

## Contact

For questions about a particular module, contact the team member who worked on that module (see the Team section above).
