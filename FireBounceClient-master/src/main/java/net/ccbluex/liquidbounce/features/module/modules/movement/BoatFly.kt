package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import kotlin.math.cos
import kotlin.math.sin


object BoatFly : Module("BoatFly", Category.MOVEMENT) {
    private val modeValue = choices("Mode", arrayOf("Motion", "Clip", "Velocity"), "Motion")
    private val speedValue = float("Speed", 0.3f, 0.0f..1.0f)

    val onUpdate = handler<UpdateEvent> {
        if(!mc.thePlayer.isRiding) return@handler

        val vehicle = mc.thePlayer.ridingEntity
        val x = -sin(MovementUtils.direction) * speedValue.get()
        val z = cos(MovementUtils.direction) * speedValue.get()

        when (modeValue.get().lowercase()) {
            "motion" -> {
                vehicle.motionX = x
                vehicle.motionY = (if(mc.gameSettings.keyBindJump.pressed) speedValue.get() else 0).toDouble()
                vehicle.motionZ = z
            }

            "clip" -> {
                vehicle.setPosition(vehicle.posX + x , vehicle.posY + (if (mc.gameSettings.keyBindJump.pressed) speedValue.get() else 0).toDouble() , vehicle.posZ + z)
            }

            "velocity" -> {
                vehicle.addVelocity(x, if(mc.gameSettings.keyBindJump.pressed) speedValue.get().toDouble() else 0.0, z)
            }
        }
    }
}