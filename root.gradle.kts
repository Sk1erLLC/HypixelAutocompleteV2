import gg.essential.gradle.util.versionFromBuildIdAndBranch

plugins {
    kotlin("jvm") version "2.3.0" apply false
    id("gg.essential.multi-version.root")
    id("gg.essential.loom") version "1.15.50" apply false
}

version = "2.0-beta2"

preprocess {
    strictExtraMappings.set(true)

    val fabric12105 = createNode("1.21.5-fabric", 12105, "yarn")
    val fabric12106 = createNode("1.21.6-fabric", 12106, "yarn")
    val fabric12107 = createNode("1.21.7-fabric", 12107, "yarn")
    val fabric12109 = createNode("1.21.9-fabric", 12109, "yarn")
    val fabric12110 = createNode("1.21.10-fabric", 12110, "yarn")
    val fabric12111 = createNode("1.21.11-fabric", 12111, "yarn")
    // 26.x is unobfuscated (Mojang names, no yarn). The preprocessor pinned by egt 0.7.2 ignores this label,
    // but it must be a non-null string.
    val fabric260100 = createNode("26.1-fabric", 26_01_00, "mojang")
    val fabric260200 = createNode("26.2-fabric", 26_02_00, "mojang")
    val fabric260300 = createNode("26.3-fabric", 26_03_00, "mojang")

    fabric12106.link(fabric12105)
    fabric12107.link(fabric12106)
    fabric12109.link(fabric12107)
    fabric12110.link(fabric12109)
    fabric12111.link(fabric12110)
    fabric260100.link(fabric12111, file("versions/26.1-1.21.11.txt"))
    fabric260200.link(fabric260100)
    fabric260300.link(fabric260200)

}