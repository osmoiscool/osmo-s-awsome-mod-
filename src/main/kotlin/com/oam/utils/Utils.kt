package com.oam.utils

import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

inline val mc: Minecraft get() = Minecraft.getInstance()

const val PREFIX = "§1[§9OAM§1]§r"

fun modMessage(msg: Any) {
    val player = mc.player ?: return
    val text = Component.literal("$PREFIX " + msg.toString().replace("&", "§"))
    mc.execute { player.sendSystemMessage(text) }
}

fun String.removeFormatting(): String = replace("[&§].".toRegex(), "")

fun String.removeUnicode(): String = replace("[^\\u0000-\\u007F]".toRegex(), "")

fun ItemStack.skyblockId(): String? {
    val tag = get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return null
    tag.getStringOr("id", "").takeIf { it.isNotBlank() }?.let { return it }
    return tag.getCompoundOrEmpty("ExtraAttributes").getStringOr("id", "").takeIf { it.isNotBlank() }
}

fun heldSkyblockId(): String? = mc.player?.mainHandItem?.skyblockId()

fun tabLines(): List<String> =
    mc.connection?.listedOnlinePlayers
        ?.mapNotNull { it.tabListDisplayName?.string?.removeFormatting() }
        ?: emptyList()

fun isPlayerInBox(x1: Double, x2: Double, y1: Double, y2: Double, z1: Double, z2: Double): Boolean {
    val p = mc.player ?: return false
    return p.x in minOf(x1, x2)..maxOf(x1, x2) &&
            p.y in minOf(y1, y2)..maxOf(y1, y2) &&
            p.z in minOf(z1, z2)..maxOf(z1, z2)
}
