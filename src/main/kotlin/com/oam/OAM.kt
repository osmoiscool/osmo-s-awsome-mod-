package com.oam

import com.oam.config.OAMConfig
import com.oam.features.AutoLeap
import com.oam.features.LeapHelper
import com.oam.features.LeapRotationFix
import com.oam.features.LeapTarget
import com.oam.features.SilentSwap
import com.oam.utils.Scheduler
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import com.oam.gui.OAMScreen
import com.oam.utils.ContainerData
import com.oam.utils.Dungeon
import com.oam.utils.mc
import com.oam.utils.modMessage
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import org.slf4j.LoggerFactory

object OAM : ClientModInitializer {

    const val MOD_ID = "oam"
    val logger = LoggerFactory.getLogger("OAM")

    override fun onInitializeClient() {
        OAMConfig.load()

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            Dungeon.onWorldChange()
            AutoLeap.onWorldChange()
            LeapTarget.onWorldChange()
            LeapRotationFix.onWorldChange()
            LeapHelper.reset()
            SilentSwap.reset()
            ContainerData.reset()
        }

        ClientTickEvents.START_CLIENT_TICK.register {
            if (mc.player == null) return@register
            Dungeon.onTick()
            Scheduler.onTickStart()
            SilentSwap.onTickStart()
            LeapHelper.onTickStart()
        }

        ClientTickEvents.END_CLIENT_TICK.register {
            LeapHelper.onTick()
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            for (name in listOf("oam", "osmo")) {
                dispatcher.register(
                    ClientCommands.literal(name)
                        .executes {
                            mc.execute { mc.gui.setScreen(OAMScreen()) }
                            1
                        }
                        .then(ClientCommands.literal("party").executes {
                            val party = Dungeon.party()
                            if (party.isEmpty()) modMessage("&7No party members parsed from tab list")
                            else party.forEach { (n, c) -> modMessage("&b$n &7- &f$c") }
                            1
                        })
                )
            }
        }

        logger.info("OAM loaded")
    }
}
