package net.ccbluex.liquidbounce.features.module.modules.alerts

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object TimerSpeedAlert : Module("TimerSpeedAlert", Category.ALERTS) {
    var realTimer = 0f
    val onUpdate = handler<UpdateEvent> {
        realTimer = mc.timer.timerSpeed
    }
    override val tag: String?
        get() = "$realTimer"
}