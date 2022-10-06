package dev.racci.elixirhub

import dev.racci.minix.api.annotations.MappedPlugin
import dev.racci.minix.api.plugin.MinixPlugin

@MappedPlugin(bindToKClass = ElixirHub::class)
class ElixirHub : MinixPlugin()
