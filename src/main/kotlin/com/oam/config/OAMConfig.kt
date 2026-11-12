package com.oam.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.oam.OAM
import net.fabricmc.loader.api.FabricLoader
import java.io.File

class FastLeapProfile {
    var fastLeap = false
    var brLeap = false
    var p1Leap = false
    var p2Leap = false
    var p3Leap = false
    var p4Leap = false
    var p5Leap = false

    var p1Target = "bers"
    var p2StormTarget = ""
    var p2InitialTarget = ""
    var p2LightningTarget = ""
    var p2CrushTarget = ""
    var ssTarget = "heal"
    var s1Target = "mage"
    var s2Target = "heal"
    var s3Target = "mage"
    var s4Target = "mage"
    var dropTarget = "heal"
    var midTarget = "heal"
    var p5Target = "heal"
    var p5GeneralTarget = ""
}

class OAMSettings {
    var selectedClass = "Archer"
    var classProfiles: MutableMap<String, FastLeapProfile> = LinkedHashMap()

    val profile: FastLeapProfile
        get() = classProfiles.getOrPut(selectedClass) { FastLeapProfile() }

    var leapRotationFix = false

    var brAuto = false
    var brUseSpiritLeap = false
    var crystalAuto = false
    var p3Auto = false
    var ssCompleteAuto = false
    var ssCompleteTarget = ""
    var p5Auto = false
    var p5AutoDelay = 0
    var stormCrushAuto = false
    var stormCrushTarget = ""
    var bersLeapoutAuto = false
    var bersLeapoutTarget = ""
}

object OAMConfig {
    val CLASSES = listOf("Archer", "Berserk", "Healer", "Mage", "Tank")

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file: File get() = FabricLoader.getInstance().configDir.resolve("oam.json").toFile()

    var settings = OAMSettings()
        private set

    fun load() {
        try {
            if (file.exists()) {
                val root = JsonParser.parseString(file.readText()).asJsonObject
                settings = gson.fromJson(root, OAMSettings::class.java) ?: OAMSettings()
                if (!root.has("classProfiles")) {
                    migrateFlatConfig(root)
                    save()
                }
            }
        } catch (e: Exception) {
            OAM.logger.error("Failed to load OAM config", e)
        }
        if (settings.selectedClass !in CLASSES) settings.selectedClass = CLASSES.first()
        for (clazz in CLASSES) settings.classProfiles.getOrPut(clazz) { FastLeapProfile() }
    }

    private fun migrateFlatConfig(root: JsonObject) {
        fun str(key: String): String? = root.get(key)?.takeIf { it.isJsonPrimitive }?.asString

        for (clazz in CLASSES) {
            val profile = gson.fromJson(root, FastLeapProfile::class.java) ?: FastLeapProfile()
            str("p2Target")?.let { profile.ssTarget = it }
            when (clazz) {
                "Archer" -> str("p2ArchTarget")
                "Berserk" -> str("p2BersTarget")
                "Healer" -> str("p2HealTarget")
                "Tank" -> str("p2TankTarget")
                else -> null
            }?.let { profile.p2StormTarget = it }
            if (clazz == "Mage") {
                str("p2MageLightningTarget")?.let { profile.p2LightningTarget = it }
                str("p2MageCrushTarget")?.let { profile.p2CrushTarget = it }
            }
            settings.classProfiles[clazz] = profile
        }
    }

    fun save() {
        try {
            file.parentFile.mkdirs()
            file.writeText(gson.toJson(settings))
        } catch (e: Exception) {
            OAM.logger.error("Failed to save OAM config", e)
        }
    }
}
