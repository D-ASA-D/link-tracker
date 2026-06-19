FROM eclipse-temurin:25-jdk AS build

WORKDIR /linktracker

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
COPY bot/pom.xml bot/pom.xml
COPY scrapper/pom.xml scrapper/pom.xml
COPY ai-agent/pom.xml ai-agent/pom.xml
COPY build-report-aggregate/pom.xml build-report-aggregate/pom.xml

COPY ai-agent/src ai-agent/src

RUN chmod +x ./mvnw
RUN ./mvnw -pl ai-agent -am clean package -DskipTests -Denforcer.skip=true

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /linktracker/ai-agent/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
