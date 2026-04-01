# Stage 1: Build
FROM gradle:9.4.1-jdk25 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew :server:buildFatJar --no-daemon

# Stage 2: Run
FROM eclipse-temurin:25-noble
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/server/build/libs/*.jar /app/server.jar
ENTRYPOINT ["java","-jar","/app/server.jar"]
