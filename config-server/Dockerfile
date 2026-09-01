# ============ Etape de build ============
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl config-server -am clean package -DskipTests

# ============ Etape d'execution ============
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/config-server/target/config-server-*.jar app.jar
# config-repo/ est lu au demarrage via spring.cloud.config.server.native.search-locations=file:./config-repo
COPY config-server/config-repo config-repo

EXPOSE 8888
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
