import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://jitpack.io")
}

kotlin {
    jvm {
        jvmToolchain(11)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.compose.ui:ui-tooling-preview-desktop:1.4.0")
                implementation("org.jfree:jfreechart:1.5.4")
                implementation("com.github.jrtom:jung:master-SNAPSHOT")
                implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
                implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "GossipLogAnalyzer"
            packageVersion = "1.0.0"
        }
    }
}
