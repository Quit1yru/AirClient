package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.matrix

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.fallDamageEnabled
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.inVoidCheck
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.legitTimer
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.timered
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.minecraft.network.play.client.C03PacketPlayer

object MatrixSpoof : NoFallMode("MatrixSpoof") {

    override fun onEnable() {
        timered = false
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (fallDamageEnabled && inVoidCheck) {
            if (packet is C03PacketPlayer && mc.thePlayer.isMoving) {
                event.cancelEvent()

                // 发送两个位置数据包
                sendPacket(
                    C03PacketPlayer.C04PacketPlayerPosition(
                        packet.x,
                        packet.y,
                        packet.z,
                        true
                    ),
                    false
                )
                sendPacket(
                    C03PacketPlayer.C04PacketPlayerPosition(
                        packet.x,
                        packet.y,
                        packet.z,
                        false
                    ),
                    false
                )

                // 重置摔落距离
                mc.thePlayer?.fallDistance = 0f

                // 计时器减速
                if (legitTimer) {
                    timered = true
                    mc.timer.timerSpeed = 0.2f
                }
            }
        } else if (timered) {
            // 恢复计时器
            mc.timer.timerSpeed = 1f
            timered = false
        }
    }

}