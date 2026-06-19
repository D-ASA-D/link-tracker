FROM eclipse-temurin:25-jdk AS build

WORKDIR /linktracker

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
COPY bot/pom.xml bot/pom.xml
COPY scrapper/pom.xml scrapper/pom.xml
COPY ai-agent/pom.xml ai-agent/pom.xml
COPY build-report-aggregate/pom.xml build-report-aggregate/pom.xml

COPY bot/src bot/src

RUN chmod +x ./mvnw
RUN ./mvnw -pl bot -am clean package -Dmaven.test.skip=true -Denforcer.skip=true

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /linktracker/bot/target/*.jar app.jar
COPY migrations /app/migrations

EXPOSE 8080
EXPOSE 8011

ENTRYPOINT ["java", "-jar", "app.jar"]
