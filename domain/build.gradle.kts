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
