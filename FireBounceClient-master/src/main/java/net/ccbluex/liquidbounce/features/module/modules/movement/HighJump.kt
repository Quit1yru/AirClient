/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.minecraft.block.BlockPane
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.util.BlockPos

object HighJump : Module("HighJump", Category.MOVEMENT) {
    private val mode = choices("Mode", arrayOf("Vanilla", "FairFight0.6.0", "Damage", "AACv3", "DAC", "Mineplex", "Matrix"), "Vanilla")
    private val height by float("Height", 2f, 1.1f..5f) { mode.get() in arrayOf("Vanilla", "Damage") }

    private val matrixMotionY by float("Matrix-MotionY", 0.998f, 0.42f..2f) { mode.get().equals("Matrix", true) }
    private val matrixTicks by int("Matrix-Ticks", 4, 1..20) { mode.get().equals("Matrix", true) }

    private val glass by boolean("OnlyGlassPane", false)

    // Matrix variables
    private var active = false
    private var falling = false
    private var moving = false
    private var ticksSinceJump = 0

    override fun onEnable() {
        if (mode.get().equals("Matrix", ignoreCase = true)) {
            ticksSinceJump = 0
            falling = false
            active = false
            moving = mc.thePlayer?.isMoving == true
        }
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        if (glass && BlockPos(thePlayer).block !is BlockPane)
            return@handler

        when (mode.get().lowercase()) {
            "damage" -> if (thePlayer.hurtTime > 0 && thePlayer.onGround) thePlayer.motionY += 0.42f * height
            "aacv3" -> if (!thePlayer.onGround) thePlayer.motionY += 0.059
            "dac" -> if (!thePlayer.onGround) thePlayer.motionY += 0.049999
            "mineplex" -> if (!thePlayer.onGround) MovementUtils.strafe(0.35f)
            "matrix" -> {
                if (!moving) {
                    MovementUtils.strafe(0.16f)
                    moving = true
                }

                if (thePlayer.isCollidedVertically) {
                    active = true
                }

                if (ticksSinceJump == 1) {
                    thePlayer.motionY = matrixMotionY.toDouble()
                }

                if (thePlayer.isCollidedVertically && ticksSinceJump > matrixTicks) {
                    state = false
                }

                if (!thePlayer.onGround && ticksSinceJump >= 2) {
                    thePlayer.motionY += 0.0034999
                    if (!falling && thePlayer.motionY < 0.0 && thePlayer.motionY > -0.05) {
                        thePlayer.motionY = 0.0029999
                        falling = true
                        state = false
                    }
                }

                if (active) {
                    ticksSinceJump++
                }
            }
        }
    }

    val onMove = handler<MoveEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        if (glass && BlockPos(thePlayer).block !is BlockPane)
            return@handler
        if (!thePlayer.onGround) {
            if ("mineplex" == mode.get().lowercase()) {
                thePlayer.motionY += if (thePlayer.fallDistance == 0f) 0.0499 else 0.05
            }
            if ("fairfight0.6.0" == mode.get().lowercase()) {
                if(mc.thePlayer.isInWater && BlockUtils.getBlock(thePlayer.position.add(-0.5, 1.5, -0.5)) == Blocks.water && thePlayer.fallDistance >= 2.0){
                    thePlayer.motionY = 1.9
                }
            }
        }
    }

    val onJump = handler<JumpEvent> { event ->
        val thePlayer = mc.thePlayer ?: return@handler

        if (glass && BlockPos(thePlayer).block !is BlockPane)
            return@handler
        when (mode.get().lowercase()) {
            "vanilla" -> event.motion *= height
            "mineplex" -> event.motion = 0.47f
        }
    }

    val onMotion = handler<MotionEvent> { event ->
        if (mode.get().equals("Matrix", ignoreCase = true)) {
            if (ticksSinceJump == 1) {
                event.onGround = false
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        if (mode.get().equals("Matrix", ignoreCase = true)) {
            val packet = event.packet
            if (packet is S12PacketEntityVelocity) {
                if (packet.entityID == mc.thePlayer.entityId && packet.motionY < -500) {
                    event.cancelEvent()
                }
            }
        }
    }

    override val tag
        get() = mode.get()
}
