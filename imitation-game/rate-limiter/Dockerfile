FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .
COPY pom.xml .
COPY src ./src
RUN chmod +x mvnw
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
