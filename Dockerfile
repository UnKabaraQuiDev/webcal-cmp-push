FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -B


FROM eclipse-temurin:21-jre

WORKDIR /opt/webcal-cmp-push

COPY --from=build /build/target/webcal-cmp-push.jar webcal-cmp-push.jar

EXPOSE 8080

CMD java \
    -Djava.security.egd=file:/dev/./urandom \
    --add-opens=java.base/java.lang=ALL-UNNAMED \
    -Xms512m \
    -Xmx1g \
    -jar webcal-cmp-push.jar