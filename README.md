# Feedback

A feedback collection service for JavaZone, built with Kotlin and Ktor.

## Prerequisites

- JDK 25
- Docker (for running PostgreSQL locally and tests)

## Getting started

Start the database:

```bash
docker compose up -d
```

Run the application:

```bash
./gradlew :core:run
```

The server starts on `http://localhost:8080`.

## Configuration

Configuration is done via environment variables. See `.env.example` for available options.

## Testing

Tests use Testcontainers and require a running Docker daemon.

```bash
./gradlew test
```

## Building

Build a fat JAR for deployment:

```bash
./gradlew :core:buildFatJar
```

Build the Docker image:

```bash
docker build -t feedback .
```
