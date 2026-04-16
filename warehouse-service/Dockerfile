FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml ./
RUN mvn install -N 2>/dev/null || true
COPY warehouse-service/pom.xml ./warehouse-service/pom.xml
COPY warehouse-service/src ./warehouse-service/src
RUN mvn clean package -DskipTests -f warehouse-service/pom.xml

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/warehouse-service/target/warehouse-service-1.0.0.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
