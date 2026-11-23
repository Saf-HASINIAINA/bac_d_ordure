# Image de l'application : compilation (Maven) puis exécution (JRE seul, utilisateur non privilégié).
# Construire : docker build -t ramassage .   (voir docs/07_deploiement.md)

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# les dépendances d'abord : cette couche reste en cache tant que le pom.xml ne change pas
COPY pom.xml .
RUN mvn -B -ntp -q dependency:go-offline
COPY src ./src
# les tests tournent dans l'intégration continue (GitHub Actions), pas à chaque construction d'image
RUN mvn -B -ntp -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S appli && adduser -S appli -G appli
WORKDIR /app
COPY --from=build /build/target/ramassage-*.jar app.jar
USER appli
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO /dev/null http://localhost:8080/login.html || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
