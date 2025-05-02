FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /app
COPY src ./src
COPY pom.xml .
RUN mvn -T 1C package -DskipTests

FROM openjdk:17
COPY --from=build /app/target/traker-0.0.1-SNAPSHOT.jar traker.jar
EXPOSE 8080
ENV TZ=Europe/Ulyanovsk
ENTRYPOINT ["java", "-jar", "traker.jar"]