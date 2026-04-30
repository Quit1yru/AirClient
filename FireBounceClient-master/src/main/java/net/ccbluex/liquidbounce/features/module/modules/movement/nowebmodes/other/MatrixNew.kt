package net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.NoWeb.inWebTicks
import net.ccbluex.liquidbounce.features.module.modules.movement.NoWeb.matrixKeepY
import net.ccbluex.liquidbounce.features.module.modules.movement.NoWeb.matrixNoWebThreshold
import net.ccbluex.liquidbounce.features.module.modules.movement.NoWeb.matrixShowThresholdTicks
import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.NoWebMode
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.reduceXZ
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.reduceY
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object MatrixNew : NoWebMode("MatrixNew") {
    override fun onUpdate() {
        if (!mc.thePlayer.isInWeb || !mc.thePlayer.isMoving) {
            inWebTicks = 0
            if (!mc.gameSettings.keyBindJump.isKeyDown && !mc.gameSettings.keyBindSneak.isKeyDown && matrixKeepY && mc.thePlayer.isInWeb) reduceY(0.0)
            return
        }

        if (mc.thePlayer.isMoving && mc.thePlayer.isInWeb) inWebTicks++
        if (matrixShowThresholdTicks) chat("$inWebTicks")
        if (inWebTicks > matrixNoWebThreshold && mc.thePlayer.isMoving) {
            mc.thePlayer.jump()
            reduceXZ(1.1)
            mc.thePlayer.jumpMovementFactor = 0.26f
        } else if (mc.thePlayer.isMoving) {
            mc.thePlayer.jump()
            reduceXZ(1.1)
        }
        if (!mc.gameSettings.keyBindJump.isKeyDown && !mc.gameSettings.keyBindSneak.isKeyDown && matrixKeepY && mc.thePlayer.isInWeb) reduceY(0.0)
    }
}