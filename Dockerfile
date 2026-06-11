FROM eclipse-temurin:21-jre
EXPOSE 8080

COPY build/libs/*.jar .
CMD java -jar *.jar
