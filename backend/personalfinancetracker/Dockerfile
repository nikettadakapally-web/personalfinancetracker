FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend/personalfinancetracker/pom.xml backend/personalfinancetracker/pom.xml
COPY backend/personalfinancetracker/src backend/personalfinancetracker/src
COPY dashboard-site ./dashboard-site
WORKDIR /app/backend/personalfinancetracker
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/backend/personalfinancetracker/target/personalfinancetracker-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
