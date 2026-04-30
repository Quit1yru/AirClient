package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.matrix

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.safe
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.movement.FallingPlayer
import net.minecraft.network.play.client.C03PacketPlayer
import kotlin.math.abs

object Matrix6632 : NoFallMode("Matrix6.6.3-2") {

    private var send = false
    private var nearGround = false
    private var wasWorking = false

    override fun onEnable() {
        send = false
        nearGround = false
        wasWorking = false
    }

    override fun onDisable() {
        if (wasWorking) {
            mc.timer.timerSpeed = 1f
        }
    }

    override fun onUpdate() {
        val isWorking = !(mc.thePlayer.fallDistance - mc.thePlayer.motionY <= 3 &&
                (absCollYMinusYPos >= 3 || mc.thePlayer.fallDistance - mc.thePlayer.motionY <= 2))

        if (isWorking) {

            mc.thePlayer.fallDistance = 0f
            send = true

            if (safe) {
                mc.timer.timerSpeed = 0.3f
                mc.thePlayer.motionX *= 0.5
                mc.thePlayer.motionZ *= 0.5
            } else {
                mc.timer.timerSpeed = 0.5f
            }
        } else if (wasWorking) {
            mc.timer.timerSpeed = 1f
        }

        // 更新状态记录
        wasWorking = isWorking
    }

    override fun onPacket(event: PacketEvent) {
        if (event.packet is C03PacketPlayer && send) {
            send = false
            if (absCollYMinusYPos > 2) {
                event.cancelEvent()
                sendPacket(
                    C03PacketPlayer.C04PacketPlayerPosition(
                        event.packet.x,
                        event.packet.y,
                        event.packet.z,
                        true
                    )
                )
                sendPacket(
                    C03PacketPlayer.C04PacketPlayerPosition(
                        event.packet.x,
                        event.packet.y,
                        event.packet.z,
                        false
                    )
                )
            }
        }
    }

    private val absCollYMinusYPos
        get() = abs((FallingPlayer(mc.thePlayer).findCollision(60)?.pos?.y ?: 0) - mc.thePlayer.posY)
}