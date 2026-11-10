package com.oam.features

import com.oam.config.OAMConfig
import com.oam.utils.Dungeon
import com.oam.utils.mc
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.entity.Relative
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes
import net.minecraft.world.level.saveddata.maps.MapId
import java.util.concurrent.ConcurrentHashMap

object LeapRotationFix {

    @Volatile var yRot: Float? = null
    @Volatile var xRot: Float? = null

    private val mapYaw = ConcurrentHashMap<String, Float>()

    @Volatile private var mapId: MapId? = null

    @Volatile private var seenDungeonStart = false
    @Volatile private var dungeonEnded = false

    private const val DUNGEON_START = "[NPC] Mort: Here, I found this map when I first entered the dungeon."

    @JvmStatic
    fun click(containerId: Int, slotId: Int) {
        if (!OAMConfig.settings.leapRotationFix) return
        if (slotId !in 11..16) return

        val screen = mc.gui.screen() as? AbstractContainerScreen<*> ?: return
        if (screen.menu.containerId != containerId) return

        val title = screen.title.string
        if (title != "Spirit Leap" && title != "Teleport to Player") return

        capture(screen.menu.slots[slotId].item)
    }

    fun onHiddenClick(slotId: Int, item: ItemStack) {
        if (!OAMConfig.settings.leapRotationFix) return
        if (slotId !in 11..16) return
        val copy = item.copy()
        mc.execute { capture(copy) }
    }

    private fun capture(item: ItemStack) {
        val name = item.hoverName.string.substringAfter(' ')

        if (name !in Dungeon.party().keys) return

        val entity = mc.level?.players()?.firstOrNull { it.name.string == name }
        if (entity != null) {
            xRot = entity.xRot
            yRot = entity.yRot
        } else {
            xRot = 0f
            yRot = mapYaw[name] ?: 0f
        }
    }

    @JvmStatic
    fun handleNoRotate(packet: ClientboundPlayerPositionPacket) {
        val x = xRot ?: return
        val y = yRot ?: return
        xRot = null
        yRot = null

        val relativeRot = packet.relatives().contains(Relative.X_ROT) && packet.relatives().contains(Relative.Y_ROT)
        val noRotation = packet.change().xRot() == 0f && packet.change().yRot() == 0f
        if (!relativeRot || !noRotation) return

        val player = mc.player ?: return
        player.setXRot(x)
        player.setYRot(y)
    }

    fun onChat(message: String) {
        if (message == DUNGEON_START) seenDungeonStart = true
        val trimmed = message.trim()
        if (trimmed.startsWith("The Catacombs - ") || trimmed.startsWith("Master Mode The Catacombs - ")) dungeonEnded = true
    }

    fun onMapData(packet: ClientboundMapItemDataPacket) {
        if (dungeonEnded || !seenDungeonStart) return

        if (mapId == null) mapId = packet.mapId()
        if (mapId!!.id() != packet.mapId().id()) return

        val decorations = packet.decorations().orElse(null) ?: return
        val me = mc.player?.name?.string
        val teammates = Dungeon.party().entries.filter { it.key != me }.iterator()

        for (decoration in decorations) {
            if (decoration.type().value() == MapDecorationTypes.FRAME.value()) continue
            while (teammates.hasNext()) {
                val (name, clazz) = teammates.next()
                if (clazz == "DEAD") continue
                mapYaw[name] = decoration.rot() * 360 / 16f
                break
            }
        }
    }

    fun onWorldChange() {
        xRot = null
        yRot = null
        mapYaw.clear()
        mapId = null
        seenDungeonStart = false
        dungeonEnded = false
    }
}
