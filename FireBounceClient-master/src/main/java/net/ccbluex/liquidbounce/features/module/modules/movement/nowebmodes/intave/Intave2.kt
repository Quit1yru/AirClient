package net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.NoWebMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils

object Intave2 : NoWebMode("Intave2") {

    override fun onUpdate() {
        if (!mc.thePlayer.isInWeb) return

        // 阻止跳跃输入
        mc.gameSettings.keyBindJump.pressed = false

        if (mc.thePlayer.isMoving && mc.thePlayer.isInWeb) {
            if (!mc.thePlayer.onGround) {
                // 空中移动策略
                mc.timer.timerSpeed = 1.0f
                if (mc.thePlayer.ticksExisted % 2 == 0) {
                    MovementUtils.strafe(0.65f)
                } else if (mc.thePlayer.ticksExisted % 5 == 0) {
                    MovementUtils.strafe(0.65f)
                }
            } else {
                // 地面移动策略
                MovementUtils.strafe(0.35f)
                mc.thePlayer.jump()
                mc.thePlayer.jump()
                mc.thePlayer.jump() // 连续跳跃脱离
            }

            // 非冲刺状态减速控制
            if (!mc.thePlayer.isSprinting) {
                mc.thePlayer.motionX *= 0.75
                mc.thePlayer.motionZ *= 0.75
            }
        }
    }
}