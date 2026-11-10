package com.oam.gui

import com.mojang.blaze3d.platform.InputConstants
import com.oam.config.OAMConfig
import com.oam.config.OAMSettings
import com.oam.utils.Dungeon
import com.oam.utils.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import kotlin.reflect.KMutableProperty1

class OAMScreen : Screen(Component.literal("OAM")) {

    private sealed class Row(val label: String)
    private class Header(label: String) : Row(label)
    private class Toggle(label: String, val prop: KMutableProperty1<OAMSettings, Boolean>) : Row(label)
    private class Text(label: String, val prop: KMutableProperty1<OAMSettings, String>) : Row(label)
    private class Number(label: String, val prop: KMutableProperty1<OAMSettings, Int>, val min: Int, val max: Int, val step: Int, val suffix: String = "") : Row(label)

    private val rows: List<Row> = listOf(
        Header("General"),
        Toggle("Fast Leap", OAMSettings::fastLeap),
        Toggle("Leap Rotation Fix", OAMSettings::leapRotationFix),
        Toggle("Br Leap", OAMSettings::brLeap),
        Toggle("Maxor Leap", OAMSettings::p1Leap),
        Toggle("Storm Leap", OAMSettings::p2Leap),
        Toggle("P3 Leap", OAMSettings::p3Leap),
        Toggle("Necron Leap", OAMSettings::p4Leap),
        Toggle("P5 Leap", OAMSettings::p5Leap),
        Header("Targets (class or IGN)"),
        Text("P1: Maxor", OAMSettings::p1Target),
        Text("P2: Pillar", OAMSettings::pillarTarget),
        Text("P2: Yellow Pad", OAMSettings::ypadTarget),
        Text("P2: Purple Pad", OAMSettings::ppadTarget),
        Text("P2: Purple Pillar", OAMSettings::ppillarTarget),
        Text("P2: Predev", OAMSettings::pdTarget),
        Text("P2: P3 Leap (healer)", OAMSettings::gpadTarget),
        Text("P3: SS Leap", OAMSettings::p2Target),
        Text("P3: S1", OAMSettings::s1Target),
        Text("P3: S2", OAMSettings::s2Target),
        Text("P3: S3", OAMSettings::s3Target),
        Text("P3: S4", OAMSettings::s4Target),
        Text("P4: Necron Leap", OAMSettings::dropTarget),
        Text("P4: P5 Leap", OAMSettings::midTarget),
        Text("P5: Relic", OAMSettings::p5Target),
        Header("Auto Leap"),
        Toggle("Br Auto Leap (door opener)", OAMSettings::brAuto),
        Toggle("  Use Spirit Leap When Required", OAMSettings::brUseSpiritLeap),
        Toggle("P3 Section Leaps (S1-S4 targets)", OAMSettings::p3Auto),
        Toggle("Relic Leap (P5 Relic target)", OAMSettings::p5Auto),
        Number("Relic Leap Delay", OAMSettings::p5AutoDelay, 0, 500, 50, "ms"),
        Toggle("Storm Crush Leap", OAMSettings::stormCrushAuto),
        Text("Storm Crush Target", OAMSettings::stormCrushTarget),
        Toggle("Bers Leapout (4th device)", OAMSettings::bersLeapoutAuto),
        Text("Bers Leapout Target", OAMSettings::bersLeapoutTarget),
    )

    private val rowHeight = 16
    private val panelWidth = 260
    private val top = 40
    private var scroll = 0

    private var editing: Text? = null
    private var buffer = ""

    private val panelX get() = width / 2 - panelWidth / 2
    private val visibleRows get() = ((height - top - 30) / rowHeight).coerceAtLeast(1)
    private val maxScroll get() = (rows.size - visibleRows).coerceAtLeast(0)

    private val settings get() = OAMConfig.settings

    override fun init() {
        super.init()
        mc.textInputManager().startTextInput(this)
    }

    override fun removed() {
        mc.textInputManager().stopTextInput(this)
        commit()
        OAMConfig.save()
        super.removed()
    }

    override fun isPauseScreen() = false

    private fun rowAt(mouseX: Double, mouseY: Double): Row? {
        if (mouseX < panelX || mouseX > panelX + panelWidth || mouseY < top) return null
        val index = ((mouseY - top) / rowHeight).toInt()
        if (index >= visibleRows) return null
        return rows.getOrNull(index + scroll)
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0xA0000000.toInt())

        val title = "OAM"
        context.text(mc.font, title, width / 2 - mc.font.width(title) / 2, 15, 0xFF55FFFF.toInt())

        val info = "WIP a better gui will be made later or sm shit idk"
        context.text(mc.font, info, width / 2 - mc.font.width(info) / 2, 26, 0xFFAAAAAA.toInt())

        val hovered = rowAt(mouseX.toDouble(), mouseY.toDouble())

        for (i in 0 until visibleRows) {
            val row = rows.getOrNull(i + scroll) ?: break
            val y = top + i * rowHeight
            val textY = y + (rowHeight - mc.font.lineHeight) / 2 + 1

            if (row is Header) {
                context.text(mc.font, row.label, panelX, textY, 0xFFFFAA00.toInt())
                continue
            }

            context.fill(panelX, y, panelX + panelWidth, y + rowHeight - 2,
                if (row === hovered) 0x80404060.toInt() else 0x80202030.toInt())
            context.text(mc.font, row.label, panelX + 4, textY, 0xFFFFFFFF.toInt())

            val valueRight = panelX + panelWidth - 4
            when (row) {
                is Toggle -> {
                    val on = row.prop.get(settings)
                    val s = if (on) "ON" else "OFF"
                    context.text(mc.font, s, valueRight - mc.font.width(s), textY,
                        if (on) 0xFF55FF55.toInt() else 0xFFFF5555.toInt())
                }
                is Text -> {
                    val boxX = panelX + panelWidth - 100
                    val isEditing = editing === row
                    context.fill(boxX, y + 1, valueRight + 2, y + rowHeight - 3,
                        if (isEditing) 0xFF303050.toInt() else 0xFF101018.toInt())
                    val shown = if (isEditing) buffer + if ((System.currentTimeMillis() / 500) % 2 == 0L) "_" else "" else row.prop.get(settings)
                    context.text(mc.font, mc.font.plainSubstrByWidth(shown, 92), boxX + 3, textY,
                        if (isEditing) 0xFFFFFFFF.toInt() else 0xFFCCCCCC.toInt())
                }
                is Number -> {
                    val s = "< ${row.prop.get(settings)}${row.suffix} >"
                    context.text(mc.font, s, valueRight - mc.font.width(s), textY, 0xFFFFFF55.toInt())
                }
                is Header -> {}
            }
        }

        val hint = ""
        context.text(mc.font, hint, width / 2 - mc.font.width(hint) / 2, height - 14, 0xFF777777.toInt())
    }

    override fun mouseClicked(event: MouseButtonEvent, bl: Boolean): Boolean {
        val row = rowAt(event.x, event.y)
        if (row !== editing) commit()

        when (row) {
            is Toggle -> {
                row.prop.set(settings, !row.prop.get(settings))
                OAMConfig.save()
                return true
            }
            is Text -> {
                if (editing !== row) {
                    editing = row
                    buffer = row.prop.get(settings)
                }
                return true
            }
            is Number -> {
                val dir = if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) -1 else 1
                adjust(row, dir * row.step)
                return true
            }
            else -> {}
        }
        return super.mouseClicked(event, bl)
    }

    private fun adjust(row: Number, delta: Int) {
        row.prop.set(settings, (row.prop.get(settings) + delta).coerceIn(row.min, row.max))
        OAMConfig.save()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val row = rowAt(mouseX, mouseY)
        if (row is Number) {
            adjust(row, if (verticalAmount > 0) row.step else -row.step)
            return true
        }
        scroll = (scroll - verticalAmount.toInt()).coerceIn(0, maxScroll)
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (editing != null) {
            when (event.key) {
                InputConstants.KEY_ESCAPE -> { editing = null; buffer = "" }
                InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> commit()
                InputConstants.KEY_BACKSPACE -> buffer = buffer.dropLast(1)
            }
            return true
        }
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (editing != null && event.isAllowedChatCharacter) {
            if (buffer.length < 32) buffer += event.codepointAsString()
            return true
        }
        return super.charTyped(event)
    }

    private fun commit() {
        val row = editing ?: return
        row.prop.set(settings, buffer.trim())
        editing = null
        buffer = ""
        OAMConfig.save()
    }
}
