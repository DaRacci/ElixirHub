package dev.racci.elixirhub

import dev.racci.minix.api.extensions.hasPermissionOrStar
import org.bukkit.entity.Player
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties

class Permission private constructor(
    val description: String,
    val playerDefault: Boolean = false,
    val opDefault: Boolean = true,
) {
    @Transient
    lateinit var name: String
    private val builtPermission by lazy { "elixirhub.$name" }

    fun hasPermission(player: Player): Boolean {
        return player.opCheck() || player.defaultCheck() || player.normalCheck()
    }

    fun hasPermission(region: Region, player: Player): Boolean {
        return player.opCheck() || region.playerPerms[player.uniqueId]?.contains(this) == true
    }

    private fun Player.opCheck(): Boolean = opDefault && this.isOp
    // Checks that this permission isn't explicitly set to false
    private fun Player.defaultCheck(): Boolean = playerDefault && !this.permissionValue(builtPermission).toBooleanOrElse(false)
    private fun Player.normalCheck(): Boolean = this.hasPermissionOrStar(builtPermission) || this.hasPermission("*")

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Permission {
        name = property.name.lowercase()
        return Permission(description, playerDefault, opDefault)
    }

    companion object {
        val values by lazy {
            Companion::class.declaredMemberProperties.filter {
                it.returnType == Permission::class
            }.map { it.getter.call(this) as Permission }
        }

        fun valueOf(name: String, ignoreCase: Boolean = false): Permission? {
            return values.find { it.name.equals(name, ignoreCase) }
        }

        val BLOCK_PLACE by Permission("Allows the player to place blocks.")
        val BLOCK_BREAK by Permission("Allows the player to break blocks.")
        val BLOCK_FERTILISE by Permission("Allows the player to fertilise blocks.")
        val BLOCK_INTERACT by Permission("Allows the player to interact with blocks.")

        val SIGN_CHANGE by Permission("Allows the player to change signs.")

        val PVP by Permission("Allows the player to damage other players.")
        val FLY by Permission("Allows the player to fly.")
        val INTERACT by Permission("Allows the player to interact with blocks.")
        val DOOR by Permission("Allows the player to open doors and trapdoors.")
        val HUNGER by Permission("Allows the player to lose saturation.")

        val BUCKET_FILL by Permission("Allows the player to fill buckets.")
        val BUCKET_EMPTY by Permission("Allows the player to empty buckets.")
        val BUCKET_ENTITY by Permission("Allows the player to bucket an entity.")
    }
}
