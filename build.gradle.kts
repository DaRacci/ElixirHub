import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder

plugins {
    id("dev.racci.minix.kotlin")
    id("dev.racci.minix.purpurmc")
    id("dev.racci.minix.copyjar")
    id("net.minecrell.plugin-yml.bukkit") version "0.5.2"
    kotlin("plugin.serialization")
    id("dev.racci.slimjar") version "1.3.3"
}

bukkit {
    name = project.name
    prefix = project.name
    author = "Racci"
    apiVersion = "1.19"
    version = rootProject.version.toString()
    main = "dev.racci.elixirhub.ElixirHub"
    load = PluginLoadOrder.POSTWORLD
    depend = listOf("Minix")
}

repositories {
    mavenCentral()
    maven("https://repo.racci.dev/snapshots")
}

dependencies {
    compileOnly(lib.minecraft.minix)

    slim(lib.kotlinx.serialization.json)
}
