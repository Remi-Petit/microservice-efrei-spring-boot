# ============ Etape d'execution ============
# Le jar est compile en amont (mvn package) ; on copie l'artefact deja construit
# pour eviter de re-telecharger toutes les dependances Maven dans le conteneur.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY payment-service/target/payment-service-*.jar app.jar

EXPOSE 8097
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
