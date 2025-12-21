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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.9.0")
}
application {
    mainClass.set("com.daemonz.App")
}
