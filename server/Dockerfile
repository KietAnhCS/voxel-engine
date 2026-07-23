# --- Giai doan 1: build bang Maven (khong can cai Maven o may that) ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Tai thu vien truoc (tan dung cache Docker: chi tai lai khi pom.xml doi).
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Bien dich va dong goi thanh file .jar chay duoc.
COPY src ./src
RUN mvn -q clean package -DskipTests

# --- Giai doan 2: anh chay gon nhe, chi co Java + file jar ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/voxel-server-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
