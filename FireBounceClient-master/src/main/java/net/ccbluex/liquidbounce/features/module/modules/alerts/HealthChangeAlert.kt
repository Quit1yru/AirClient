package net.ccbluex.liquidbounce.features.module.modules.alerts

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import java.math.BigDecimal
import java.math.RoundingMode

object HealthChangeAlert : Module("HealthChangeAlert", Category.ALERTS) {
    val minAlertHealth by float("minAlertHealthChange",0f,0.0f..20.0f)

    var nowHealth = 0f
    var lastHealth = 0f
    var alertChange = 0f
    var maxHealth = 0f
    var alertChange2 = ""


    override fun onEnable() {
        val player = mc.thePlayer ?: return
        nowHealth = player.health
        lastHealth = player.health
        maxHealth = player.maxHealth
    }

    val onUpdate = handler<UpdateEvent> { event ->
        if (nowHealth != lastHealth) {

            alertChange = nowHealth - lastHealth

            lastHealth = nowHealth
            var nowHealth1 = BigDecimal(nowHealth.toString()).setScale(3, RoundingMode.DOWN).toFloat()
            var maxHealth1 = BigDecimal(maxHealth.toString()).setScale(3, RoundingMode.DOWN).toFloat()
            var alertChange1 = BigDecimal(alertChange.toString()).setScale(3, RoundingMode.DOWN).toFloat()
            if (alertChange1 >= minAlertHealth || alertChange1 <= -minAlertHealth) {
                alertChange2 = if (alertChange1 >= 0) {
                    "§a+$alertChange1"
                } else "§c$alertChange1"
                chat("Health:$nowHealth1/$maxHealth1 ($alertChange2§f)")
            }
            alertChange = 0f
        }
        maxHealth = mc.thePlayer.maxHealth
        nowHealth = mc.thePlayer.health
    }
    override val tag: String?
        get() = "$nowHealth/$maxHealth"
}