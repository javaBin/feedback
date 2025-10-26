plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvmToolchain(24)
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.bundles.database)
    implementation(libs.bundles.testcontainers)
}

