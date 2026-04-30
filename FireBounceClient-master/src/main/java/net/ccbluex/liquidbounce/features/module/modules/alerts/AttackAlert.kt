package net.ccbluex.liquidbounce.features.module.modules.alerts

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.play.client.C02PacketUseEntity

object AttackAlert : Module("AttackAlert", Category.ALERTS) {
    private val mode by choices("DisplayMode",arrayOf(
        "EveryAttack",
        "TotalAttackCountInHurting",
        "WhenTotalAttackCountReachedLimit",
    ),"EveryAttack")

    private var attackCounter = 0
    private val alertLimit by int("AlertLimit",20,1..100) {mode == "WhenTotalAttackCountReachedLimit"}

    val onPacket = handler<PacketEvent> { event ->
        val p = event.packet
        if (p is C02PacketUseEntity && p.action == C02PacketUseEntity.Action.ATTACK) {
            when (mode) {
                "EveryAttack" -> chat("You Attacked Entity! | Target:${p.getEntityFromWorld(mc.theWorld)}")
                "TotalAttackCountInHurting" -> {
                    if (mc.thePlayer.hurtTime == 0) return@handler
                    attackCounter++
                }
                "WhenTotalAttackCountReachedLimit" -> {
                    if (attackCounter < alertLimit) {
                        attackCounter++
                    } else {
                        chat("You Attacked $alertLimit Times")
                        attackCounter = 0
                    }
                }
            }
        }
    }
    val onUpdate = handler<UpdateEvent> {
        when (mode) {
            "TotalAttackCountInHurting" -> {
                if (mc.thePlayer.hurtTime == 0 && attackCounter != 0) {
                    chat("You Attacked $attackCounter times in this hurt!")
                    attackCounter = 0
                }
            }
        }
    }
}