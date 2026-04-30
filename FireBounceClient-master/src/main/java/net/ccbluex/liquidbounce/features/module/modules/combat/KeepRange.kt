package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.Vec3_ZERO
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.ccbluex.liquidbounce.utils.extensions.times
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.Entity
import net.minecraft.util.Vec3

object KeepRange : Module("KeepRange", Category.COMBAT) {

    val delayBlock by int("DelayBlock", 100, 100..10000)
    val delayRelease by int("DelayRelease", 100, 100..10000)
    val maxRange by float("KeepRange", 3f, 1f..8f)
    val searchRange by float("SearchRange", 7f, 1f..8f)
    val fov by float("FOV", 180f, 1f..180f)
    var blockMovement = false
    val timer = MSTimer()
    var target: Entity? = null
    override fun onEnable() {
        blockMovement=false
        target=null
    }
    val onWorld = handler<WorldEvent> {
        blockMovement=false
        target=null
    }
    fun getFinalVec(): Vec3{
        val player = mc.thePlayer // 这里可以断言mc.thePlayer非null所以不用判断
        var motion = Vec3(player.motionX, player.motionY, player.motionZ)
        var position = Vec3(player.posX, player.posY, player.posZ)
        while (motion.distanceTo(Vec3_ZERO) > 0.0625) { // 1px精度已经足够，循环次数可能导致卡顿!!!
            position+=motion
            motion *= 0.98
            if(motion.xCoord<=0.0005f) motion.xCoord=0.0
            if(motion.yCoord<=0.0005f) motion.yCoord=0.0
            if(motion.zCoord<=0.0005f) motion.zCoord=0.0
        }
        return position
    }
    val onMotion = handler<MotionEvent>{
        if (target == null||mc.thePlayer==null) return@handler
        val playerFinalVec = getFinalVec()
        val dist = target!!.getDistance(playerFinalVec.xCoord, playerFinalVec.yCoord, playerFinalVec.zCoord)
        if (dist < maxRange) {
            if(mc.thePlayer.canEntityBeSeen(target!!)&&rotationDifference(target!!) <= fov){
                if (blockMovement) {
                    mc.gameSettings.keyBindForward.pressed = false
                    mc.gameSettings.keyBindBack.pressed = false
                    mc.gameSettings.keyBindLeft.pressed = false
                    mc.gameSettings.keyBindRight.pressed = false
                    if (timer.hasTimePassed(delayBlock)) {
                        blockMovement = false
                    }
                } else {
                    mc.gameSettings.keyBindBack.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindBack)
                    mc.gameSettings.keyBindForward.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
                    mc.gameSettings.keyBindLeft.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)
                    mc.gameSettings.keyBindRight.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindRight)

                    if (timer.hasTimePassed(delayRelease)) {
                        blockMovement = true
                    }
                }
            }
        }
        if(dist > searchRange){
            target=null
            mc.gameSettings.keyBindBack.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindBack)
            mc.gameSettings.keyBindForward.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
            mc.gameSettings.keyBindLeft.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)
            mc.gameSettings.keyBindRight.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindRight)
        }
    }
}