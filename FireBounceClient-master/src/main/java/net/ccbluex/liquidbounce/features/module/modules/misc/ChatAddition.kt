package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.play.client.C01PacketChatMessage

object ChatAddition : Module("ChatAddition", Category.MISC) {
    private val mode by multiChoices("Mode", arrayOf("Prefix", "Suffix"), arrayOf("Prefix"))
    private val prefixText by text("PrefixText", "FireBounce") {"Prefix" in mode}
    private val suffixText by text("SuffixText", "| FireBounce") {"Suffix" in mode}
    private val ignoreCommands by boolean("IgnoreCommands", true)
    private val ignorePrefix by boolean("IgnorePrefix", true)
    private val addSpace by boolean("AddSpace", true)
    private val infiniteLength by boolean("InfiniteMessageLength",false)
    private val maxLength by int("MaxLength", 256, 1..256) {!infiniteLength}

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet is C01PacketChatMessage) {
            var msg = packet.message

            // Check for empty or null message
            if (msg.isBlank()) return@handler

            // Check if we should ignore commands
            if (ignoreCommands && (msg.startsWith("/") || (ignorePrefix && msg.startsWith(CommandManager.prefix)))) {
                return@handler
            }

            // Apply prefix or suffix
            val processedMessage = when {
                "Prefix" in mode && "Suffix" in mode -> {
                    // Both prefix and suffix
                    buildMessage(prefixText, msg, suffixText)
                }
                "Prefix" in mode -> {
                    // Only prefix
                    buildMessage(prefixText, msg, null)
                }
                "Suffix" in mode -> {
                    // Only suffix
                    buildMessage(null, msg, suffixText)
                }
                else -> msg
            }

            // Check message length
            if (processedMessage.length > maxLength && !infiniteLength) {
                chat("Message too long after adding prefix/suffix! Original message sent.")
                return@handler
            }

            // Update packet message
            packet.message = processedMessage
        }
    }

    private fun buildMessage(prefix: String?, original: String, suffix: String?): String {
        return buildString {
            prefix?.let {
                append(it)
                if (addSpace) append(" ") // Add space only if there's also a suffix
            }
            append(original)
            suffix?.let {
                if (addSpace) append(" ")
                append(it)
            }
        }
    }
}