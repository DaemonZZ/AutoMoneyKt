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
    implementation("org.json:json:20240303")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}