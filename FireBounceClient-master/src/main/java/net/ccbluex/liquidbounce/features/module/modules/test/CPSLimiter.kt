package net.ccbluex.liquidbounce.features.module.modules.test

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.CPSCounter
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.play.client.C02PacketUseEntity

object CPSLimiter : Module("CPSLimiter", Category.TEST) {
    private val maxCPS by int("MaxCPS",20,1..100)
    private val alert by boolean("AlertWhenExceededLimit",false)


    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is C02PacketUseEntity && event.packet.action == C02PacketUseEntity.Action.ATTACK) {
            val cps = CPSCounter.getCPS(CPSCounter.MouseButton.LEFT)
            if (cps > maxCPS) {
                event.cancelEvent()
                if (alert) chat("CPS exceeds the limit, attack packet has been cancelled (§c$cps§f/$maxCPS)")
            }
        }
    }
}