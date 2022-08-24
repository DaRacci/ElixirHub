import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder

plugins {
    id("dev.racci.minix.kotlin")
    id("dev.racci.minix.purpurmc")
    id("dev.racci.minix.copyjar")
    id("net.minecrell.plugin-yml.bukkit") version "0.5.2"
    kotlin("plugin.serialization")
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
    libraries = listOf(
        lib.minecraft.commandAPI.get().toString(),
    )
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.racci.dev/snapshots")
}

dependencies {
    compileOnly("dev.racci:Minix:3.0.0-SNAPSHOT")

    compileOnly(lib.minecraft.commandAPI)
    compileOnly(lib.bundles.kotlin)
    compileOnly(lib.bundles.kotlinx)
    compileOnly(lib.bundles.exposed)
    compileOnly(lib.bundles.kyori)

    compileOnly(lib.caffeine)
}
