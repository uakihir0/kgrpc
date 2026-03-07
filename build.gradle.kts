plugins {
    id("root.publications")

    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.kotlin.cocoapods).apply(false)

    alias(libs.plugins.dokka).apply(false)
    alias(libs.plugins.maven.publish).apply(false)

    alias(libs.plugins.git.versioning)
}

allprojects {
    group = "work.socialhub.kgrpc"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

gitVersioning.apply {
    refs {
        considerTagsOnBranches = true
        tag("v(?<version>.*)") {
            version = "\${ref.version}"
        }
    }
}

tasks.wrapper {
    gradleVersion = "9.3.1"
    distributionType = Wrapper.DistributionType.ALL
}
