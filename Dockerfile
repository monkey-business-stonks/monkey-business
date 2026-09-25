# Multi-stage build for Monkey Business Server
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml and download dependencies
COPY server/pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY server/src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built JAR from builder
COPY --from=builder /build/target/monkeybusiness-server-1.0.0.jar ./monkeybusiness-server.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD java -cp monkeybusiness-server.jar org.springframework.boot.loader.Launch || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "monkeybusiness-server.jar"]
