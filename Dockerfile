# Multi-purpose Dockerfile for building and running the Simulation module
# - Builds the project with Maven
# - Installs Xvfb so JavaFX can start inside the container (virtual display)

FROM eclipse-temurin:17-jdk

WORKDIR /workspace

# Install Maven, Xvfb and minimal libraries required by JavaFX
RUN apt-get update \
     && apt-get install -y --no-install-recommends \
         maven \
         xvfb \
         x11-utils \
         libgtk-3-0 \
         libglib2.0-0 \
         libx11-6 \
         libxrender1 \
         libxtst6 \
         libxext6 \
     && rm -rf /var/lib/apt/lists/*

# Copy repository into the container
COPY . /workspace

# Build the Simulation module (skip tests by default to speed up image builds)
RUN mvn -f Simulation/pom.xml -DskipTests clean package -q

# Environment: use a virtual display by default
ENV DISPLAY=:99

# Default command: start a virtual X server and run the JavaFX app via Maven
# Run the application (GUI) inside Xvfb. To run tests instead, override CMD or run the image with a different command.
CMD ["sh", "-c", "xvfb-run -s \"-screen 0 1024x768x24\" mvn -f Simulation/pom.xml javafx:run -DskipTests"]

# Notes:
# - To run the GUI on your host display, you can instead run the container with the host X11 socket mounted
#   and set DISPLAY accordingly (security implications apply):
#     docker run -it --rm -e DISPLAY=$DISPLAY -v /tmp/.X11-unix:/tmp/.X11-unix <image>
# - To run tests in the image use:
#     docker run --rm <image> mvn -f Simulation/pom.xml test
