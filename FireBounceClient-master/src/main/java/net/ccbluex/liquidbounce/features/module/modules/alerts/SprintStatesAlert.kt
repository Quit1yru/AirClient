package net.ccbluex.liquidbounce.features.module.modules.alerts

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object SprintStatesAlert : Module("SprintStatesAlert", Category.ALERTS) {
    var cState = false
    var sState = false
    val onUpdate = handler<UpdateEvent> {
        sState = mc.thePlayer.serverSprintState
        cState = mc.thePlayer.isSprinting
    }
    override val tag: String?
        get() = if (sState == cState) {
            "$sState"
        } else "Server:$sState | Client:$cState"
}