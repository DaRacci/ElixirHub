package dev.racci.elixirhub.services

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import dev.racci.elixirhub.ElixirHub
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

@MappedExtension(ElixirHub::class, "Event Service")
class EventService(override val plugin: ElixirHub) : Extension<ElixirHub>() {

    override suspend fun handleEnable() {
        event<PlayerJoinEvent>(EventPriority.MONITOR, true) {
            giveSpawnAttributes(this.player)
        }

        event<PlayerPostRespawnEvent>(EventPriority.MONITOR, true) {
            giveSpawnAttributes(this.player)
        }

        event<EntityPotionEffectEvent>(EventPriority.MONITOR, true) {
            when {
                this.entityType !== EntityType.PLAYER -> return@event
                this.action.ordinal == 0 -> return@event
                this.modifiedType == PotionEffectType.JUMP -> cancel()
            }
        }
    }

    private fun giveSpawnAttributes(player: Player) {
        with(getAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED)) {
            baseValue *= 1.2
        }
        player.addPotionEffect(SPEED_POTION)
    }

    private fun getAttribute(
        player: Player,
        attribute: Attribute
    ): AttributeInstance {
        return player.getAttribute(attribute) ?: player.registerAttribute(attribute).let { player.getAttribute(attribute)!! }
    }

    private companion object {
        val SPEED_POTION: PotionEffect = PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 1, true, false, false)
//        val SPEED_UUID: UUID = UUID.fromString("b59f31c0-ad2d-4847-8cc4-08b7f81d8180")
//        val JUMP_UUID: UUID = UUID.fromString("b672247a-088e-49fd-b4e5-02a5c3b13947")
//        val SPEED_MODIFIER = AttributeModifier(SPEED_UUID, "HUB_SPEED", 1.4, AttributeModifier.Operation.ADD_SCALAR)
//        val JUMP_MODIFIER = AttributeModifier(JUMP_UUID, "HUB_JUMP", 1.4, AttributeModifier.Operation.ADD_SCALAR)
    }
}
