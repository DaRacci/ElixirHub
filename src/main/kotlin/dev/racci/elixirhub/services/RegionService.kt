package dev.racci.elixirhub.services

import dev.racci.elixirhub.ElixirHub
import dev.racci.elixirhub.Region
import dev.racci.minix.api.builders.ItemBuilderDSL
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.extensions.worlds
import dev.racci.minix.api.utils.minecraft.BlockPos
import dev.racci.minix.api.utils.minecraft.PosRange
import dev.racci.minix.api.utils.minecraft.RangeIteratorWithFactor
import dev.racci.minix.api.utils.minecraft.asBlockPos
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import java.io.File
import java.util.UUID

//@MappedExtension(ElixirHub::class, "Region Service")
class RegionService(override val plugin: ElixirHub) : Extension<ElixirHub>() {
    val selects: MutableMap<UUID, Triple<String, BlockPos?, BlockPos?>> = mutableMapOf()
    val regions: MutableSet<Region> = mutableSetOf()

    fun createRegion(
        name: String,
        world: String,
        posRange: PosRange<Location, BlockPos>
    ): Result<Region> {
        var result: Result<Region>? = null

        for (region in regions) {
            if (region.name == name) {
                log.debug { "Region with name $name already exists" }
                result = Result.failure(RegionAlreadyExistsException("Region with name '$name' already exists"))
                break
            }

            // TODO: Find smaller region
            for (pos in region.posRange) {
                log.debug { "Checking if $pos is in ${posRange.first}-${posRange.last}" }
                if (posRange.contains(pos)) {
                    log.debug { "Region $pos is in ${posRange.first}-${posRange.last}" }
                    result = Result.failure(RegionOverlapException("Region with name ${region.name} overlaps with $posRange."))
                    break
                }
            }
        }

        if (result != null) return result

        val region = Region(name, world, posRange, hashMapOf(), mutableMapOf())
        regions += region

        return Result.success(region)
    }

    fun getRegion(name: String): Result<Region> {
        var result: Result<Region>? = null

        for (region in regions) {
            if (region.name != name) continue

            result = Result.success(region)
            break
        }

        return result ?: Result.failure(RegionNotFoundException("Region with name '$name' not found."))
    }

    fun getRegionFromPos(pos: BlockPos, world: String): Result<Region> {
        var result: Result<Region>? = null

        for (region in regions) {
            if (region.world != world) continue
            if (!region.posRange.contains(pos)) continue

            result = Result.success(region)
            break
        }

        return result ?: Result.failure(RegionNotFoundException("Region with pos $pos not found."))
    }

    fun getRegions(world: String? = null): PersistentSet<Region> {
        if (world == null) return regions.toPersistentSet()
        return regions.filter { it.world == world }.toPersistentSet()
    }

    fun setPosition(
        uuid: UUID,
        location: Location,
        pos: Short
    ): Boolean {
        if (location.world == null) return false
        if (selects[uuid] != null && selects[uuid]!!.first != location.world.name) selects.remove(uuid)

        val old = selects[uuid]
        val newPos = BlockPos(location.blockX, location.blockY, location.blockZ)
        val newTriple = Triple(
            location.world.name,
            if (pos == 0.toShort()) old?.second else newPos,
            if (pos == 1.toShort()) old?.third else newPos
        )

        selects[uuid] = newTriple
        return true
    }

    fun getRange(uuid: UUID): Result<PosRange<Location, BlockPos>> {
        val (world, pos1, pos2) = selects[uuid] ?: return Result.failure(RegionNotSelectedException("Region not selected."))
        val bukkitWorld = worlds.find { it.name == world }

        if (bukkitWorld == null || pos1 == null || pos2 == null) return Result.failure(RegionNotSelectedException("Region not selected."))

        val range = PosRange(pos1, pos2) {
            RangeIteratorWithFactor<Location, BlockPos>(
                pos1.asBukkitLocation(bukkitWorld),
                pos2.asBukkitLocation(bukkitWorld),
                { it.asBukkitLocation(bukkitWorld) },
                { it.asBlockPos() }
            )
        }
        return Result.success(range)
    }

    override suspend fun handleEnable() {
        loadStorage()
    }

    override suspend fun handleUnload() {
        saveStorage()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun loadStorage() {
        val folder = plugin.dataFolder
        val file = folder.resolve("regions.json")

        if (!canUse(file)) return

        file.inputStream().use {
            try {
                regions.addAll(Json.decodeFromStream<Array<Region>>(it))
            } catch (e: Exception) {
                log.error(e) { "Failed to load regions from file $file" }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun saveStorage() {
        val folder = plugin.dataFolder
        val file = folder.resolve("regions.json")

        if (!canUse(file)) return

        file.outputStream().use {
            try {
                Json.encodeToStream(regions.toTypedArray(), it)
            } catch (e: Exception) {
                log.error(e) { "Failed to save regions to file $file" }
            }
        }
    }

    private fun canUse(file: File): Boolean {
        if (!file.parentFile.exists() && !file.parentFile.mkdirs()) {
            log.error { "Failed to create directory ${file.parentFile}" }
            return false
        }

        if (!file.exists()) {
            if (!file.createNewFile()) log.error { "Failed to create file $file" }
            return false
        }

        return true
    }

    class RegionAlreadyExistsException(message: String) : Exception(message)
    class RegionOverlapException(message: String) : Exception(message)
    class RegionNotFoundException(reason: String) : Exception(reason)
    class RegionNotSelectedException(reason: String) : Exception(reason)

    companion object : Extension.ExtensionCompanion<RegionService>() {
        val NBT_KEY = NamespacedKey("elixirhub", "region_selector")
        val SELECTOR by lazy {
            ItemBuilderDSL.from(Material.STICK) {
                name = "<green>Region tool".parse()
                lore = listOf("<green>Left click to set position 1".parse(), "<green>Right click to set position 2".parse())
                glowing = true
                pdc {
                    set(NBT_KEY, PersistentDataType.BYTE, 1.toByte())
                }
            }
        }
    }
}
