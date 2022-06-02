package dev.racci.elixirhub

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIConfig
import dev.racci.minix.api.plugin.MinixPlugin
import kotlin.reflect.full.staticProperties

class ElixirHub : MinixPlugin() {

    override suspend fun handleLoad() {
        val loaded = CommandAPI::class.staticProperties.first { it.name == "loaded" }.get() as Boolean
        if (!loaded) CommandAPI.onLoad(CommandAPIConfig().silentLogs(true))
    }

    override suspend fun handleEnable() {
        val canRegister = CommandAPI::class.staticProperties.first { it.name == "canRegister" }.get() as Boolean
        if (canRegister) CommandAPI.onEnable(this)
    }
}
