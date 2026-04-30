package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.FireBounce.hud
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraft.network.play.server.S38PacketPlayerListItem.Action.UPDATE_LATENCY

object AntiVanish : Module("AntiVanish", Category.MISC, gameDetecting = false) {

    private val warn by choices("Warn", arrayOf("Chat", "Notification"), "Chat")

    private var alertClearVanish = false

    override fun onDisable() {
        alertClearVanish = false
    }

    fun onWorld(event: WorldEvent) {
        // Reset check on world change
        alertClearVanish = false
    }

    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return
        }

        val packet = event.packet

        if (packet is S38PacketPlayerListItem) {
            handlePlayerList(packet)
        }
    }

    private fun handlePlayerList(packet: S38PacketPlayerListItem) {
        val action = packet.action
        val entries = packet.entries

        if (action == UPDATE_LATENCY) {
            val playerListSize = mc.netHandler?.playerInfoMap?.size ?: 0

            if (entries.size != playerListSize) {
                if (warn == "Chat") {
                    chat("§aA player might be vanished.")
                } else {
                    hud.addNotification(Notification("Warning", "§aA player might be vanished.",2000L))
                }

                alertClearVanish = false
            } else {
                if (alertClearVanish)
                    return

                if (warn == "Chat") {
                    chat("§cNo players are vanished")
                } else {
                    hud.addNotification(Notification("Warning", "§aA player might be vanished.",2000L))
                }

                alertClearVanish = true
            }
        }
    }
}
