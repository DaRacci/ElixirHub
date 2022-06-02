package dev.racci.elixirhub.services

import dev.racci.elixirhub.ElixirHub
import dev.racci.elixirhub.Permission
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.msg
import dev.racci.minix.api.extensions.pdc
import dev.racci.minix.api.utils.minecraft.asBlockPos
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockFertilizeEvent
import org.bukkit.event.block.BlockMultiPlaceEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.CauldronLevelChangeEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketEntityEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerInteractEvent

@MappedExtension(ElixirHub::class, "Event Service")
class EventService(override val plugin: ElixirHub) : Extension<ElixirHub>() {

    override suspend fun handleEnable() {
        bucketEvents()
        blockEvents()
        regionEvents()
    }

    private fun bucketEvents() {
        event<PlayerBucketFillEvent>(EventPriority.LOWEST) {
            if (!overrideRegionOrDefault(player, player.location, Permission.BUCKET_FILL)) return@event cancel()
        }

        event<PlayerBucketEmptyEvent>(EventPriority.LOWEST) {
            if (!overrideRegionOrDefault(player, player.location, Permission.BUCKET_EMPTY)) return@event cancel()
        }

        event<PlayerBucketEntityEvent>(EventPriority.LOWEST) {
            if (!overrideRegionOrDefault(player, player.location, Permission.BUCKET_ENTITY)) return@event cancel()
        }
    }

    private fun blockEvents() {
        event<BlockBreakEvent>(EventPriority.LOWEST) {
            if (!overrideRegionOrDefault(player, block.location, Permission.BLOCK_BREAK)) return@event cancel()
        }

        event<BlockPlaceEvent>(EventPriority.LOWEST) {
            if (!overrideRegionOrDefault(player, blockAgainst.location, Permission.BLOCK_PLACE)) return@event cancel()
        }

        event<BlockMultiPlaceEvent>(EventPriority.LOWEST) {
            if (!overrideRegionOrDefault(player, blockAgainst.location, Permission.BLOCK_PLACE)) return@event cancel()
        }

        event<BlockFertilizeEvent>(EventPriority.LOWEST) {
            if (player == null) return@event
            if (!overrideRegionOrDefault(player!!, block.location, Permission.BLOCK_FERTILISE)) return@event cancel()
        }

        event<CauldronLevelChangeEvent>(EventPriority.LOWEST) {
            if (entity == null || entity !is Player) return@event
            if (!overrideRegionOrDefault(entity as Player, block.location, Permission.BLOCK_INTERACT)) return@event cancel()
        }

        event<SignChangeEvent>(EventPriority.LOWEST) {
            if (!overrideRegionOrDefault(player, block.location, Permission.SIGN_CHANGE)) return@event cancel()
        }
    }

    private fun regionEvents() {
        event<PlayerInteractEvent>(EventPriority.LOWEST) {
            if (action == Action.PHYSICAL || !hasItem() || clickedBlock == null) return@event
            if (!player.inventory.itemInMainHand.pdc.has(RegionService.NBT_KEY)) return@event

            val type = if (action.isLeftClick) 1 else 2
            RegionService.getService().setPosition(player.uniqueId, clickedBlock!!.location, type.toShort())

            player.msg("<aqua>Set position $type to <gold>${clickedBlock!!.location}")
        }
    }

    private fun overrideRegionOrDefault(
        player: Player,
        location: Location,
        vararg perms: Permission,
    ): Boolean {
        val result = RegionService.getService().getRegionFromPos(location.asBlockPos(), location.world.name)
        if (result.isFailure) {
            return perms.any { it.hasPermission(player) }
        }

        val region = result.getOrThrow()

        for ((override, value) in region.ruleOverrides) {
            for (perm in perms) {
                // Override permissions take priority
                if (override == perm) return value
            }
        }

        return perms.any { it.hasPermission(player) }
    }
}
