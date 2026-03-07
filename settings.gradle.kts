import java.util.Locale

pluginManagement {
    includeBuild("plugins")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "kgrpc"

include("core")

// exclude "all" on non-Mac OS
val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
if (osName.contains("mac")) {
    include("all")
}

plugins {
    // To obtain an appropriate JVM environment in CI environments, etc.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
