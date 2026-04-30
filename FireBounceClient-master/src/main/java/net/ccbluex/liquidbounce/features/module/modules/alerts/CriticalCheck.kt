package net.ccbluex.liquidbounce.features.module.modules.alerts

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.S0BPacketAnimation
import net.minecraft.network.play.server.S12PacketEntityVelocity

object CriticalCheck : Module("CriticalCheck", Category.ALERTS) {
    private val PacketCheck by boolean("PacketCheck-Test",true)
    private val OwnCheck by boolean("OwnCheck",true)
    private val OwnCheckDelay by int("OwnCheckDelay",4,1..20,"*0.05s")

    private var OwnChecked = false
    private var S12PacketCheck = false
    private var S0BPacketCheck = false
    private var lastAttackTime = 0L
    private var lastTarget: EntityLivingBase? = null
    val onAttack = handler<AttackEvent> { event ->
        val target = mc.objectMouseOver?.entityHit
        target?.let {
            if (it.hurtResistantTime >= 9 &&
                !mc.thePlayer.onGround &&
                mc.thePlayer.motionY <= 0 &&
                OwnCheck &&
                lastAttackTime <= 0
                ) {
                chat("§cCriticalChecked | Target:${target.name}")
                OwnChecked = true
            }
        }
    }
    val onUpdate = handler<UpdateEvent> { event ->
        if (S0BPacketCheck || S12PacketCheck && PacketCheck) {
            val target = mc.objectMouseOver?.entityHit
            target?.let { chat("PacketCheck-CriticalChecked | Target:${it.name}") }
        }
        if (S0BPacketCheck) S0BPacketCheck = false
        if (S12PacketCheck) S12PacketCheck = false
        if (OwnChecked) {
            OwnChecked = false
            lastAttackTime = OwnCheckDelay.toLong()
        }
        if (lastAttackTime > 0) {
            lastAttackTime--
        }
    }
    val onPacket = handler<PacketEvent> { event ->
        if (PacketCheck) {
            val onPacket = handler<PacketEvent> { event ->
                val packet = event.packet
                when (packet) {
                    is S12PacketEntityVelocity -> {
                        val entityId = packet.entityID
                        lastTarget?.takeIf { it.entityId == entityId }?.let { target ->
                            if (target.hurtTime > 0) {
                                S12PacketCheck = true
                            }
                        }
                    }
                    is S0BPacketAnimation -> {
                        if (packet.animationType == 4) {
                            val entityId = packet.entityID
                            lastTarget?.takeIf { it.entityId == entityId }?.let {
                                S0BPacketCheck = true
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDisable() {
        S0BPacketCheck = false
        S12PacketCheck = false
    }

    override fun onEnable() {
        S0BPacketCheck = false
        S12PacketCheck = false
    }
}