package dev.racci.elixirhub

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIConfig
import dev.racci.minix.api.annotations.MappedPlugin
import dev.racci.minix.api.plugin.MinixPlugin
import java.util.logging.Level
import kotlin.reflect.full.staticProperties
import kotlin.reflect.jvm.isAccessible

@MappedPlugin(bindToKClass = ElixirHub::class)
class ElixirHub : MinixPlugin() {

    override suspend fun handleLoad() {
        logger.level = Level.ALL
        val loaded = CommandAPI::class.staticProperties
            .first { it.name == "loaded" }
            .also { it.isAccessible = true }
            .get() as Boolean
        if (!loaded) CommandAPI.onLoad(CommandAPIConfig().verboseOutput(true))
    }

    override suspend fun handleEnable() {
        val canRegister = CommandAPI::class.staticProperties
            .first { it.name == "canRegister" }
            .also { it.isAccessible = true }
            .get() as Boolean
        if (canRegister) CommandAPI.onEnable(this)
    }
}
