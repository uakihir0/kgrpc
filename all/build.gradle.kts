import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.cocoapods)
    alias(libs.plugins.swiftpackage)
    id("module.publications")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    js(IR) {
        nodejs()
        browser()

        binaries.library()

        compilerOptions {
            target.set("es2015")
        }
    }

    val xcf = XCFramework("kgrpc")
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        macosX64(),
        macosArm64(),
    ).forEach {
        it.binaries.framework {
            export(project(":core"))
            baseName = "kgrpc"
            xcf.add(this)
        }
    }

    cocoapods {
        name = "kgrpc"
        version = "0.0.1"
        summary = "kgrpc is a Kotlin Multiplatform gRPC client library."
        homepage = "https://github.com/uakihir0/kgrpc"
        authors = "Akihiro Urushihara"
        license = "MIT"
        framework { baseName = "kgrpc" }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
        }
    }
}

multiplatformSwiftPackage {
    swiftToolsVersion("5.7")
    targetPlatforms {
        iOS { v("15") }
        macOS { v("12.0") }
    }
}

tasks.configureEach {
    if (name.contains("assembleKgrpc") && name.contains("XCFramework")) {
        mustRunAfter(tasks.matching { it.name.contains("FatFramework") })
    }
}

tasks.podPublishXCFramework {
    doLast {
        providers.exec {
            executable = "sh"
            args = listOf(project.projectDir.path + "/../tool/rename_podfile.sh")
        }.standardOutput.asText.get()
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}
