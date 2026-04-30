package net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.tatako

import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.NoWebMode
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos

object Tatako : NoWebMode("Tatako") {
    override fun onUpdate() {
        if (mc.thePlayer.isInWeb) {
            mc.thePlayer.isInWeb = false
            mc.thePlayer.swingItem()
            mc.netHandler.addToSendQueue(
                C08PacketPlayerBlockPlacement(
                    BlockPos(
                        mc.thePlayer.posX,
                        mc.thePlayer.posY - 1.0,
                        mc.thePlayer.posZ
                    ), 1, null as ItemStack?, 0.5f, 0.5f, 0.5f
                )
            )
        }
    }
}