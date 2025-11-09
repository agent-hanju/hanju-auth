# Multi-stage build for optimized Docker image
# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# Copy Gradle wrapper and build files
COPY gradle gradle
COPY gradlew .
COPY settings.gradle .
COPY build.gradle .
COPY validator/build.gradle validator/
COPY authenticator/build.gradle authenticator/

# Grant execution permission to gradlew
RUN chmod +x gradlew

# Download dependencies for better layer caching
RUN ./gradlew :authenticator:dependencies --no-daemon || true

# Copy source code
COPY validator/src validator/src
COPY authenticator/src authenticator/src

# Build the application
RUN ./gradlew :authenticator:bootJar -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

# Set working directory
WORKDIR /app

# Create a non-root user
RUN groupadd -r authenticator && useradd -r -g authenticator authenticator

# Copy the jar file from builder stage
COPY --from=builder /workspace/authenticator/build/libs/authenticator.jar app.jar

# Change ownership to authenticator user
RUN chown -R authenticator:authenticator /app

# Switch to non-root user
USER authenticator:authenticator

# Expose the application port
EXPOSE 8090

# Set JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
