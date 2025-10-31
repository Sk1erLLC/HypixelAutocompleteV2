import gg.essential.gradle.util.versionFromBuildIdAndBranch

plugins {
    kotlin("jvm") version "1.9.23" apply false
    id("gg.essential.multi-version.root")
    id("gg.essential.loom") version "1.7.35" apply false
}

version = "2.0-beta1"

preprocess {
    strictExtraMappings.set(true)

    val fabric12105 = createNode("1.21.5-fabric", 12105, "yarn")
    val fabric12106 = createNode("1.21.6-fabric", 12106, "yarn")
    val fabric12107 = createNode("1.21.7-fabric", 12107, "yarn")
    val fabric12109 = createNode("1.21.9-fabric", 12109, "yarn")
    val fabric12110 = createNode("1.21.10-fabric", 12110, "yarn")

    fabric12106.link(fabric12105)
    fabric12107.link(fabric12106)
    fabric12109.link(fabric12107)
    fabric12110.link(fabric12109)

}