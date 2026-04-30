package net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.NoWeb.testDisableInWebState
import net.ccbluex.liquidbounce.features.module.modules.movement.NoWeb.testJumpMovementFactor
import net.ccbluex.liquidbounce.features.module.modules.movement.NoWeb.testModeAlwaysJump
import net.ccbluex.liquidbounce.features.module.modules.movement.NoWeb.testMotionXZFactor
import net.ccbluex.liquidbounce.features.module.modules.movement.NoWeb.testMotionYFactor
import net.ccbluex.liquidbounce.features.module.modules.movement.NoWeb.testOnlyWhenForwarding
import net.ccbluex.liquidbounce.features.module.modules.movement.NoWeb.testSetJumpMovementFactor
import net.ccbluex.liquidbounce.features.module.modules.movement.NoWeb.testSetMotionXZ
import net.ccbluex.liquidbounce.features.module.modules.movement.NoWeb.testSetMotionY
import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.NoWebMode
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.reduceXZ
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.reduceY

object TestMode : NoWebMode("TestMode") {
    override fun onUpdate() {
        if (mc.thePlayer.isInWeb) {
            if (testOnlyWhenForwarding && !mc.gameSettings.keyBindForward.isKeyDown) return
            if (testDisableInWebState) mc.thePlayer.isInWeb = false
            if (testModeAlwaysJump) mc.thePlayer.jump()
            if (testSetMotionXZ) reduceXZ(testMotionXZFactor.toDouble())
            if (testSetMotionY) reduceY(testMotionYFactor.toDouble())
            if (testSetJumpMovementFactor) mc.thePlayer.jumpMovementFactor = testJumpMovementFactor
        }
    }
}