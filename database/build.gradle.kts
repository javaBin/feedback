plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.bundles.database)
    implementation(libs.bundles.testcontainers)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

