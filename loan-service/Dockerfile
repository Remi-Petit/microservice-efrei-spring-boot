# ============ Etape de build ============
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl loan-service -am clean package -DskipTests

# ============ Etape d'execution ============
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/loan-service/target/loan-service-*.jar app.jar

EXPOSE 8092
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
