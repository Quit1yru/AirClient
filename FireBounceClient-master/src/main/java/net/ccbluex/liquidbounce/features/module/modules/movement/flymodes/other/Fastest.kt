/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vanilla

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.fastestMode
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.fastestMotionA
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.fastestMotionB
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.fastestMotionY
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extra.MovementUtils
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.math.FastMathUtil
import net.ccbluex.liquidbounce.utils.pathfinder.CustomPathHelper
import net.minecraft.network.play.client.C03PacketPlayer

object Fastest : FlyMode("Fastest") {
    var curSpeed = 0f
    override fun onMotion(event: MotionEvent) {
        val thePlayer = mc.thePlayer ?: return
        thePlayer.motionX=0.0
        thePlayer.motionY=0.0
        thePlayer.motionZ=0.0
        when(fastestMode){
            "Switch" -> curSpeed = if(thePlayer.ticksExisted%2==0) fastestMotionA else fastestMotionB
            "Random" -> curSpeed = nextFloat(fastestMotionA, fastestMotionB)
        }

        thePlayer.onGround = false
        thePlayer.isInWeb = false

        thePlayer.capabilities.isFlying = false

        var ySpeed = 0.0

        if (mc.gameSettings.keyBindJump.isKeyDown)
            ySpeed += fastestMotionY

        if (mc.gameSettings.keyBindSneak.isKeyDown)
            ySpeed -= fastestMotionY

        val radYaw = FastMathUtil.toAngleRadian(thePlayer.rotationYaw)
        val sinYaw = FastMathUtil.sin(radYaw).toDouble()
        val cosYaw = FastMathUtil.cos(radYaw).toDouble()
        if(!MovementUtils.isMoving()){
            curSpeed = 0f
        }

        val newPos = thePlayer.positionVector.addVector(-sinYaw * curSpeed, ySpeed, cosYaw * curSpeed)
        if(newPos!=thePlayer.positionVector){
            val interpolation = CustomPathHelper.findTeleportPathPointToPoint(thePlayer.posX,thePlayer.posY,thePlayer.posZ,newPos.xCoord, newPos.yCoord, newPos.zCoord, 8.5, 1500, 4)
            if (interpolation.isNotEmpty()) {
                interpolation.forEach { sendPacket(C03PacketPlayer.C04PacketPlayerPosition(it.xCoord, it.yCoord, it.zCoord, true)) }
                mc.thePlayer.setPosition(newPos.xCoord, newPos.yCoord, newPos.zCoord)
            }
        }
    }
}
