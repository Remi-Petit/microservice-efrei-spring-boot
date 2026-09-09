# ============ Etape de build ============
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl payment-service -am clean package -DskipTests

# ============ Etape d'execution ============
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/payment-service/target/payment-service-*.jar app.jar

EXPOSE 8097
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
