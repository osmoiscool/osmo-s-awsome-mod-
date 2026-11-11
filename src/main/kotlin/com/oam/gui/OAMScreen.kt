package com.oam.gui

import com.mojang.blaze3d.platform.InputConstants
import com.oam.config.FastLeapProfile
import com.oam.config.OAMConfig
import com.oam.config.OAMSettings
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
    private class Toggle(label: String, val get: () -> Boolean, val set: (Boolean) -> Unit) : Row(label)
    private class Text(label: String, val get: () -> String, val set: (String) -> Unit) : Row(label)
    private class Number(label: String, val get: () -> Int, val set: (Int) -> Unit, val min: Int, val max: Int, val step: Int, val suffix: String = "") : Row(label)
    private class Dropdown(label: String, val get: () -> String, val set: (String) -> Unit, val options: List<String>) : Row(label)
    private class Option(val dropdown: Dropdown, val value: String) : Row(value)

    private fun toggle(label: String, p: KMutableProperty1<FastLeapProfile, Boolean>) =
        Toggle(label, { p.get(settings.profile) }, { p.set(settings.profile, it) })
    private fun text(label: String, p: KMutableProperty1<FastLeapProfile, String>) =
        Text(label, { p.get(settings.profile) }, { p.set(settings.profile, it) })

    private fun sharedToggle(label: String, p: KMutableProperty1<OAMSettings, Boolean>) =
        Toggle(label, { p.get(settings) }, { p.set(settings, it) })
    private fun sharedText(label: String, p: KMutableProperty1<OAMSettings, String>) =
        Text(label, { p.get(settings) }, { p.set(settings, it) })
    private fun sharedNumber(label: String, p: KMutableProperty1<OAMSettings, Int>, min: Int, max: Int, step: Int, suffix: String = "") =
        Number(label, { p.get(settings) }, { p.set(settings, it) }, min, max, step, suffix)

    private enum class Tab(val label: String) { FAST("Fast Leap"), AUTO("Auto Leap") }

    private var tab = Tab.FAST

    private val classDropdown = Dropdown("Class", { settings.selectedClass }, { settings.selectedClass = it }, OAMConfig.CLASSES)
    private val classOptions = classDropdown.options.map { Option(classDropdown, it) }

    private val fastTop: List<Row> = listOf(
        Header("General"),
        toggle("Fast Leap", FastLeapProfile::fastLeap),
        Header("Phases"),
        toggle("Blood Rush", FastLeapProfile::brLeap),
        toggle("Maxor", FastLeapProfile::p1Leap),
        toggle("Storm", FastLeapProfile::p2Leap),
        toggle("Goldor", FastLeapProfile::p3Leap),
        toggle("Necron", FastLeapProfile::p4Leap),
        toggle("P5", FastLeapProfile::p5Leap),
        Header("Targets (class or IGN)"),
        text("P1: Maxor", FastLeapProfile::p1Target),
    )

    private val stormRows: List<Row> = listOf(text("P2: Storm", FastLeapProfile::p2StormTarget))
    private val mageStormRows: List<Row> = listOf(
        text("P2: After Lightning", FastLeapProfile::p2LightningTarget),
        text("P2: After Crush", FastLeapProfile::p2CrushTarget),
    )

    private val fastBottom: List<Row> = listOf(
        text("P3: SS Leap", FastLeapProfile::ssTarget),
        text("P3: S1", FastLeapProfile::s1Target),
        text("P3: S2", FastLeapProfile::s2Target),
        text("P3: S3", FastLeapProfile::s3Target),
        text("P3: S4", FastLeapProfile::s4Target),
        text("P4: Necron Leap", FastLeapProfile::dropTarget),
        text("P4: P5 Leap", FastLeapProfile::midTarget),
        text("P5: Relic", FastLeapProfile::p5Target),
        Header("All Classes"),
        sharedToggle("Leap Rotation Fix", OAMSettings::leapRotationFix),
    )

    private val autoRows: List<Row> = listOf(
        Header("Blood Rush"),
        sharedToggle("Door Opener Leap", OAMSettings::brAuto),
        sharedToggle("Use Spirit Leap When Required", OAMSettings::brUseSpiritLeap),
        Header("Storm"),
        sharedToggle("Crush Leap", OAMSettings::stormCrushAuto),
        sharedText("Crush Target", OAMSettings::stormCrushTarget),
        Header("Goldor"),
        sharedToggle("Section Leaps (S1-S4 targets)", OAMSettings::p3Auto),
        sharedToggle("Bers Leapout (4th device)", OAMSettings::bersLeapoutAuto),
        sharedText("Bers Leapout Target", OAMSettings::bersLeapoutTarget),
        Header("P5"),
        sharedToggle("Relic Leap (P5 Relic target)", OAMSettings::p5Auto),
        sharedNumber("Relic Leap Delay", OAMSettings::p5AutoDelay, 0, 500, 50, "ms"),
    )

    private var expanded: Dropdown? = null

    private val rows: List<Row>
        get() = when (tab) {
            Tab.FAST -> buildList {
                add(classDropdown)
                if (expanded === classDropdown) addAll(classOptions)
                addAll(fastTop)
                addAll(if (settings.selectedClass == "Mage") mageStormRows else stormRows)
                addAll(fastBottom)
            }
            Tab.AUTO -> autoRows
        }

    private val rowHeight = 16
    private val tabHeight = 18
    private val sidebarWidth = 80
    private val gap = 12
    private val panelWidth = 250
    private val totalWidth = sidebarWidth + gap + panelWidth
    private val top = 48
    private var scroll = 0

    private var editing: Text? = null
    private var buffer = ""

    private val leftX get() = width / 2 - totalWidth / 2
    private val panelX get() = leftX + sidebarWidth + gap
    private val visibleRows get() = ((height - top - 20) / rowHeight).coerceAtLeast(1)
    private val maxScroll get() = (rows.size - visibleRows).coerceAtLeast(0)

    private val settings get() = OAMConfig.settings

    private object Theme {
        const val BACKDROP = 0xC8070310.toInt()
        const val FRAME = 0xF0120916.toInt()
        const val TRIM = 0xFF8B1E3F.toInt()
        const val TRIM_DARK = 0xFF4A0F24.toInt()
        const val EAR_INNER = 0xFFE07AA8.toInt()
        const val ROW = 0xB01C1024.toInt()
        const val ROW_HOVER = 0xD02E1638.toInt()
        const val ACCENT = 0xFFE0457B.toInt()
        const val TEXT = 0xFFEDE0F5.toInt()
        const val MUTED = 0xFF9A86A8.toInt()
        const val HEADER = 0xFFD46A9F.toInt()
        const val LILAC = 0xFFB794F4.toInt()
        const val SWITCH_OFF = 0xFF3A2A44.toInt()
        const val KNOB = 0xFFF4E8FF.toInt()
        const val BOX = 0xFF0A0510.toInt()
        const val BOX_EDIT = 0xFF1E0F26.toInt()
    }

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

    private fun tabY(index: Int) = top + index * (tabHeight + 2)

    private fun tabAt(mouseX: Double, mouseY: Double): Tab? {
        if (mouseX < leftX || mouseX >= leftX + sidebarWidth) return null
        return Tab.entries.withIndex().firstOrNull { (i, _) -> mouseY >= tabY(i) && mouseY < tabY(i) + tabHeight }?.value
    }

    private fun rowAt(mouseX: Double, mouseY: Double): Row? {
        if (mouseX < panelX || mouseX > panelX + panelWidth || mouseY < top) return null
        val index = ((mouseY - top) / rowHeight).toInt()
        if (index >= visibleRows) return null
        return rows.getOrNull(index + scroll)
    }

    private fun outline(context: GuiGraphicsExtractor, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        context.fill(x1, y1, x2, y1 + 1, color)
        context.fill(x1, y2 - 1, x2, y2, color)
        context.fill(x1, y1, x1 + 1, y2, color)
        context.fill(x2 - 1, y1, x2, y2, color)
    }

    private fun ear(context: GuiGraphicsExtractor, centerX: Int, baseY: Int) {
        val h = 12
        for (i in 0 until h) {
            val y = baseY - h + i
            context.fill(centerX - i - 1, y, centerX + i + 1, y + 1, Theme.TRIM)
            if (i >= 4) context.fill(centerX - (i - 3), y, centerX + (i - 3), y + 1, Theme.EAR_INNER)
        }
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, Theme.BACKDROP)

        val frameX1 = leftX - 8
        val frameX2 = leftX + totalWidth + 8
        val frameY1 = top - 8
        val frameY2 = top + visibleRows * rowHeight + 6

        ear(context, frameX1 + 22, frameY1)
        ear(context, frameX2 - 22, frameY1)

        context.fill(frameX1, frameY1, frameX2, frameY2, Theme.FRAME)
        outline(context, frameX1, frameY1, frameX2, frameY2, Theme.TRIM)

        val title = "OAM"
        context.text(mc.font, title, width / 2 - mc.font.width(title) / 2, frameY1 - 12, Theme.ACCENT)

        val hoveredTab = tabAt(mouseX.toDouble(), mouseY.toDouble())
        for ((i, t) in Tab.entries.withIndex()) {
            val y = tabY(i)
            val selected = t == tab
            if (selected || t == hoveredTab) {
                context.fill(leftX, y, leftX + sidebarWidth, y + tabHeight, if (selected) Theme.ROW_HOVER else Theme.ROW)
            }
            if (selected) context.fill(leftX, y, leftX + 2, y + tabHeight, Theme.ACCENT)
            context.text(mc.font, t.label, leftX + 8, y + (tabHeight - mc.font.lineHeight) / 2 + 1,
                if (selected) Theme.TEXT else Theme.MUTED)
        }

        val dividerX = panelX - gap / 2
        context.fill(dividerX, top, dividerX + 1, frameY2 - 6, Theme.TRIM_DARK)

        val hovered = rowAt(mouseX.toDouble(), mouseY.toDouble())
        val blink = (System.currentTimeMillis() / 500) % 2 == 0L

        for (i in 0 until visibleRows) {
            val row = rows.getOrNull(i + scroll) ?: break
            val y = top + i * rowHeight
            val textY = y + (rowHeight - mc.font.lineHeight) / 2 + 1

            if (row is Header) {
                context.text(mc.font, row.label, panelX, textY, Theme.HEADER)
                val lineX = panelX + mc.font.width(row.label) + 6
                context.fill(lineX, textY + 3, panelX + panelWidth, textY + 4, Theme.TRIM_DARK)
                continue
            }

            val isHovered = row === hovered
            context.fill(panelX, y, panelX + panelWidth, y + rowHeight - 2, if (isHovered) Theme.ROW_HOVER else Theme.ROW)
            if (isHovered) context.fill(panelX, y, panelX + 2, y + rowHeight - 2, Theme.ACCENT)

            val labelX = if (row is Option) panelX + 16 else panelX + 6
            context.text(mc.font, row.label, labelX, textY, if (row is Option) Theme.LILAC else Theme.TEXT)

            val valueRight = panelX + panelWidth - 5
            when (row) {
                is Toggle -> {
                    val on = row.get()
                    val x1 = valueRight - 20
                    val y1 = y + 3
                    val y2 = y + rowHeight - 5
                    context.fill(x1, y1, valueRight, y2, if (on) Theme.ACCENT else Theme.SWITCH_OFF)
                    val knobX = if (on) valueRight - 8 else x1 + 1
                    context.fill(knobX, y1 + 1, knobX + 7, y2 - 1, Theme.KNOB)
                }
                is Text -> {
                    val boxX = panelX + panelWidth - 102
                    val isEditing = editing === row
                    context.fill(boxX, y + 1, valueRight + 1, y + rowHeight - 3, if (isEditing) Theme.BOX_EDIT else Theme.BOX)
                    outline(context, boxX, y + 1, valueRight + 1, y + rowHeight - 3, if (isEditing) Theme.ACCENT else Theme.TRIM_DARK)
                    val shown = if (isEditing) buffer + if (blink) "_" else "" else row.get()
                    context.text(mc.font, mc.font.plainSubstrByWidth(shown, 92), boxX + 4, textY,
                        if (isEditing) Theme.TEXT else Theme.LILAC)
                }
                is Number -> {
                    val s = "‹ ${row.get()}${row.suffix} ›"
                    context.text(mc.font, s, valueRight - mc.font.width(s), textY, Theme.LILAC)
                }
                is Dropdown -> {
                    val s = row.get() + if (expanded === row) " ▲" else " ▼"
                    context.text(mc.font, s, valueRight - mc.font.width(s), textY, Theme.HEADER)
                }
                is Option -> {
                    if (row.value == row.dropdown.get()) {
                        context.text(mc.font, "♥", valueRight - mc.font.width("♥"), textY, Theme.ACCENT)
                    }
                }
                is Header -> {}
            }
        }

        if (maxScroll > 0) {
            val trackX = frameX2 - 6
            val trackY1 = top
            val trackY2 = top + visibleRows * rowHeight - 2
            val trackH = trackY2 - trackY1
            val thumbH = (trackH * visibleRows / rows.size).coerceAtLeast(8)
            val thumbY = trackY1 + (trackH - thumbH) * scroll / maxScroll
            context.fill(trackX, trackY1, trackX + 2, trackY2, Theme.TRIM_DARK)
            context.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, Theme.ACCENT)
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, bl: Boolean): Boolean {
        tabAt(event.x, event.y)?.let { clicked ->
            commit()
            if (clicked != tab) {
                tab = clicked
                expanded = null
                scroll = 0
            }
            return true
        }

        val row = rowAt(event.x, event.y)
        if (row !== editing) commit()
        if (row !is Dropdown && row !is Option) collapse()

        when (row) {
            is Dropdown -> {
                if (expanded === row) collapse() else expanded = row
                return true
            }
            is Option -> {
                row.dropdown.set(row.value)
                OAMConfig.save()
                collapse()
                return true
            }
            is Toggle -> {
                row.set(!row.get())
                OAMConfig.save()
                return true
            }
            is Text -> {
                if (editing !== row) {
                    editing = row
                    buffer = row.get()
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

    private fun collapse() {
        expanded = null
        scroll = scroll.coerceIn(0, maxScroll)
    }

    private fun adjust(row: Number, delta: Int) {
        row.set((row.get() + delta).coerceIn(row.min, row.max))
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
        row.set(buffer.trim())
        editing = null
        buffer = ""
        OAMConfig.save()
    }
}
