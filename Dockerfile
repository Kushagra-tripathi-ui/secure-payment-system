# ─── STAGE 1: BUILD ──────────────────────────────────────────────────────────
#
# CONCEPT: Multi-stage Docker build
#
# Stage 1 uses a full JDK image to COMPILE the app (mvn package).
# Stage 2 uses a slim JRE image to RUN the compiled JAR.
#
# Why two stages?
# - The build tools (Maven, JDK source files) are only needed at compile time.
# - Final image only contains what's needed to RUN — much smaller (~200MB vs ~600MB).
# - Smaller image = faster deployments, less attack surface, lower storage cost.

FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Set working directory inside the container
WORKDIR /app

# Copy pom.xml first and download dependencies.
# IMPORTANT: This is a Docker layer caching optimization.
# If pom.xml doesn't change, Docker reuses the cached dependency layer.
# Only code changes trigger a rebuild — not dependency re-downloads.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─── STAGE 2: RUN ─────────────────────────────────────────────────────────────
#
# Use slim JRE (Java Runtime only — no compiler) for the final image.
# eclipse-temurin is the recommended production-grade OpenJDK distribution.

FROM eclipse-temurin:17-jre-alpine

# Add a non-root user for security
# Running as root inside a container is a security risk.
# If the app is compromised, the attacker gets root inside the container.
RUN addgroup -S payment && adduser -S payment -G payment

WORKDIR /app

# Copy only the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership to non-root user
RUN chown payment:payment app.jar
USER payment

# Expose application port
EXPOSE 8080

# JVM tuning for containers:
# -XX:+UseContainerSupport: Respect container CPU/memory limits (not host)
# -XX:MaxRAMPercentage=75.0: Use max 75% of container memory for heap
# -Djava.security.egd: Faster startup on Linux (use /dev/urandom instead of /dev/random)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
