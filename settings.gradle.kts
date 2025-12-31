plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "AutoMoneyKt"
include("core")
include("adapters")
include("runtime")
include("cli-app")
include("strategies")
include("app-ui")