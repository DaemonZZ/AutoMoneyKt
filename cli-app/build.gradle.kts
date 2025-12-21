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
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}