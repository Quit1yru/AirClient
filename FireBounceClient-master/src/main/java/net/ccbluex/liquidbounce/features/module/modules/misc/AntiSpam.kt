/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent

object AntiSpam : Module("AntiSpam", Category.MISC, subjective = true, gameDetecting = false) {

    private val messageHistory = LinkedHashMap<String, Pair<Int, IChatComponent>>() // 存储消息格式化文本、计数和原始组件
    private val maxHistorySize by int("MaxHistorySize", 50, 10..200) // 最大历史记录数量
    private val enabled by boolean("Enabled", true) // 是否启用功能

    val onPacket = handler<PacketEvent> { event ->
        if (!enabled) return@handler

        if (event.packet is S02PacketChat) {
            val chatComponent = event.packet.chatComponent
            val formattedText = chatComponent.formattedText // 使用带格式的文本作为键

            // 检查是否为重复消息
            if (messageHistory.containsKey(formattedText)) {
                // 如果是重复消息，增加计数
                val (count, originalComponent) = messageHistory[formattedText]!!
                val newCount = count + 1
                messageHistory[formattedText] = Pair(newCount, chatComponent) // 更新计数

                // 取消原版消息显示
                event.cancelEvent()

                // 显示合并后的消息（保留原始格式）
                val updatedMessage = ChatComponentText("$formattedText [x$newCount]")

                mc.ingameGUI.chatGUI.printChatMessage(updatedMessage)
            } else {
                // 如果不是重复消息，添加到历史记录
                messageHistory[formattedText] = Pair(1, chatComponent)

                // 清理超出历史记录大小限制的旧消息
                if (messageHistory.size > maxHistorySize) {
                    messageHistory.remove(messageHistory.keys.first()) // 移除最老的条目
                }
            }
        }
    }

    /**
     * 清空消息历史记录
     */
    fun clearHistory() {
        messageHistory.clear()
    }
}
