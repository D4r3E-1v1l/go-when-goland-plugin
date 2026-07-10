import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        goland("2025.3.5")
        bundledPlugin("org.jetbrains.plugins.go")
        testFramework(TestFrameworkType.Platform)
    }
}

tasks.named<Zip>("buildPlugin") {
    archiveFileName.set("go-when-goland-plugin-${project.version}.zip")
}
