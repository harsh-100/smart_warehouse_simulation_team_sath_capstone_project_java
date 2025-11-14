# Project commands & quick-start (top-level)

This file documents the common commands to set up, run, and test the Smart Warehouse Simulation project.
Place yourself at the repository root (the folder that contains the `Simulation/` directory) before running these commands.

## Prerequisites

- Java Development Kit 17 (OpenJDK 17)
- Apache Maven (3.6+)

Verify your environment:

    # show java
    java -version

    # show javac
    javac -version

    # show maven
    mvn -v

If your `java` or `javac` reports a different major version than 17, either install Java 17 or configure `JAVA_HOME` to point to a JDK 17 installation. Example (zsh):

    export JAVA_HOME=/path/to/jdk-17
    export PATH="$JAVA_HOME/bin:$PATH"

## Project layout (important paths)

- Root of repo: (this file)
- Main Maven module: `Simulation/`
- Source: `Simulation/src/main/java`
- Tests: `Simulation/src/test/java`

All Maven commands below are executed from inside the `Simulation/` folder. Example:

    cd Simulation
    # now run the commands below

## Build the project

Compile sources (no tests):

    mvn clean compile -DskipTests

Package the project (creates target artifacts):

    mvn package

Note: This project uses JavaFX and is configured with the JavaFX Maven plugin. Packaging may create an application JAR, but the recommended way to run during development is the JavaFX plugin command below.

## Run the JavaFX application

Recommended (use Maven/JavaFX plugin):

    # from Simulation/
    mvn clean javafx:run -DskipTests

If you prefer to run the packaged JAR, first create it and then run (may need extra JavaFX module options depending on your platform):

    mvn clean package
    java -jar target/Simulation-0.0.1-SNAPSHOT.jar

If you get errors launching JavaFX, prefer the `mvn javafx:run` approach as it configures JavaFX modules automatically.

## Run tests

Run the entire test suite:

    mvn clean test

Run only a single test class (replace `ClassName` with the test class simple name):

    mvn -Dtest=ClassName test

Examples:

    # run only the StorageUnit tests
    mvn -Dtest=StorageUnitTest test

    # run the Item, Order and TaskManager tests only
    mvn -Dtest=ItemTest,OrderTest,TaskManagerTest test

    # run a single test method in a class
    mvn -Dtest=OrderTest#testAddItem test

Notes on test failures related to Java version
- If you see an error like "compiled by a more recent version of the Java Runtime (class file version 69.0) ... recognizes up to 61.0", then some classes were compiled with a newer JDK. Fix by cleaning and recompiling with the project JDK:

    # from repo root
    cd Simulation
    rm -rf target
    mvn clean test

## Running tests in an IDE

- Import the Maven project (open `Simulation/pom.xml`) into your IDE (IntelliJ/Eclipse/VSCode).
- Ensure the project's JDK is set to Java 17 in IDE settings and that the IDE uses the project Maven settings for compilation.
- Use the IDE's JUnit runner to run individual tests or test classes.

## Useful troubleshooting tips

- If Maven can't find the `javafx` goal: ensure `pom.xml` contains the JavaFX Maven plugin and that you're running the command from inside `Simulation/`.
- If tests fail with `UnsupportedClassVersionError`, run `mvn clean` to remove stale classes and verify `java -version`.
- If you see NPEs related to logging or file I/O during tests, try running tests with logging disabled or run targeted unit tests that don't touch the filesystem.

## Quick commands summary

    # go to module
    cd Simulation

    # full build and tests
    mvn clean test

    # run app (fast)
    mvn clean javafx:run -DskipTests

    # run one test class
    mvn -Dtest=ItemTest test

    # run one test method
    mvn -Dtest=OrderTest#testAddItem test

    # force a fresh compile if class version problems appear
    rm -rf target && mvn clean test

## Docker helper script (recommended)

A small helper script `run_in_docker.sh` is provided at the repository root to build the Docker image and run the app or tests.

Make the script executable (only needed once):

```bash
chmod +x run_in_docker.sh
```

Run the GUI app using your host X display (the script auto-detects and will forward DISPLAY if available), or falls back to using Xvfb inside the container:

```bash
./run_in_docker.sh
```

Run tests inside the container (fast, isolated):

```bash
./run_in_docker.sh test
```

If you prefer to use Docker commands directly, these are the exact commands the script uses:

```bash
# build image
docker build -t smart-warehouse-sim .

# run with host X forwarding (you may need to allow X access first with `xhost +local:docker`)
docker run --rm -it -e DISPLAY=$DISPLAY -v /tmp/.X11-unix:/tmp/.X11-unix smart-warehouse-sim mvn -f Simulation/pom.xml javafx:run -DskipTests

# run headless (container default entry: uses Xvfb)
docker run --rm -it smart-warehouse-sim

# run tests inside the image
docker run --rm smart-warehouse-sim mvn -f Simulation/pom.xml test
```


## Next recommended steps

- If you plan to add more unit tests, follow the existing test patterns in `src/test/java` and keep tests isolated (avoid heavy IO or UI initialization when possible).
- Consider adding a small CI job (GitHub Actions) that runs `mvn -DskipTests=false test` on each PR. I can create a starter workflow if you want.

---

If you'd like, I can also add a small `README-tests.md` inside `Simulation/` with examples tailored to the tests we've added and a short CI workflow. Which would you prefer next?
