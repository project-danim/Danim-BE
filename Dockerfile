FROM adoptopenjdk:17-jdk-hotspot


COPY ./build/libs/danim_be-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
