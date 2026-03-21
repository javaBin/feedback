FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY gradle gradle
COPY gradlew settings.gradle.kts gradle.properties ./
COPY core/build.gradle.kts core/build.gradle.kts
COPY domain/build.gradle.kts domain/build.gradle.kts
COPY database/build.gradle.kts database/build.gradle.kts

RUN ./gradlew --no-daemon dependencies

COPY core core
COPY domain domain
COPY database database

RUN ./gradlew :core:buildFatJar --no-daemon

FROM gcr.io/distroless/java25-debian13

COPY --from=build /app/core/build/libs/core-all.jar /app/app.jar

EXPOSE 8080

CMD ["/app/app.jar"]
