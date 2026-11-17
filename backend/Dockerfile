FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml ./
COPY backend/src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/cyberladies-0.0.1-SNAPSHOT.jar /app/api.jar
USER 10001:10001
ENV PORT=8080 SPRING_PROFILES_ACTIVE=production
# Reserva memória para metaspace, threads e buffers fora do heap.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50.0 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/api.jar"]
