import gg.essential.gradle.util.versionFromBuildIdAndBranch

plugins {
    kotlin("jvm") version "1.9.23" apply false
    id("gg.essential.multi-version.root")
    id("gg.essential.loom") version "1.7.30" apply false
}

version = "2.0"

preprocess {
    strictExtraMappings.set(true)

    val fabric11902 = createNode("1.19.2-fabric", 11902, "yarn")
    val fabric12006 = createNode("1.20.6-fabric", 12006, "yarn")
    val fabric12105 = createNode("1.21.5-fabric", 12105, "yarn")

    val forge11902 = createNode("1.19.2-forge", 11902, "srg")
    val forge12006 = createNode("1.20.6-forge", 12006, "srg")
    val forge12105 = createNode("1.21.5-forge", 12105, "srg")

//    val neoForge12006 = createNode("1.20.6-neoforge", 12006, "srg")
//    val neoForge12105 = createNode("1.21.5-neoforge", 12105, "srg")

    fabric12006.link(fabric11902)
    fabric12105.link(fabric12006)

    forge11902.link(fabric11902)
    forge12006.link(forge11902)
    forge12105.link(forge12006)

//    neoForge12006.link(forge12006)
//    neoForge12105.link(forge12105)
}