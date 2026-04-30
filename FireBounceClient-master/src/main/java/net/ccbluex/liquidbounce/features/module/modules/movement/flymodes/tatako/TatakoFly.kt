package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.tatako

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.horizontalSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.verticalSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.x
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.y
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.z
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.reduceXZ
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.reduceY
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C03PacketPlayer

object TatakoFly : FlyMode("TatakoFly") {

    override fun onEnable() {
        x = mc.thePlayer.posX
        y = mc.thePlayer.posY
        z = mc.thePlayer.posZ
    }

    override fun onDisable() {
        val player: EntityPlayer = mc.thePlayer
        PacketUtils.sendPacket(
            C03PacketPlayer.C06PacketPlayerPosLook(
                player.posX + 0.05,
                player.posY,
                player.posZ,
                player.rotationYaw,
                player.rotationPitch,
                true
            ),false
        )
        PacketUtils.sendPacket(
            C03PacketPlayer.C06PacketPlayerPosLook(
                player.posX,
                player.posY + 0.42,
                player.posZ,
                player.rotationYaw,
                player.rotationPitch,
                true
            ),false
        )
        PacketUtils.sendPacket(
            C03PacketPlayer.C06PacketPlayerPosLook(
                player.posX,
                player.posY + 0.7532,
                player.posZ,
                player.rotationYaw,
                player.rotationPitch,
                true
            ),false
        )
        PacketUtils.sendPacket(
            C03PacketPlayer.C06PacketPlayerPosLook(
                player.posX,
                player.posY + 1.0,
                player.posZ,
                player.rotationYaw,
                player.rotationPitch,
                true
            ),false
        )
    }

    override fun onUpdate() {


        MovementUtils.setSpeed(horizontalSpeed.toDouble(), true)

        if (GameSettings.isKeyDown(mc.gameSettings.keyBindJump)) {
            mc.thePlayer.motionY = verticalSpeed.toDouble()
        }
        if (GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            mc.thePlayer.motionY = -verticalSpeed.toDouble()
        }
    }

    fun stop(y: Boolean) {
        reduceXZ(0.0)
        if (y) reduceY(0.0)
    }

    override fun onPacket(event: PacketEvent) {
        if (event.packet is C03PacketPlayer) {
            y -= 0.09
            event.packet.x = x
            event.packet.y = y
            event.packet.z = z
            event.packet.onGround = true
        }
        if (event.packet is C03PacketPlayer.C06PacketPlayerPosLook) {
            event.packet.x = x
            event.packet.y = y
            event.packet.z = z
            PacketUtils.sendPacket(
                C03PacketPlayer.C06PacketPlayerPosLook(
                    x,
                    y,
                    z,
                    event.packet.yaw,
                    event.packet.pitch,
                    false
                ),false
            )
            event.cancelEvent()
        }
    }
}