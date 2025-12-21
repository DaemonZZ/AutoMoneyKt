plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "AutoMoneyKt"
include("core")
include("stategies")
include("adapters")
include("runtime")
include("cli-app")