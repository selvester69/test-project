FROM openjdk:17-jdk-slim

LABEL maintainer="inventory-service@example.com"
LABEL description="Inventory Service for stock management"

WORKDIR /app

# Copy Maven dependencies
COPY pom.xml .
COPY target/*.jar app.jar

# Expose port
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8084/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]