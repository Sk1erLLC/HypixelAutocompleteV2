pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.essential.gg/repository/maven-public")
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net")
        maven("https://maven.minecraftforge.net")
        maven("https://repo.spongepowered.org/repository/maven-public/")
    }
    plugins {
        val egtVersion = "0.7.2"
        id("gg.essential.multi-version.root") version egtVersion
    }
}

listOf(
    "1.21.5-fabric",
    "1.21.6-fabric",
    "1.21.7-fabric",
    "1.21.9-fabric",
    "1.21.10-fabric",
    "1.21.11-fabric",
    "26.1-fabric",
    "26.2-fabric",
    "26.3-fabric"
).forEach { version ->
    include(":$version")
    project(":$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../../build.gradle.kts"
    }
}

rootProject.buildFileName = "root.gradle.kts"