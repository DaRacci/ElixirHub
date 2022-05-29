import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder

plugins {
    id("dev.racci.minix.kotlin")
    id("dev.racci.minix.purpurmc")
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
    kotlin("plugin.serialization")
}

bukkit {
    name = project.name
    prefix = project.name
    author = "Racci"
    apiVersion = "1.18"
    version = rootProject.version.toString()
    main = "dev.racci.elixirhub.ElixirHub"
    load = PluginLoadOrder.POSTWORLD
    depend = listOf("Minix")
    libraries = listOf(
        libs.minecraft.commandAPI.get().toString()
    )
}

repositories {
    maven("https://repo.racci.dev/snapshots")
}

dependencies {
    implementation("dev.racci:Minix:3.0.0-SNAPSHOT")
}
