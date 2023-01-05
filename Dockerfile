FROM maven:3.8.4-openjdk-17-slim AS MAVEN_BUILD

WORKDIR /build/

COPY pom.xml mvnw mvnw.cmd /build/

RUN mvn -N io.takari:maven:wrapper

COPY src /build/src/

RUN mvn package -Dmaven.test.skip=true

FROM registry.access.redhat.com/ubi8/openjdk-11

WORKDIR /app

COPY --from=MAVEN_BUILD /build/target/generator-0.0.1-SNAPSHOT.jar /app/

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=docker","-jar", "generator-0.0.1-SNAPSHOT.jar"]
