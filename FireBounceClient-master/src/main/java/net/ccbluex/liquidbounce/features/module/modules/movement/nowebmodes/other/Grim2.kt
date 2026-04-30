package net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.other

import net.ccbluex.liquidbounce.event.BlockCollideEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.NoWeb.grimBreakOnWorld
import net.ccbluex.liquidbounce.features.module.modules.movement.NoWeb.grimStrict
import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.NoWebMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.block.BlockWeb
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.START_DESTROY_BLOCK
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK
import net.minecraft.util.EnumFacing.DOWN

object Grim2 : NoWebMode("Grim2") {
    override fun onCollide(event: BlockCollideEvent) {
        if (event.blockState.block is BlockWeb) {
            event.cancelEvent()

            if (grimStrict) sendPacket(C07PacketPlayerDigging(START_DESTROY_BLOCK, event.blockPos, DOWN))
            if (grimBreakOnWorld) mc.theWorld.setBlockToAir(event.blockPos)

            sendPacket(C07PacketPlayerDigging(STOP_DESTROY_BLOCK, event.blockPos, DOWN))
        }
    }
}