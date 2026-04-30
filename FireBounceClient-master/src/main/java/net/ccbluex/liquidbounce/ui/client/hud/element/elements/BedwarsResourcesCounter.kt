/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import java.awt.Color

/**
 * Simple Bedwars Resources Display
 */
@ElementInfo(name = "BedwarsResourcesCounter")
class BedwarsResourcesCounter(x: Double = 10.0, y: Double = 100.0, scale: Float = 1F) : Element("BedwarsResourcesCounter", x, y, scale) {

    // Settings
    private val showEmeralds by boolean("Emeralds", true)
    private val showDiamonds by boolean("Diamonds", true)
    private val showGold by boolean("Gold", true)
    private val showIron by boolean("Iron", true)
    private val backgroundAlpha by int("BackgroundAlpha", 150, 0..255)
    private val textSize by float("TextSize", 0.85f, 0.5f..1.5f)

    // Colors
    private val shadowColor = Color(0, 0, 0, 120)
    private val bgColor = Color(40, 40, 40, backgroundAlpha)
    private val textColor = Color(255, 255, 255)
    private val textShadowColor = Color(0, 0, 0, 200)

    private val font by font("Font", Fonts.Bold36)

    override fun drawElement(): Border? {
        // Get amounts
        val emeralds = countItem(Items.emerald)
        val diamonds = countItem(Items.diamond)
        val gold = countItem(Items.gold_ingot)
        val iron = countItem(Items.iron_ingot)

        // Create resource list
        val resources = mutableListOf<Pair<ItemStack, Int>>()
        if (showEmeralds) resources.add(Pair(ItemStack(Items.emerald), emeralds))
        if (showDiamonds) resources.add(Pair(ItemStack(Items.diamond), diamonds))
        if (showGold) resources.add(Pair(ItemStack(Items.gold_ingot), gold))
        if (showIron) resources.add(Pair(ItemStack(Items.iron_ingot), iron))

        if (resources.isEmpty()) return null

        // Layout constants
        val itemSize = 24f
        val padding = 6f
        val gap = 4f
        val shadowOffset = 1.5f

        val rows = 1
        val cols = resources.size.coerceAtMost(4)

        val width = (itemSize * cols) + (gap * (cols - 1)) + (padding * 2)
        val height = (itemSize * rows) + (padding * 2)

        // 1. 先绘制背景和阴影
        // Draw shadow
        drawRoundedRect(
            shadowOffset, shadowOffset,
            width, height,
            shadowColor.rgb, 4f
        )

        // Draw background
        drawRoundedRect(0f, 0f, width, height, bgColor.rgb, 4f)
        
        for (i in resources.indices) {
            val (itemStack, _) = resources[i]

            // Calculate position
            val xPos = padding + (i % cols) * (itemSize + gap)
            val yPos = padding

            // Draw item
            GlStateManager.pushMatrix()
            RenderHelper.enableGUIStandardItemLighting()
            GlStateManager.translate(xPos, yPos, 0f)
            GlStateManager.scale(itemSize / 16f, itemSize / 16f, 1f)

            try {
                mc.renderItem.renderItemAndEffectIntoGUI(itemStack, 0, 0)
            } catch (_: Exception) { }
            RenderHelper.disableStandardItemLighting()
            GlStateManager.popMatrix()
        }
        for (i in resources.indices) {
            val (_, amount) = resources[i]

            // Calculate position
            val xPos = padding + (i % cols) * (itemSize + gap)
            val yPos = padding

            // Draw amount in bottom right corner
            val amountStr = amount.toString()

            // 使用矩阵变换
            GlStateManager.pushMatrix()
            GlStateManager.translate(xPos, yPos, 0f)

            // 应用文字缩放
            GlStateManager.scale(textSize, textSize, 1f)

            // 计算缩放后的文字位置
            val scaledItemSize = itemSize / textSize
            val textWidth = font.getStringWidth(amountStr).toFloat()
            val textHeight = 10f

            // 绘制文字阴影（大一点，确保可见）
            font.drawString(
                amountStr,
                (scaledItemSize - textWidth - 2f).toInt(),
                (scaledItemSize - textHeight - 2f).toInt(),
                textShadowColor.rgb
            )

            // 绘制白色文字
            font.drawString(
                amountStr,
                (scaledItemSize - textWidth - 3f).toInt(),
                (scaledItemSize - textHeight - 3f).toInt(),
                textColor.rgb
            )

            GlStateManager.popMatrix()
        }
        return Border(0f, 0f, width, height)
    }

    private fun countItem(item: net.minecraft.item.Item): Int {
        val player = mc.thePlayer ?: return 0
        var count = 0

        for (i in 0..35) {
            val stack = player.inventory.getStackInSlot(i)
            if (stack != null && stack.item == item) {
                count += stack.stackSize
            }
        }

        return count
    }
}