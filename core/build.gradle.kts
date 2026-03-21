plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":database"))
    implementation(libs.bundles.ktor)
    implementation(libs.qrcode)
    // Use JUnit Jupiter for testing.
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(25)
}

application {
    // Define the main class for the application.
    mainClass = "no.javazone.feedback.AppKt"
}

tasks.test {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
