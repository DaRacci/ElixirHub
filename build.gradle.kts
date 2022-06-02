import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder

plugins {
    id("dev.racci.minix.kotlin")
    id("dev.racci.minix.purpurmc")
    id("dev.racci.minix.copyjar")
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
        libs.minecraft.commandAPI.get().toString(),
    )
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.racci.dev/snapshots")
}

dependencies {
    implementation("dev.racci:Minix:3.0.0-SNAPSHOT")
    implementation("com.github.MilkBowl:VaultAPI:1.7")

//    compileOnly(libs.cloud.minecraft.brigadier)
//    compileOnly(libs.cloud.kotlin.coroutines)
//    compileOnly(libs.cloud.kotlin.extensions)
//    compileOnly(libs.cloud.minecraft.extras)
//    compileOnly(libs.cloud.minecraft.paper)

    compileOnly(libs.minecraft.commandAPI)
    compileOnly(libs.bundles.kotlin)
    compileOnly(libs.bundles.kotlinx)
    compileOnly(libs.bundles.exposed)
    compileOnly(libs.bundles.kyori)

    compileOnly(libs.caffeine)
}
