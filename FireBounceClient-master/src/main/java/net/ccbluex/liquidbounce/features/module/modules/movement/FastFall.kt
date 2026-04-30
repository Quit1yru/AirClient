package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.changeTimer
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.isFalling

object FastFall : Module("FastFall", Category.MOVEMENT) {
    private val boostMode by choices("BoostMode",arrayOf("Number","Factor","SetMotion"),"Factor")
    private val boostNumber by float("BoostNumber",1f,0.01f..10f) {boostMode == "Number"}
    private val boostFactor by float("BoostFactor",2f,1f..10f) {boostMode == "Factor"}
    private val setMotionY by float("SetMotionNumber",0.8f,0.01f..10f) {boostMode == "SetMotion"}
    private val changeTimer by boolean("ChangeTimer",false)
    private val timers by int("Times",1,1..2) { changeTimer }
    private val timer1Factor by float("Timer1Factor",0.5f,0.01f..2f) {timers >= 1 && changeTimer}
    private val timer1Ticks by int("Timer1Ticks",3,1..20) {timers >= 1 && changeTimer}
    private val timer2Factor by float("Timer2Factor",0.5f,0.01f..150f) {timers >= 2 && changeTimer}
    private val timer2Ticks by int("Timer2Ticks",3,1..20) {timers >= 2 && changeTimer}
    private val autoDisable by boolean("AutoDisable",false)
    private var boosted = false
    private var tick1Start = false
    private var timer1Tick = 0
    private var tick2Start = false
    private var timer2Tick = 0
    private var changingTimer = false
    val onUpdate = handler<UpdateEvent> { 
        if (mc.thePlayer.isFalling())  {
            when (boostMode) {
                "Number" -> {
                    if (!boosted) mc.thePlayer.motionY -= boostNumber
                }
                "Factor" -> {
                    if (!boosted) mc.thePlayer.motionY *= boostFactor
                }
                "SetMotion" -> {
                    if (!boosted) mc.thePlayer.motionY = -setMotionY.toDouble()
                }
            }
            boosted = true
        }
        if ((!mc.thePlayer.isFalling() || mc.thePlayer.onGround && boosted)) {
            if (autoDisable) state = false
            if (changeTimer) changeTimer(1f)
            tick1Start = false
            tick2Start = false
            timer1Tick = 0
            timer2Tick = 0
            changingTimer = false
        }
    }
    val onTick = handler<GameTickEvent> {
        if (boosted && changeTimer && !mc.thePlayer.onGround && !changingTimer) {
            changingTimer = true
        }
        if (boosted && changeTimer && changingTimer) {
            if (mc.thePlayer.onGround) {
                changeTimer(1f)
                return@handler
            }
            if (!tick1Start && !tick2Start) {
                if (timer1Tick > timer1Ticks && changingTimer && tick1Start) {
                    tick1Start = false
                    tick2Start = true
                    return@handler
                }
                tick1Start = true
                changeTimer(timer1Factor)
                timer1Tick++
            }
            if (!tick1Start && tick2Start) {
                if (timer2Tick > timer2Ticks && changingTimer && tick2Start) {
                    tick2Start = false
                    changingTimer = false
                    return@handler
                }
                changeTimer(timer2Factor)
                timer2Tick++
            }
        }
    }

    override fun onEnable() {
        boosted = false
        tick1Start = false
        tick2Start = false
        timer1Tick = 0
        timer2Tick = 0
        changingTimer = false
    }

    override fun onDisable() {
        if (changeTimer) changeTimer(1f)
    }
}