# Stage 1: Cache
#FROM gradle:9.5.1-jdk25 AS cache
#RUN mkdir -p /home/gradle/cache_home
#ENV GRADLE_USER_HOME=/home/gradle/cache_home
#COPY build.gradle.* settings.gradle.* gradle.properties /home/gradle/app/
#COPY gradle /home/gradle/app/gradle
#WORKDIR /home/gradle/app
#RUN gradle :server:dependencies --no-daemon

# Stage 2: Build
FROM gradle:9.5.1-jdk25 AS build
#COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle :server:buildFatJar --no-daemon
#FROM gradle:9.5.1-jdk25 AS build
#COPY --chown=gradle:gradle . /home/gradle/src
#WORKDIR /home/gradle/src
#RUN chmod +x gradlew
#RUN ./gradlew :server:buildFatJar --no-daemon

# Stage 3: Runtime image
FROM eclipse-temurin:25-noble AS runtime
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/server/build/libs/*.jar /app/server.jar
ENTRYPOINT ["java","-jar","/app/server.jar"]
