package net.ccbluex.liquidbounce.utils.extra

import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.mc
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos

object SendOffHandUseItem {
    fun sendOffHandUseItem() {
        mc.netHandler.addToSendQueue(
            C08PacketPlayerBlockPlacement(BlockPos(-1, -2, -1), 255, null, 0.0f, 0.0f, 0.0f)
        )
    }
}