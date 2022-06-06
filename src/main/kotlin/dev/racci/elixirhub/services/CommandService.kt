package dev.racci.elixirhub.services

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.StringTooltip
import dev.jorel.commandapi.SuggestionInfo
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.BooleanArgument
import dev.jorel.commandapi.arguments.PlayerArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import dev.racci.elixirhub.ElixirHub
import dev.racci.elixirhub.Permission
import dev.racci.elixirhub.Region
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.completableAsync
import dev.racci.minix.api.extensions.msg
import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.extensions.worlds
import dev.racci.minix.api.utils.collections.CollectionUtils.getCast
import dev.racci.minix.api.utils.collections.CollectionUtils.getCastOrNull
import org.bukkit.World
import org.bukkit.entity.Player

@MappedExtension(ElixirHub::class, "Command Service")
class CommandService(override val plugin: ElixirHub) : Extension<ElixirHub>() {

    override suspend fun handleEnable() {
        registerCommands()
    }

    private fun registerCommands() {
        CommandAPICommand("elixirhub").apply {
            withShortDescription("Base command for ElixirHub")
        }.register()

        CommandAPICommand("region").apply {
            withShortDescription("Base command for managing regions.")
            withAliases("regions")

            val subcommands = subcommands as ArrayList

            subcommands += CommandAPICommand("wand").apply {
                withShortDescription("Gives the tool to use for region editing.")
                withPermission("elixirhub.region.wand")
                withArguments(PlayerArgument("target"))
                withAliases("tool")

                executes(
                    CommandExecutor { commandSender, anies ->
                        val target = anies.getCastOrNull<Player>(0) ?: commandSender as? Player ?: return@CommandExecutor

                        if (target.inventory.addItem(RegionService.SELECTOR).isEmpty()) {
                            commandSender.msg("&c${target.name} doesn't have enough space in their inventory to receive the wand.")
                            return@CommandExecutor
                        }

                        commandSender.msg("&b${target.name} has been given the wand.")
                    }
                )
            }

            subcommands += CommandAPICommand("create").apply {
                withShortDescription("Create a new region.")
                withPermission("elixirhub.region.create")
                withArguments(StringArgument("name"))
                withAliases("new")

                executesPlayer(
                    PlayerCommandExecutor { player, anies ->
                        val name = anies.getCast<String>(0)

                        val range = RegionService.getService().getRange(player.uniqueId).getOrElse {
                            player.msg("Please ensure you have set position 1 and 2.")
                            return@PlayerCommandExecutor
                        }

                        val region = RegionService.getService().createRegion(name, player.location.world.name, range).getOrElse {
                            player.msg(it.localizedMessage)
                            return@PlayerCommandExecutor
                        }

                        player.msg("Successfully created new region ${region.name}.")
                    }
                )
            }

            subcommands += CommandAPICommand("delete").apply {
                withShortDescription("Delete a region.")
                withPermission("elixirhub.region.delete")
                withArguments(StringArgument("name"))
                withAliases("remove")

                executesPlayer(
                    PlayerCommandExecutor { player, anies ->
                        val name = anies.getCast<String>(0)

                        val region = RegionService.getService().getRegion(name).getOrElse {
                            player.msg("Couldn't find region with name $name.")
                            return@PlayerCommandExecutor
                        }

                        RegionService.getService().regions -= region
                        player.msg("Successfully deleted region ${region.name}.")
                    }
                )
            }

            subcommands += CommandAPICommand("list").apply {
                withShortDescription("List all regions.")
                withPermission("elixirhub.region.list")
                withArguments(
                    StringArgument("world").replaceSuggestions(
                        ArgumentSuggestions.stringsWithTooltipsAsync { info ->
                            if (info.currentArg.isNullOrBlank()) {
                                completableAsync { worlds.map { mapWorldTooltip(info, it) }.toTypedArray() }
                            } else completableAsync {
                                worlds.filter { it.name.startsWith(info.currentArg) }
                                    .map { mapWorldTooltip(info, it) }.toTypedArray()
                            }
                        }
                    )
                )

                executesPlayer(
                    PlayerCommandExecutor { player, anies ->
                        val regions = RegionService.getService().getRegions()
                        val world = anies.getCast<String>(0)

                        if (regions.isEmpty()) {
                            player.msg("There are no regions.")
                            return@PlayerCommandExecutor
                        }

                        player.msg("There are ${regions.size} regions:")

                        regions.forEach { player.msg(it.toString()) }
                    }
                )
            }

            subcommands += CommandAPICommand("view").apply {
                withShortDescription("View a region.")
                withPermission("elixirhub.region.view")
                withArguments(
                    StringArgument("name").replaceSuggestions(
                        ArgumentSuggestions.stringsAsync { info ->
                            if (info.currentArg.isNullOrBlank()) {
                                completableAsync {
                                    RegionService.getService().getRegions().map(Region::name).toTypedArray()
                                }
                            } else completableAsync {
                                RegionService.getService().getRegions().filter { it.name.startsWith(info.currentArg) }.map(Region::name).toTypedArray()
                            }
                        }
                    )
                )

                executesPlayer(
                    PlayerCommandExecutor { player, anies ->
                        val name = anies.getCast<String>(0)

                        val region = RegionService.getService().getRegion(name).getOrElse {
                            player.msg("Couldn't find region with name $name.")
                            return@PlayerCommandExecutor
                        }

                        player.msg("Region ${region.name}")
                        player.msg("World: ${region.world}")
                        player.msg("Range: ${region.posRange}")
                        player.msg("Rules: ${region.ruleOverrides}")
                    }
                )
            }

            subcommands += CommandAPICommand("modify").apply {
                withShortDescription("Modify a region.")
                withPermission("elixirhub.region.modify")
                withArguments(
                    StringArgument("name").replaceSuggestions(
                        ArgumentSuggestions.stringsAsync { info ->
                            if (info.currentArg.isNullOrBlank()) {
                                completableAsync {
                                    RegionService.getService().getRegions().map(Region::name).toTypedArray()
                                }
                            } else completableAsync {
                                RegionService.getService().getRegions().filter { it.name.startsWith(info.currentArg) }.map(Region::name).toTypedArray()
                            }
                        }
                    ),
                    StringArgument("modification").replaceSuggestions(
                        ArgumentSuggestions.stringsAsync { info ->
                            completableAsync {
                                if (info.currentArg.isNullOrBlank()) {
                                    arrayOf("set", "remove")
                                } else {
                                    arrayOf("add", "remove", "set").filter { it.startsWith(info.currentArg) }.toTypedArray()
                                }
                            }
                        }
                    ),
                    StringArgument("permission").replaceSuggestions(
                        ArgumentSuggestions.stringsWithTooltipsAsync { info ->
                            if (info.currentArg.isNullOrBlank()) {
                                completableAsync {
                                    Permission.values.map(::mapPermissionTooltip).toTypedArray()
                                }
                            } else completableAsync {
                                Permission.values.filter { it.name.startsWith(info.currentArg) }.map(::mapPermissionTooltip).toTypedArray()
                            }
                        }
                    ),
                    BooleanArgument("value").replaceSuggestions(
                        ArgumentSuggestions.stringsAsync { info ->
                            if (info.previousArgs.getOrNull(1) == "remove") {
                                completableAsync {
                                    arrayOf("")
                                }
                            }
                            completableAsync {
                                if (info.currentArg.isNullOrBlank()) {
                                    arrayOf("true", "false")
                                } else {
                                    arrayOf("true", "false").filter { it.startsWith(info.currentArg) }.toTypedArray()
                                }
                            }
                        }
                    )
                )

                executesPlayer(
                    PlayerCommandExecutor { player, anies ->
                        val name = anies[0] as String
                        val modification = anies[1] as String
                        val permission = anies[2] as String
                        val value = anies.getCastOrNull<Boolean>(3)

                        val region = RegionService.getService().getRegion(name).getOrElse {
                            return@PlayerCommandExecutor player.sendMessage("<red>Region <white>$name <red>not found".parse())
                        }

                        val perm = Permission.valueOf(permission, true) ?: return@PlayerCommandExecutor player.sendMessage("<red>Invalid permission <white>$permission".parse())
                        when (modification) {
                            "remove" -> {
                                region.removeOverride(perm)
                                player.sendMessage("<green>Removed permission <white>$perm <green>from region <white>$name".parse())
                            }
                            "set" -> {
                                if (value == null) return@PlayerCommandExecutor player.sendMessage("<red>Requires value.".parse())
                                region.addOverride(perm, value)
                                player.sendMessage("<green>Set permission <white>$perm <green>to <white>$value <green>in region <white>$name".parse())
                            }
                        }
                    }
                )
            }
        }.register()
    }

    private fun mapPermissionTooltip(permission: Permission): StringTooltip {
        return StringTooltip.of(
            permission.name,
            permission.description
        )
    }

    private fun mapWorldTooltip(info: SuggestionInfo, world: World): StringTooltip {
        if (info.sender !is Player) {
            return StringTooltip.none("")
        }

        val playerWorld = (info.sender as Player).world.name
        val tooltip = StringBuilder("Is current world: ").append(world.name == playerWorld)
        return StringTooltip.of(world.name, tooltip.toString())
    }
}
