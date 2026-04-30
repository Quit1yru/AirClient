/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.client.settings.GameSettings

object AutoWalk : Module("AutoWalk", Category.MOVEMENT, subjective = true, gameDetecting = false) {

    private val Forward by boolean("Forward",true)
    private val Backward by boolean("Backward",false)
    private val Left by boolean("Left",false)
    private val Right by boolean("Right",false)
    private val AutoDisable by boolean("AutoDisable",false)
    private val DisableTime by int("DisableTime",1000,0..100000,"ms") {AutoDisable}
    var DisableTimer = MSTimer()
    val onUpdate = handler<UpdateEvent> {
        if (Forward) {
            mc.gameSettings.keyBindForward.pressed = true
            if (!Backward) {
                mc.gameSettings.keyBindBack.pressed = false
            }
            if (!Left) {
                mc.gameSettings.keyBindLeft.pressed = false
            }
            if (!Right) {
                mc.gameSettings.keyBindRight.pressed = false
            }
        }
        if (Backward) {
            mc.gameSettings.keyBindBack.pressed = true
            if (Forward) {
                mc.gameSettings.keyBindForward.pressed = false
            }
            if (!Left) {
                mc.gameSettings.keyBindLeft.pressed = false
            }
            if (!Right) {
                mc.gameSettings.keyBindRight.pressed = false
            }
        }
        if (Left) {
            mc.gameSettings.keyBindLeft.pressed = true
            if (Forward) {
                mc.gameSettings.keyBindForward.pressed = false
            }
            if (!Backward) {
                mc.gameSettings.keyBindBack.pressed = false
            }
            if (!Right) {
                mc.gameSettings.keyBindRight.pressed = false
            }
        }
        if (Right) {
            mc.gameSettings.keyBindRight.pressed = true
            if (Forward) {
                mc.gameSettings.keyBindForward.pressed = false
            }
            if (!Backward) {
                mc.gameSettings.keyBindBack.pressed = false
            }
            if (!Left) {
                mc.gameSettings.keyBindLeft.pressed = false
            }
        }
        if (AutoDisable) {
            if (DisableTimer.hasTimePassed(DisableTime)) {
                DisableTimer.reset()
                AutoWalk.state = false
            }
        }
    }

    override fun onDisable() {
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindForward)) {
            mc.gameSettings.keyBindForward.pressed = false
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindBack)) {
            mc.gameSettings.keyBindBack.pressed = false
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) {
            mc.gameSettings.keyBindLeft.pressed = false
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight)) {
            mc.gameSettings.keyBindRight.pressed = false
        }
        DisableTimer.reset()
    }

    override fun onEnable() {
        DisableTimer.reset()
    }
}
