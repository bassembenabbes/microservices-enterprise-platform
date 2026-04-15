# ============ STAGE 1: BUILD ============
# Utiliser maven 3.9 avec Java 17
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copier les fichiers de dépendances
COPY pom.xml .

# Télécharger les dépendances (mise en cache)
RUN mvn dependency:go-offline -B 2>/dev/null || true

# Copier le code source
COPY src ./src

# Compiler et empaqueter
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# ============ STAGE 2: RUNTIME ============
FROM eclipse-temurin:17-jre-jammy

LABEL maintainer="Backend Team <backend@example.com>"
LABEL version="1.0.0"
LABEL description="Chatbot Service - Microservice Spring Boot"

WORKDIR /app

# Installer curl pour les health checks (Ubuntu utilise apt-get)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Créer l'utilisateur non-root (sécurité)
RUN useradd -m -u 1000 appuser && \
    chown -R appuser:appuser /app

# Créer le dossier pour les logs
RUN mkdir -p /app/logs && chown -R appuser:appuser /app/logs

# Copier le JAR du builder
COPY --from=build --chown=appuser:appuser /app/target/chatbot-service-*.jar app.jar

# Changer vers l'utilisateur non-root
USER appuser

# Ports
EXPOSE 8005

# Health check (vérifie la santé du service)
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8005/api/chatbots/health || exit 1

# JVM optimisé pour containers
ENV JAVA_OPTS="\
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+ParallelRefProcEnabled \
    -XX:G1ReservePercent=10 \
    -XX:+UseStringDeduplication \
    -Xmx256m \
    -Xms128m \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC"

# Graceful shutdown & logging
ENV SERVER_SHUTDOWN=graceful
ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS

# Entrypoint avec shell pour traiter les signaux correctement
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
