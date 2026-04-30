/*
 * FireBounce Hacked Client
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.grim

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.network.play.client.C03PacketPlayer
import kotlin.math.sqrt

object OldGrim : NoFallMode("OldGrim") {

    private val timer = TickTimer()
    private var shouldCancel = false
    private var waitingForGround = false
    private var fallState = 0

    override fun onEnable() {
        timer.reset()
        shouldCancel = false
        waitingForGround = false
        fallState = 0
    }

    override fun onDisable() {
        shouldCancel = false
        waitingForGround = false
        fallState = 0
    }

    override fun onMotion(event: MotionEvent) {
        val player = mc.thePlayer ?: return

        // Reset state when on ground
        if (player.onGround) {
            if (fallState > 0) {
                fallState = 0
                shouldCancel = false
                waitingForGround = false
            }
            return
        }

        // Activate when falling distance is sufficient
        if (player.fallDistance >= 3.0f && fallState == 0) {
            fallState = 1
            shouldCancel = true
            waitingForGround = true

            // Apply motion reduction
            player.motionX *= 0.2
            player.motionZ *= 0.2
        }

        // Advanced ground approach detection
        if (fallState == 1 && player.fallDistance >= 4.0f) {
            val speed = sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ)

            // Speed control when close to ground
            if (isCloseToGround(2.0) && speed > 0.19) {
                player.motionX = player.motionX / speed * 0.19
                player.motionZ = player.motionZ / speed * 0.19
            }

            // Force ground state when conditions met
            if (isCloseToGround(1.0) && speed < 0.2) {
                event.onGround = true
                player.fallDistance = 0.0f
                fallState = 2
            }
        }
    }

    override fun onPacket(event: PacketEvent) {
        val player = mc.thePlayer ?: return
        val packet = event.packet

        if (packet is C03PacketPlayer && shouldCancel) {
            when (fallState) {
                1 -> {
                    // Cancel movement packets while waiting for ground
                    if (!player.onGround) {
                        event.cancelEvent()
                    } else {
                        // Send ground confirmation packet
                        event.cancelEvent()
                        sendPacket(C03PacketPlayer.C04PacketPlayerPosition(
                            packet.x, packet.y, packet.z, true
                        ))
                        shouldCancel = false
                        waitingForGround = false
                    }
                }
                2 -> {
                    // Ensure ground state is maintained
                    if (packet is C03PacketPlayer.C04PacketPlayerPosition ||
                        packet is C03PacketPlayer.C06PacketPlayerPosLook) {
                        event.cancelEvent()
                        sendPacket(C03PacketPlayer.C04PacketPlayerPosition(
                            packet.x, packet.y, packet.z, true
                        ))
                    }
                }
            }
        }
    }

    /**
     * Check if player is close to ground
     */
    private fun isCloseToGround(distance: Double): Boolean {
        val player = mc.thePlayer ?: return false

        for (i in 0 until distance.toInt() + 2) {
            val pos = net.minecraft.util.BlockPos(
                player.posX,
                player.posY - i,
                player.posZ
            )
            val block = mc.theWorld.getBlockState(pos).block
            if (block !is net.minecraft.block.BlockAir) {
                return true
            }
        }
        return false
    }
}