# ЕТАП 1: Збірка (Build)
# Використовуємо образ з Maven та Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Копіюємо конфігурацію Maven та завантажуємо залежності (кешування)
COPY pom.xml .
RUN mvn dependency:go-offline

# Копіюємо код і збираємо JAR файл (пропускаючи тести для швидкості)
COPY src ./src
RUN mvn clean package -DskipTests

# ЕТАП 2: Запуск (Run)
# Використовуємо мінімальний образ Java 21 для запуску
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Копіюємо зібраний JAR файл з етапу build
COPY --from=build /app/target/*.jar app.jar

# Відкриваємо порт 8080
EXPOSE 8080

# Команда запуску
ENTRYPOINT ["java", "-jar", "app.jar"]