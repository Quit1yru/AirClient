package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.runAttack
import net.ccbluex.liquidbounce.utils.timing.MSTimer

object MoreClick : Module("MoreClick", Category.COMBAT) {
    val ExtraPacket by int("ExtraClickPacket",1,1..20)
    private val keepSprint by boolean("KeepSprint",false)
    val SendPacketDelay by int("SendPacketDelay",50,0..1000,"ms")
    val Debugger by boolean("Debugger",false)

    var PacketDelay = MSTimer()

    val onAttack = handler<AttackEvent> { event ->
        if (PacketDelay.hasTimePassed(SendPacketDelay.toLong())) {
            PacketDelay.reset()
            runAttack(
                keepSprint,
                3.0f,
                ExtraPacket,
                silentAttack = true,
                debugMessage = Debugger)
        }
    }

    override fun onEnable() {
        PacketDelay.reset()
    }

    override fun onDisable() {
        PacketDelay.reset()
    }
}