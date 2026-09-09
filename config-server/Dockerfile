# ============ Etape d'execution ============
# Le jar est compile en amont (mvn package) ; on copie l'artefact deja construit.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY config-server/target/config-server-*.jar app.jar
# config-repo/ est lu au demarrage via spring.cloud.config.server.native.search-locations=file:./config-repo
COPY config-server/config-repo config-repo

EXPOSE 8888
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
