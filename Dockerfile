FROM eclipse-temurin:17-jdk

WORKDIR /workspace

# Install Maven, Xvfb, Fluxbox, VNC server and JavaFX dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    maven \
    xvfb \
    x11vnc \
    fluxbox \
    libgtk-3-0 \
    libglib2.0-0 \
    libx11-6 \
    libxrender1 \
    libxtst6 \
    libxext6 \
    && rm -rf /var/lib/apt/lists/*

# Copy your project
COPY . /workspace

# Build your JavaFX module
RUN mvn -f Simulation/pom.xml -Dmaven.test.skip=true clean package -q

# Set virtual display
ENV DISPLAY=:99

# CMD: Start virtual display, start VNC, run JavaFX app
CMD sh -c "\
  Xvfb :99 -screen 0 1024x768x24 & \
  x11vnc -display :99 -nopw -forever -shared -rfbport 5900 & \
  fluxbox & \
  mvn -f Simulation/pom.xml javafx:run -DskipTests \
"
