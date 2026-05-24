FROM eclipse-temurin:21-jdk

WORKDIR /app

VOLUME /tmp

COPY target/*.jar app.jar

EXPOSE 8092

ENTRYPOINT ["java", "-jar", "app.jar"]