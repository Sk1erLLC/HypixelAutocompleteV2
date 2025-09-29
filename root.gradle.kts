import gg.essential.gradle.util.versionFromBuildIdAndBranch

plugins {
    kotlin("jvm") version "1.9.23" apply false
    id("gg.essential.multi-version.root")
    id("gg.essential.loom") version "1.7.30" apply false
}

version = "2.0"

preprocess {
    strictExtraMappings.set(true)

    val fabric12105 = createNode("1.21.5-fabric", 12105, "yarn")

}