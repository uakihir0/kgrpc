import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    alias(libs.plugins.kotlin.multiplatform)
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

        compilerOptions {
            target.set("es2015")
        }

        compilations.all {
            compileTaskProvider.configure {
                compilerOptions { target.set("es2015") }
            }
        }
    }

    if (HostManager.hostIsMac) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
        macosX64()
        macosArm64()
    }

    linuxX64()
    mingwX64()

    targets.filterIsInstance<KotlinNativeTarget>().forEach {
        it.compilations.getByName("main") {
            cinterops {
                create("kgrpc_native") {
                    definitionFile = layout.projectDirectory.file("src/nativeInterop/cinterop/kgrpc_native.def")
                }
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    targets.filterIsInstance<KotlinNativeTarget>().forEach {
        it.compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.coroutines.core)
            implementation(libs.kotlinx.io.core)
            implementation(libs.okio)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
        }

        val jvmMain by getting {
            dependencies {
                api(libs.grpc.stub)
                api(libs.grpc.api)
                api(libs.grpc.kotlin.stub)
                implementation(libs.grpc.okhttp)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.slf4j.simple)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.js)
            }
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}
