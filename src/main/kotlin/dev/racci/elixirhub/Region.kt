package dev.racci.elixirhub

import dev.racci.minix.api.extensions.worlds
import dev.racci.minix.api.serializables.LocationSerializer
import dev.racci.minix.api.serializables.UUIDSerializer
import dev.racci.minix.api.serializables.VectorSerializer
import dev.racci.minix.api.utils.minecraft.BlockPos
import dev.racci.minix.api.utils.minecraft.PosRange
import dev.racci.minix.api.utils.minecraft.RangeIteratorWithFactor
import dev.racci.minix.api.utils.minecraft.asBlockPos
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import org.bukkit.Location
import org.bukkit.util.Vector
import java.util.UUID

@Serializable
data class Region(
    var name: String,
    var world: String,
    @Serializable(with = PosRangeSerializer::class) var posRange: PosRange<@Serializable(with = LocationSerializer::class) Location, @Contextual BlockPos>,
    var ruleOverrides: HashMap<@Serializable(with = PermissionSerializer::class) Permission, Boolean>,
    var playerPerms: MutableMap<@Serializable(with = UUIDSerializer::class) UUID, HashSet<@Serializable(with = PermissionSerializer::class) Permission>>
) {
    fun getPermissions(uuid: UUID): PersistentSet<Permission> {
        return playerPerms[uuid]?.toPersistentSet() ?: persistentSetOf()
    }

    fun addPermission(
        uuid: UUID,
        permission: Permission
    ): Boolean {
        if (playerPerms[uuid]?.contains(permission) == true) return false
        playerPerms[uuid] = playerPerms[uuid] ?: hashSetOf()
        playerPerms[uuid]!!.add(permission)
        return true
    }

    fun removePermission(
        uuid: UUID,
        permission: Permission
    ): Boolean = playerPerms[uuid]?.remove(permission) == true

    fun addOverride(
        permission: Permission,
        override: Boolean
    ): Boolean {
        if (ruleOverrides[permission] == override) return false
        return ruleOverrides.put(permission, override) == null
    }

    fun removeOverride(
        permission: Permission
    ): Boolean = ruleOverrides.remove(permission) != null
}

class PermissionSerializer : KSerializer<Permission> {

    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: Permission
    ) { encoder.encodeString(value.name) }

    override fun deserialize(decoder: Decoder): Permission {
        return Permission.valueOf(decoder.decodeString()) ?: error("Invalid permission")
    }
}

class PosRangeSerializer : KSerializer<PosRange<Location, BlockPos>> {

    override val descriptor = buildClassSerialDescriptor("PosRange") {
        element<BlockPos>("firstPos")
        element<BlockPos>("secondPos")
        element<String>("world")
    }

    override fun serialize(
        encoder: Encoder,
        value: PosRange<Location, BlockPos>
    ) = encoder.encodeStructure(descriptor) {
        encodeSerializableElement(descriptor, 0, VectorSerializer, Vector(value.first.x, value.first.y, value.first.z))
        encodeSerializableElement(descriptor, 1, VectorSerializer, Vector(value.last.x, value.last.y, value.last.z))
    }

    override fun deserialize(decoder: Decoder): PosRange<Location, BlockPos> {
        var firstPos: Vector? = null
        var secondPos: Vector? = null
        var world: String? = null
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    0 -> firstPos = decodeSerializableElement(descriptor, i, VectorSerializer)
                    1 -> secondPos = decodeSerializableElement(descriptor, i, VectorSerializer)
                    2 -> world = decodeStringElement(descriptor, i)
                    CompositeDecoder.DECODE_DONE -> break
                }
            }
        }
        val firstBlockPos = BlockPos(firstPos!!.x.toInt(), firstPos!!.y.toInt(), firstPos!!.z.toInt())
        val secondBlockPos = BlockPos(secondPos!!.x.toInt(), secondPos!!.y.toInt(), secondPos!!.z.toInt())
        val bukkitWorld = worlds.firstOrNull { it.name == world } ?: error("Invalid world")

        return PosRange(firstBlockPos, secondBlockPos) {
            RangeIteratorWithFactor<Location, BlockPos>(
                firstBlockPos.asBukkitLocation(bukkitWorld),
                secondBlockPos.asBukkitLocation(bukkitWorld),
                { it.asBukkitLocation(bukkitWorld) },
                { it.asBlockPos() }
            )
        }
    }
}
