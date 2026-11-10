package com.oam.config

import com.google.gson.GsonBuilder
import com.oam.OAM
import net.fabricmc.loader.api.FabricLoader
import java.io.File

class OAMSettings {
    var fastLeap = false
    var leapRotationFix = false
    var brLeap = false
    var p1Leap = false
    var p2Leap = false
    var p3Leap = false
    var p4Leap = false
    var p5Leap = false

    var p1Target = "bers"
    var pillarTarget = "tank"
    var ypadTarget = "arch"
    var ppadTarget = "arch"
    var ppillarTarget = "heal"
    var pdTarget = "arch"
    var gpadTarget = "bers"
    var p2Target = "heal"
    var s1Target = "mage"
    var s2Target = "heal"
    var s3Target = "mage"
    var s4Target = "mage"
    var dropTarget = "heal"
    var midTarget = "heal"
    var p5Target = "heal"

    var brAuto = false
    var brUseSpiritLeap = false
    var p3Auto = false
    var p5Auto = false
    var p5AutoDelay = 0
    var stormCrushAuto = false
    var stormCrushTarget = ""
    var bersLeapoutAuto = false
    var bersLeapoutTarget = ""
}

object OAMConfig {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file: File get() = FabricLoader.getInstance().configDir.resolve("oam.json").toFile()

    var settings = OAMSettings()
        private set

    fun load() {
        try {
            if (file.exists()) settings = gson.fromJson(file.readText(), OAMSettings::class.java) ?: OAMSettings()
        } catch (e: Exception) {
            OAM.logger.error("Failed to load OAM config", e)
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
