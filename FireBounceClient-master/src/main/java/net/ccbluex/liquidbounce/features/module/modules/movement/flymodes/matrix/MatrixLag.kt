package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.matrix


import net.ccbluex.liquidbounce.event.EventState.PRE
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.canJump
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.flag
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.flags
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.minFlags
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.zeroRot
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object MatrixLag : FlyMode("MatrixLagFly") {
    
    override fun onEnable() {
        canJump = false
        flag = false
        mc.timer.timerSpeed = 1f
    }

    override fun onUpdate() {
        if (canJump) {
            mc.timer.timerSpeed = 1f
            mc.thePlayer.motionY = 0.42
            canJump = false
        } else if (flag) {
            if (mc.thePlayer.motionY < -0.01) {
                mc.timer.timerSpeed = 1f
                flag = false
            }
        }
    }

    override fun onMotion(event: MotionEvent) {
        if (flag) return
        if (event.eventState == PRE) {
            PacketUtils.sendPacket(C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX + 16, mc.thePlayer.posY, mc.thePlayer.posZ + 16, false))
        }
    }

    override fun onPacket(event: PacketEvent) {
        if (event.packet is C03PacketPlayer) {
            if (!zeroRot.value) return
            if (mc.thePlayer.isMoving) {
                PacketUtils.sendPacket(
                    C03PacketPlayer.C06PacketPlayerPosLook(
                        event.packet.x, event.packet.y, event.packet.z,
                        0f, 0f,  // 强制归零旋转
                        event.packet.onGround
                    ),
                    false
                )
            } else {
                PacketUtils.sendPacket(
                    C03PacketPlayer.C06PacketPlayerPosLook(
                        mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                        0f, 0f,
                        event.packet.onGround
                    ),
                    false
                )
            }
            event.cancelEvent()
        }
        if (event.packet is S08PacketPlayerPosLook) {
            flags++
            if (flags >= minFlags.get()) {
                if (!canJump && !flag) {
                    canJump = true
                    flag = true
                }
            }
        }
    }
}