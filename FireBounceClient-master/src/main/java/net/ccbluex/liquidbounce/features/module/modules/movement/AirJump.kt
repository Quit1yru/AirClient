package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object AirJump : Module("AirJump", Category.MOVEMENT) {
    private val mode by choices("Mode",arrayOf("VanillaJump","Motion"),"VanillaJump")
    private val cooldown by int("CoolDown",5,0..20)



    private var cooldownCounter = 0
    val onUpdate = handler<UpdateEvent> {
        if (mc.gameSettings.keyBindJump.isKeyDown || mc.gameSettings.keyBindJump.isPressed) {
            if (cooldownCounter != 0) return@handler
            when (mode) {
                "VanillaJump" -> mc.thePlayer.jump()
                "Motion" -> mc.thePlayer.motionY = 0.42
            }
            cooldownCounter = cooldown
        }
        if (cooldownCounter != 0) cooldownCounter--
    }
}