plugins {
    kotlin("jvm") version "2.0.0"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}
dependencies {
    implementation(project(":runtime"))
    implementation(project(":adapters"))
    implementation(project(":strategies"))
    implementation(project(":core"))

    // Coroutines + JavaFX dispatcher
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.9.0")

    // DI
    implementation(platform("io.insert-koin:koin-bom:4.1.1"))
    implementation("io.insert-koin:koin-core")

    // Logging (optional)
    implementation("org.slf4j:slf4j-simple:2.0.16")
}
application {
    mainClass.set("com.daemonz.App")
}
