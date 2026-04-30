package net.ccbluex.liquidbounce.utils.client

import net.minecraft.network.play.client.C01PacketChatMessage

object MessageManager {
    fun sayMessage(message: String) {
        val mc = MinecraftInstance.mc
        val player = mc.thePlayer
        val netHandler = mc.netHandler

        if (message.isEmpty() || player == null || netHandler == null) return

        val trimmedMessage = if (message.length > 256) {
            message.substring(0, 256)
        } else {
            message
        }

        netHandler.addToSendQueue(C01PacketChatMessage(trimmedMessage))
    }
}