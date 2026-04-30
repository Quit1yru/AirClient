package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.matrix

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.minecraft.network.play.client.C03PacketPlayer

object Matrix62xPacket : NoFallMode("Matrix6.2.X-Packet") {
    override fun onUpdate() {
        if (mc.thePlayer.onGround) {
            mc.timer.timerSpeed = 1f
        } else if (mc.thePlayer.fallDistance - mc.thePlayer.motionY > 3f) {
            mc.timer.timerSpeed =
                (mc.timer.timerSpeed * if (mc.timer.timerSpeed < 0.6) 0.25f else 0.5f).coerceAtLeast(0.2f)
            sendPackets(
                C03PacketPlayer(false),
                C03PacketPlayer(false),
                C03PacketPlayer(false),
                C03PacketPlayer(false),
                C03PacketPlayer(false),
                C03PacketPlayer(true),
                C03PacketPlayer(false),
                C03PacketPlayer(false)
            )
            mc.thePlayer.fallDistance = 0f
        }
    }
}
