FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml ./
RUN mvn install -N 2>/dev/null || true
COPY authentication-service/pom.xml ./authentication-service/pom.xml
COPY authentication-service/src ./authentication-service/src
RUN mvn clean package -DskipTests -f authentication-service/pom.xml

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/authentication-service/target/authenticationservice-1.0.0.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
