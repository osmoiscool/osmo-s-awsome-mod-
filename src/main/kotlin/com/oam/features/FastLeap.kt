package com.oam.features

import com.oam.config.OAMConfig
import com.oam.utils.heldSkyblockId

object FastLeap {

    private const val PRESS = 1
    private const val LEFT = 0
    private const val RIGHT = 1

    private val LEAP_ITEMS = setOf("INFINITE_SPIRIT_LEAP", "SPIRIT_LEAP")

    fun onMouseButton(button: Int, action: Int): Boolean {
        if (action != PRESS) return false
        if (heldSkyblockId() !in LEAP_ITEMS) return false

        if (button == RIGHT) {
            LeapHelper.target = null
            return false
        }

        if (!OAMConfig.settings.profile.fastLeap) return false
        if (button != LEFT) return false

        val leapTo = LeapTarget.getLeap() ?: return false

        LeapHelper.leap(leapTo)
        return true
    }
}
