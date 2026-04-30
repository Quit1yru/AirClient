/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vulcan

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

object VulcanBoost : FlyMode("VulcanBoost") {
    override fun onMove(event: MoveEvent){
        if (mc.thePlayer.ticksExisted< 20F){
            mc.thePlayer.motionY = 0.7
            strafe(1.24F-mc.thePlayer.ticksExisted/10F, true, event)
            mc.timer.timerSpeed = 0.3F
        }else mc.timer.timerSpeed=1.0F
    }
}
