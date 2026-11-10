package com.oam

import com.oam.features.AutoLeap
import com.oam.features.FastLeap
import com.oam.features.LeapHelper
import com.oam.features.LeapRotationFix
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import com.oam.utils.Dungeon
import com.oam.utils.mc
import com.oam.utils.removeFormatting
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket

object OAMEvents {

    @JvmStatic
    fun isGuiOpen(): Boolean =
        mc.player?.hasContainerOpen() == true || mc.gui.screen() != null

    @JvmStatic
    fun onMouseButton(button: Int, action: Int): Boolean =
        FastLeap.onMouseButton(button, action)

    @JvmStatic
    fun onPacketReceive(packet: Packet<*>): Boolean {
        if (packet is ClientboundSystemChatPacket && !packet.overlay) {
            val message = packet.content.string.removeFormatting()
            Dungeon.onChat(message)
            LeapHelper.onChat(message)
            AutoLeap.onChat(message)
            LeapRotationFix.onChat(message)
        }
        if (packet is ClientboundMapItemDataPacket) LeapRotationFix.onMapData(packet)
        AutoLeap.onPacketReceive(packet)
        if (packet is ClientboundPlayerInfoUpdatePacket) {
            val lines = packet.entries().mapNotNull { it.displayName?.string }
            if (lines.isNotEmpty()) Dungeon.onTabUpdate(lines)
        }
        return LeapHelper.onPacketReceive(packet)
    }

    @JvmStatic
    fun onPacketSend(packet: Packet<*>) {
        LeapHelper.onPacketSend(packet)
    }
}
