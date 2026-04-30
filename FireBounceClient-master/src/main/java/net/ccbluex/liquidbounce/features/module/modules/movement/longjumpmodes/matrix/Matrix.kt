package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.matrix

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object Matrix : LongJumpMode("Matrix") {

    override fun onEnable() {
        val module = LongJump

        module.matrixBoosted = false
        module.matrixCanBoost = false
        module.matrixReceivedFlag = false
        module.matrixTouchGround = false

        if (module.matrixBypassMethod == "NoGround") {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.tryJump()
            }
            module.matrixTouchGround = true
        }
    }

    override fun onPacket(event: PacketEvent){
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook) {
            LongJump.matrixReceivedFlag = true
        }
    }

    override fun onMotion(event: MotionEvent) {
        if (LongJump.matrixBypassMethod == "NoGround" && !LongJump.matrixCanBoost) {
            event.onGround = false
        }
    }

    override fun onUpdate() {
        val module = LongJump

        if (!mc.thePlayer.onGround && module.matrixTouchGround) {
            module.matrixTouchGround = false
        }

        if (mc.thePlayer.onGround && !module.matrixTouchGround) {
            // 触发跳跃
            if (mc.thePlayer.isMoving) {
                mc.thePlayer.tryJump()
            }
            module.matrixBoosted = false
            if (module.matrixBypassMethod == "NoGround" && !module.matrixBoosted) {
                module.matrixCanBoost = true
            }
        }

        if (mc.thePlayer.fallDistance >= 0.25f && !module.matrixBoosted && module.matrixBypassMethod == "Fall") {
            module.matrixCanBoost = true
        }

        if (module.matrixCanBoost) {
            MovementUtils.setSpeed(module.matrixBoostSpeed.toDouble(),false)
            mc.thePlayer.motionY = 0.42
            module.matrixBoosted = true
        }

        if (module.matrixReceivedFlag && module.matrixBoosted) {
            // 自动禁用
            if (module.autoDisable) {
                module.state = false
            }
            module.matrixCanBoost = false
            module.matrixReceivedFlag = false
        }
    }
}