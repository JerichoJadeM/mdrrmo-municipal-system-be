FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

EXPOSE 10000

CMD ["sh", "-c", "java -Xms64m -Xmx256m -Dserver.port=$PORT -jar target/mdrrmo-system-0.0.1-SNAPSHOT.jar"]