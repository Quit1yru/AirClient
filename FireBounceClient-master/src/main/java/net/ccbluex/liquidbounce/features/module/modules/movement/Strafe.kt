package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.toDegreesF
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed
import kotlin.math.cos
import kotlin.math.sin

object Strafe : Module("Strafe", Category.MOVEMENT, gameDetecting = false) {

    private val mode by choices("Mode", arrayOf("Vanilla","Matrix"), "Vanilla")
    private val strength by float("Strength", 0.5F, 0F..1F) {mode == "Vanilla"}
    private val noMoveStop by boolean("NoMoveStop", false) {mode == "Vanilla"}
    private val onGroundStrafe by boolean("OnGroundStrafe", false) {mode == "Vanilla"}
    private val allDirectionsJump by boolean("AllDirectionsJump", false)

    private var wasDown = false
    private var jump = false


    val onJump = handler<JumpEvent> { event ->
        if (jump) {
            event.cancelEvent()
        }
    }

    override fun onEnable() {
        wasDown = false
    }

    val onUpdate = handler<UpdateEvent> {
        if (mc.thePlayer.onGround && mc.gameSettings.keyBindJump.isKeyDown && allDirectionsJump && mc.thePlayer.isMoving && !(mc.thePlayer.isInLiquid || mc.thePlayer.isOnLadder || mc.thePlayer.isInWeb)) {
            if (mc.gameSettings.keyBindJump.isKeyDown) {
                mc.gameSettings.keyBindJump.pressed = false
                wasDown = true
            }
            val yaw = mc.thePlayer.rotationYaw
            mc.thePlayer.rotationYaw = direction.toDegreesF()
            mc.thePlayer.tryJump()
            mc.thePlayer.rotationYaw = yaw
            jump = true
            if (wasDown) {
                mc.gameSettings.keyBindJump.pressed = true
                wasDown = false
            }
        } else {
            jump = false
        }
    }

    val onStrafe = handler<StrafeEvent> {
        when (mode) {
            "Vanilla" -> handleLiquidBounceStrafe()
            "Matrix" -> handleMatrixStrafe()
        }
    }

    private fun handleLiquidBounceStrafe() {
        if (!mc.thePlayer.isMoving) {
            if (noMoveStop) {
                mc.thePlayer.motionX = .0
                mc.thePlayer.motionZ = .0
            }
            return
        }

        val shotSpeed = speed
        val speed = shotSpeed * strength
        val motionX = mc.thePlayer.motionX * (1 - strength)
        val motionZ = mc.thePlayer.motionZ * (1 - strength)

        if (!mc.thePlayer.onGround || onGroundStrafe) {
            val yaw = direction
            mc.thePlayer.motionX = -sin(yaw) * speed + motionX
            mc.thePlayer.motionZ = cos(yaw) * speed + motionZ
        }
    }

    private fun handleMatrixStrafe() {
        if (!mc.thePlayer.isMoving) {
            return
        }

        val currentSpeed = speed
        if (currentSpeed <= 0.0) {
            return
        }

        var yaw = mc.thePlayer.rotationYaw
        if (mc.thePlayer.moveForward < 0.0F) {
            yaw += 180.0F
        } else {
            val forwardMultiplier = if (mc.thePlayer.moveForward < 0.0F) {
                -0.5F
            } else if (mc.thePlayer.moveForward > 0.0F) {
                0.5F
            } else {
                1.0F
            }

            if (mc.thePlayer.moveStrafing > 0.0F) {
                yaw -= 90.0F * forwardMultiplier
            }

            if (mc.thePlayer.moveStrafing < 0.0F) {
                yaw += 90.0F * forwardMultiplier
            }
        }

        val direction = Math.toRadians(yaw.toDouble())
        mc.thePlayer.motionX = -sin(direction) * currentSpeed
        mc.thePlayer.motionZ = cos(direction) * currentSpeed
    }

    override val tag: String?
        get() = mode
}