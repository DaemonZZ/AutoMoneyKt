plugins {
    kotlin("jvm")
}

group = "com.daemonz"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":core"))
    implementation(project(":strategies"))
    implementation(project(":runtime"))
    implementation(project(":adapters"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}