/**
 * @author FireFly_Legit
 * For FireBounceClient
 */
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.MessageManager.sayMessage
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.EntityLivingBase

object AttackMessage : Module("AttackMessage", Category.FUN) {
    private val text by text("Message","L")
    private val cooldown by int("Cooldown",0,0..5000,"ms")
    private val onlyWhenHurt by boolean("OnlyWhenTargetHurt",true)
    private var timerHelper = MSTimer()

    private var target: EntityLivingBase? = null

    val onAttack = handler<AttackEvent> {
        if (it.targetEntity == null) return@handler
        target = it.targetEntity as EntityLivingBase
        if (shouldSend()) {
            sayMessage(text)
            timerHelper.reset()
        }
    }

    private fun shouldSend(): Boolean {
        if (text == "") return false
        return when {
            !timerHelper.hasTimePassed(cooldown) -> false
            onlyWhenHurt && (target?: return false).hurtTime != 9 -> false
            else -> true
        }
    }
}