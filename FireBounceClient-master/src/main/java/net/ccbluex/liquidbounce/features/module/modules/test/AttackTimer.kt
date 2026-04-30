package net.ccbluex.liquidbounce.features.module.modules.test

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.changeTimer

object AttackTimer : Module("AttackTimer", Category.TEST) {
    private val timer by float("Timer",1.02f,0.1f..2.0f,"x")
    private val durationTime by int("DurationTime",1,1..5,"Ticks")

    private var changedTicks = 0
    val onUpdate = handler<UpdateEvent> {
        if (changedTicks > 0) {
            changeTimer(timer)
            changedTicks--
        } else if (mc.timer.timerSpeed == timer) {
            mc.timer.timerSpeed = 1f
            return@handler
        }
    }
    val onAttack = handler<AttackEvent> {
        changedTicks = durationTime
    }

    override fun onEnable() {
        changedTicks = 0
    }
}