FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml ./
RUN mvn install -N 2>/dev/null || true
COPY cart-service/pom.xml ./cart-service/pom.xml
COPY cart-service/src ./cart-service/src
RUN mvn clean package -DskipTests -f cart-service/pom.xml

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/cart-service/target/cart-service-1.0.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
