package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.random
import net.minecraft.network.play.client.C03PacketPlayer

object AutoBan : Module("AutoBan", Category.FUN) {
    val RandomStrength by floatRange("BanSpeed",-1f..1f,-200f..200f)
    val onUpdate = handler<UpdateEvent> { event ->
        sendPacket(
            C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX + 10,
                mc.thePlayer.posY + 10,
                mc.thePlayer.posZ + 10,
                false
            )
        )
        sendPacket(
            C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX - 10,
                mc.thePlayer.posY - 10,
                mc.thePlayer.posZ - 10,
                false
            )
        )
        mc.thePlayer.motionX *= RandomStrength.random()
        mc.thePlayer.motionY *= RandomStrength.random()
        mc.thePlayer.motionZ *= RandomStrength.random()
    }
}