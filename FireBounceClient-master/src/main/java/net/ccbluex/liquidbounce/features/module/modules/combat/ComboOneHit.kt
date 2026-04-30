package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.play.client.C02PacketUseEntity

object ComboOneHit : Module("ComboOneHit", Category.COMBAT) {
    val attackPackets by int("AttackPackets",50,1..1000)
    val onAttack = handler<AttackEvent> {
        val target = mc.objectMouseOver?.entityHit
        repeat(attackPackets) {
            sendPacket(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))
            mc.thePlayer.swingItem()
        }
    }
}