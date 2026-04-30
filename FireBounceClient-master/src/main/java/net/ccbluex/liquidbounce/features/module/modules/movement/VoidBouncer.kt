package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object VoidBouncer : Module("VoidBouncer", Category.MOVEMENT) {
    private val bounceFactor by float("BounceFactor",1.0f,0.0f..100.0f)
    private var bounced = false

    val onUpdate = handler<UpdateEvent> {
        if (!mc.thePlayer.onGround &&
            mc.thePlayer.posY < -64 &&
            mc.thePlayer.hurtTime != 0 &&
            !bounced
            ) {
            mc.thePlayer.motionY *= -bounceFactor
            bounced = true
        }
        if (mc.thePlayer.onGround || mc.thePlayer.posY >= -64) bounced = false
    }
}