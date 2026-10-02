import gg.essential.gradle.util.noServerRunConfigs
import net.fabricmc.loom.task.RemapJarTask

plugins {
    kotlin("jvm")
    id("gg.essential.multi-version")
    id("gg.essential.defaults")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val modGroup: String by project
val modBaseName: String by project
group = modGroup
base.archivesName.set("$modBaseName-${platform.mcVersionStr}-${platform.loaderStr}")

loom {
    noServerRunConfigs()
}
// 26.x (unobfuscated): no remapping, so no mixin AP / refmap.
// (Checked outside `loom {}` because inside it `platform` resolves to Loom's own `platform` property.)
if (!platform.isUnobfuscated) {
    loom.mixin {
        useLegacyMixinAp = true
        defaultRefmapName.set("mixins.hypixel_auto_complete.refmap.json")
    }
}

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://repo.essential.gg/repository/maven-public")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://maven.terraformersmc.com/")
}

val libraryInclude: Configuration by configurations.creating {
    configurations.modImplementation.get().extendsFrom(this)
}

dependencies {
    val ucPlatform = when {
        platform.isFabric -> "fabric"
        platform.isForge -> "forge"
        platform.isNeoForge -> "neoforge"
        else -> error("Unable to determine platform")
    }
    if (platform.isFabric) {
        val fabricApiVersion = when (platform.mcVersion) {
            12105 -> "0.128.2+1.21.5"
            12106 -> "0.128.2+1.21.6"
            12107 -> "0.129.0+1.21.7"
            12109 -> "0.134.0+1.21.9"
            12110 -> "0.136.0+1.21.10"
            12111 -> "0.141.6+1.21.11"
            260100 -> "0.145.1+26.1"
            260200 -> "0.161.0+26.2"
            260300 -> "0.161.0+26.3"
            else -> error("Unable to determine platform")
        }

        include(modImplementation(fabricApi.module("fabric-api-base", fabricApiVersion))!!)
        include(modImplementation(fabricApi.module("fabric-command-api-v2", fabricApiVersion))!!)
//        modImplementation(fabricApiVersion)

       val modMenuVersion = when (platform.mcVersion) {
            11902 -> "4.2.0-beta.2"
            12006 -> "10.0.0"
            12105 -> "14.0.0-rc.2"
            12106, 12107, 12108 -> "15.0.0"
            12109,12110 -> "16.0.0-rc.1"
            12111 -> "17.0.1"
            260100 -> "18.0.2"
            260200 -> "20.0.3"
            260300 -> "21.0.0"
            else -> error("Unable to determine version")
        }
        modImplementation("com.terraformersmc:modmenu:$modMenuVersion")
    } else {
        libraryInclude("gg.essential:elementa:706") {
            exclude(group = "org.jetbrains.kotlin")
            exclude(module = "kotlinx-coroutines-core")
        }
        libraryInclude("gg.essential:vigilance:306") {
            exclude(group = "org.jetbrains.kotlin")
            exclude(module = "kotlinx-coroutines-core")
        }
        libraryInclude("gg.essential:universalcraft-${platform.mcVersionStr}-$ucPlatform:401") {
            exclude(group = "org.jetbrains.kotlin")
            exclude(module = "kotlinx-coroutines-core")
        }

        if (platform.isForge) {
            libraryInclude(annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.0-rc.2")!!)
        }
    }

    val devAuthPlatform = when {
        platform.isFabric -> "fabric"
        platform.isForge -> "forge-latest"
        platform.isNeoForge -> "neoforge"
        else -> error("Unable to determine platform")
    }
    modLocalRuntime("me.djtheredstoner:DevAuth-${devAuthPlatform}:${if (platform.isUnobfuscated) "1.2.2" else "1.2.1"}")
}

tasks {
    jar {
        if (!platform.isFabric) {
            manifest.attributes("MixinConfigs" to "mixins.hypixel_auto_complete.json")
        }

        dependsOn(shadowJar)
        archiveClassifier = null
    }

    if (platform.isUnobfuscated) {
        // No refmap is generated without remapping; drop the reference so Mixin doesn't warn about a missing file.
        processResources {
            filesMatching("mixins.hypixel_auto_complete.json") {
                filter { line: String -> if (line.trimStart().startsWith("\"refmap\"")) "" else line }
            }
        }
    }

    // Unobfuscated (26.x) Loom has no remapJar task; the plain `jar` is the release artifact there.
    if (!platform.isUnobfuscated) {
        named<RemapJarTask>("remapJar") {
            dependsOn(shadowJar)
            mustRunAfter(shadowJar)
            inputFile = shadowJar.get().archiveFile
            archiveClassifier = null
        }
    }

    shadowJar {
        configurations = listOf(libraryInclude)

        if (!platform.isFabric) {
            relocate("gg.essential.vigilance", "club.sk1er.mods.hypixel_auto_complete.vigilance")
            relocate("gg.essential.elementa", "club.sk1er.mods.hypixel_auto_complete.elementa")
            relocate("gg.essential.universal", "club.sk1er.mods.hypixel_auto_complete.universalcraft")

            if (platform.isForge) {
                relocate("com.llamalad7.mixinextras", "club.sk1er.mods.hypixel_auto_complete.mixinextras")
            }
        }
        mergeServiceFiles()

        if (!platform.isUnobfuscated) finalizedBy("remapJar")
    }
}