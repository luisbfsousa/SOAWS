FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml ./
RUN mvn install -N 2>/dev/null || true
COPY catalogue-service/pom.xml ./catalogue-service/pom.xml
COPY catalogue-service/src ./catalogue-service/src
RUN mvn clean package -DskipTests -f catalogue-service/pom.xml

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/catalogue-service/target/catalogue-service-1.0.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
