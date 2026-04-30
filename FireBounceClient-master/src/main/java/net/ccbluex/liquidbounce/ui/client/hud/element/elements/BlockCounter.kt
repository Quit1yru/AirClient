/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold2
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.render.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorder
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.max

/**
 * WinUI 3 Style Block Counter
 * Features: Compact Layout, Configurable Alpha/Speed, Per-digit animation, Render Held Item.
 */
@ElementInfo(name = "BlockCounter")
class BlockCounter(x: Double = 10.0, y: Double = 50.0, scale: Float = 1F) : Element("BlockCounter", x, y, scale) {

    // Settings
    private val layoutMode by choices("Layout", arrayOf("Horizontal", "Vertical"), "Horizontal")
    private val lowAmountThreshold by int("LowAmountThreshold", 64, 0..128)
    private val backgroundAlpha by int("BackgroundAlpha", 180, 0..255)
    private val animationSpeed by float("AnimSpeed", 0.1f, 0.01f..0.25f)

    // Colors
    private val warnColor = Color(255, 80, 80) // Red when low
    private val normalColor = Color(255, 255, 255) // White normally
    private val cardBorderColor = Color(60, 60, 60, 255)

    // Animation State
    private var targetAmount = 0
    private var oldAmount = 0
    private var animProgress = 1f // 0f = Start, 1f = Finished

    // Visual Anim States
    private var currentShakeX = 0f
    private var scaleAnim = 0f // 0f = Hidden, 1f = Shown

    // Fonts
    private val countFont = Fonts.fontExtraBold40
    private val labelFont = Fonts.fontSemibold35

    override fun drawElement(): Border? {
        // Condition: Show only on Scaffold or Designer
        val shouldShow = Scaffold.state || Scaffold2.state || mc.currentScreen is GuiHudDesigner

        // Faster Scale Animation (0.25f)
        val targetScale = if (shouldShow) 1f else 0f
        scaleAnim = lerp(scaleAnim, targetScale, 0.25f)

        if (scaleAnim < 0.05f && !shouldShow) return null

        // 1. Logic
        val realAmount = InventoryUtils.blocksAmount()

        if (realAmount != targetAmount) {
            oldAmount = targetAmount
            targetAmount = realAmount
            animProgress = 0f
        }

        // Scroll Animation Speed
        if (animProgress < 1f) {
            animProgress += animationSpeed
            if (animProgress > 1f) animProgress = 1f
        }

        val isLow = realAmount < 10

        // Shake Logic
        currentShakeX = if (isLow && realAmount > 0) {
            lerp(currentShakeX, (Math.random().toFloat() - 0.5f) * 3f, 0.2f)
        } else {
            lerp(currentShakeX, 0f, 0.1f)
        }

        // 2. Layout Calculations
        val padding = 6f // Reduced padding
        val iconSize = 20f // Slightly smaller icon for compact look
        val gap = 8f // Gap between Icon and Text
        val textGap = 4f // Gap between Number and Label

        val strTarget = targetAmount.toString()
        val strOld = oldAmount.toString()
        val maxLen = max(strTarget.length, strOld.length)

        // Calculate widest possible number width
        val amountWidth = max(countFont.getStringWidth(strTarget), countFont.getStringWidth(strOld)).toFloat()
        val labelStr = "Blocks"
        val labelWidth = labelFont.getStringWidth(labelStr)

        // --- Calculate Box Dimensions based on Layout ---
        val width: Float
        val height: Float

        val textStartX = padding + iconSize + gap

        // Coordinates for text elements
        val numberX: Float
        val numberY: Float
        val labelX: Float
        val labelY: Float

        if (layoutMode == "Horizontal") {
            // Layout: [Icon] [Number] [Label]
            // Everything in one line
            width = textStartX + amountWidth + textGap + labelWidth + padding
            height = 32f // Much more compact height

            val centerY = height / 2f

            // Align Number
            numberX = textStartX
            // Font correction: ExtraBold40 needs ~4f offset to center
            numberY = centerY - 4f

            // Align Label (Next to number)
            labelX = numberX + amountWidth + textGap
            // Correction: Lift the label up to align with the number center
            labelY = centerY - 3f
        } else {
            // Layout: [Icon] [Number]
            //                [Label]
            val contentW = max(amountWidth, labelWidth.toFloat())
            width = textStartX + contentW + padding
            height = 44f

            val centerY = height / 2f

            numberX = textStartX
            numberY = centerY - 9f

            labelX = textStartX
            labelY = centerY + 3f
        }

        val textColor = if (isLow) warnColor else normalColor

        // === Drawing Main Content ===
        GlStateManager.pushMatrix()

        // Scale Animation (Center Pivot)
        val centerX = width / 2f
        val centerY = height / 2f
        GlStateManager.translate(centerX, centerY, 0f)
        GlStateManager.scale(scaleAnim, scaleAnim, 1f)
        GlStateManager.translate(-centerX, -centerY, 0f)

        // Shake
        GlStateManager.translate(currentShakeX, 0f, 0f)

        // Background
        GlowUtils.drawGlow(0f, 0f, width, height, 15, Color(0, 0, 0, 80))

        drawRoundedRect(0f, 0f, width, height, Color(32, 32, 32, backgroundAlpha).rgb, 6f)

        // Border & Highlight
        drawRoundedBorder(0f, 0f, width, height, 1f, cardBorderColor.rgb, 6f)
        drawRect(6f, 0.5f, width - 6f, 1.5f, Color(255, 255, 255, 20).rgb) // Top Highlight

        // Floating Icon (Static Center Y)
        val iconX = padding
        val iconY = (height - iconSize) / 2f

        GlStateManager.pushMatrix()
        RenderHelper.enableGUIStandardItemLighting()
        GlStateManager.translate(iconX, iconY, 0f)
        GlStateManager.scale(iconSize / 16f, iconSize / 16f, 1f)

        // Logic: Try Held Item -> Try Inventory Block -> Default to Stone
        val stackToRender = mc.thePlayer.heldItem ?: findBlockInInventory() ?: ItemStack(Blocks.stone)

        try {
            mc.renderItem.renderItemAndEffectIntoGUI(stackToRender, 0, 0)
        } catch (_: Exception) { }
        RenderHelper.disableStandardItemLighting()
        GlStateManager.popMatrix()

        // --- Draw Label (Static) ---
        labelFont.drawString(labelStr, labelX, labelY, Color(180, 180, 180).rgb)

        // --- Draw Rolling Numbers ---

        // Only enable scissor if scale is fully complete
        val useScissor = scaleAnim > 0.95f

        if (useScissor) {
            val sr = ScaledResolution(mc)
            val factor = sr.scaleFactor
            val globalX = renderX + numberX

            // Clip Area for Numbers
            GL11.glEnable(GL11.GL_SCISSOR_TEST)
            GL11.glScissor(
                (globalX * factor).toInt(),
                (mc.displayHeight - (renderY + numberY + 15f) * factor).toInt(),
                ((amountWidth + 5f) * factor).toInt(),
                (25 * factor)
            )
        }

        // Animation Math
        val t = 1f - animProgress
        val ease = 1f - (t * t * t)
        val isIncreasing = targetAmount > oldAmount
        val direction = if (isIncreasing) 1 else -1

        val padTarget = strTarget.padStart(maxLen, ' ')
        val padOld = strOld.padStart(maxLen, ' ')

        var currentDigitX = numberX

        for (i in 0 until maxLen) {
            val charNew = padTarget[i]
            val charOld = padOld[i]
            val charWidth = max(countFont.getStringWidth(charNew.toString()), countFont.getStringWidth(charOld.toString())).toFloat()

            if (charNew == charOld) {
                // Static digit
                countFont.drawString(charNew.toString(), currentDigitX, numberY, textColor.rgb)
            } else {
                // Animated digit

                // Old (Moving Out)
                if (animProgress < 1f) {
                    val oldAlpha = (255 * (1f - ease)).toInt().coerceIn(0, 255)
                    val oldColor = Color(textColor.red, textColor.green, textColor.blue, oldAlpha)
                    val oldYOffset = (ease * 12f * direction)
                    countFont.drawString(charOld.toString(), currentDigitX, numberY - oldYOffset, oldColor.rgb)
                }

                // New (Moving In)
                val newAlpha = (255 * ease).toInt().coerceIn(0, 255)
                val newColor = Color(textColor.red, textColor.green, textColor.blue, newAlpha)
                val newYOffset = ((1f - ease) * 12f * direction)
                countFont.drawString(charNew.toString(), currentDigitX, numberY + newYOffset, newColor.rgb)
            }

            currentDigitX += charWidth
        }

        if (useScissor) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST)
        }

        GlStateManager.popMatrix()

        return Border(0f, 0f, width, height)
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }

    private fun findBlockInInventory(): ItemStack? {
        for (i in 0..8) {
            val stack = mc.thePlayer.inventory.getStackInSlot(i)
            if (stack != null && stack.item is net.minecraft.item.ItemBlock) {
                return stack
            }
        }
        return null
    }
}