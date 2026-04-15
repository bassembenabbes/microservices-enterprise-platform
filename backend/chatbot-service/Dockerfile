# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copier les fichiers de dépendances
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Compiler et empaqueter
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Créer l'utilisateur non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Installer curl pour healthcheck
RUN apk add --no-cache curl

# Copier le JAR depuis le build stage
COPY --from=build /app/target/*.jar app.jar

# Créer le dossier pour les logs
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# Passer à l'utilisateur non-root
USER appuser

# Exposer le port
EXPOSE 8005

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8005/api/chatbot/health || exit 1

# Démarrer l'application
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]
