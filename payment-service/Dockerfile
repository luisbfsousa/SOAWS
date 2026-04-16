FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml ./
RUN mvn install -N 2>/dev/null || true
COPY payment-service/pom.xml ./payment-service/pom.xml
COPY payment-service/src ./payment-service/src
RUN mvn clean package -DskipTests -f payment-service/pom.xml

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/payment-service/target/payment-service-1.0.0.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
