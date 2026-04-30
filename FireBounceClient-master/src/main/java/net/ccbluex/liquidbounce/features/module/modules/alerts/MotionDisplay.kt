package net.ccbluex.liquidbounce.features.module.modules.alerts

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.play.server.S12PacketEntityVelocity
import java.math.BigDecimal
import java.math.RoundingMode

object MotionDisplay : Module("MotionDisplay", Category.ALERTS) {
    val motionX by boolean("DisplayMotionX",true)
    val motionY by boolean("DisplayMotionY",true)
    val motionZ by boolean("DisplayMotionZ",true)
    val bigDecimalLevel by int("BigDecimalLevel",2,1..16)
    val alertKnockBack by boolean("AlertKnockBack",false)

    var realMotionX = 0.0
    var realMotionY = 0.0
    var realMotionZ = 0.0
    var DisplayX = 0.0
    var DisplayY = 0.0
    var DisplayZ = 0.0
    var alertX = 0.0
    var alertY = 0.0
    var alertZ = 0.0
    val onUpdate = handler<UpdateEvent> { event ->
        realMotionX = mc.thePlayer.motionX
        realMotionY = mc.thePlayer.motionY
        realMotionZ = mc.thePlayer.motionZ
        DisplayX = BigDecimal(realMotionX.toString()).setScale(bigDecimalLevel, RoundingMode.HALF_UP).toDouble()
        DisplayY = BigDecimal(realMotionY.toString()).setScale(bigDecimalLevel, RoundingMode.HALF_UP).toDouble()
        DisplayZ = BigDecimal(realMotionZ.toString()).setScale(bigDecimalLevel, RoundingMode.HALF_UP).toDouble()
    }
    val onPacket = handler<PacketEvent> { e ->
        if (e.packet is S12PacketEntityVelocity && e.packet.entityID == mc.thePlayer.entityId) {
            if (!alertKnockBack) return@handler
            alertX = BigDecimal((e.packet.motionX / 8000.0).toString()).setScale(bigDecimalLevel, RoundingMode.HALF_UP)
                .toDouble()
            alertY = BigDecimal((e.packet.motionY / 8000.0).toString()).setScale(bigDecimalLevel, RoundingMode.HALF_UP)
                .toDouble()
            alertZ = BigDecimal((e.packet.motionZ / 8000.0).toString()).setScale(bigDecimalLevel, RoundingMode.HALF_UP)
                .toDouble()
            chat("[MotionDisplay] KnockBackReceived! X:$alertX Y:$alertY Z:$alertZ")
        }
    }
    override val tag: String?
        get() = if (motionX && motionY && motionZ) {
            "$DisplayX | $DisplayY | $DisplayZ"
        } else if (motionX && !motionY && !motionZ) {
            "$DisplayX"
        } else if (motionX && motionY && !motionZ) {
            "$DisplayX | $DisplayY"
        } else if (motionX && !motionY && motionZ) {
            "$DisplayX | $DisplayZ"
        } else if (!motionX && motionY && !motionZ) {
            "$DisplayY"
        } else if (!motionX && !motionY && motionZ) {
            "$DisplayZ"
        } else "No Data"
}