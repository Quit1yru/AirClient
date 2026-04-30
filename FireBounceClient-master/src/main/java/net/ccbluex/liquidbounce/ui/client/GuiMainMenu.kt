/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
@file:Suppress("unused", "SameParameterValue")

package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.FireBounce
import net.ccbluex.liquidbounce.FireBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.FireBounce.MINECRAFT_VERSION
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.client.tools.GuiTools
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.I18n
import net.minecraft.util.ResourceLocation
import java.awt.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Suppress("PrivatePropertyName")
class GuiMainMenu : GuiScreen() {

    private val splashText = "§c§lFireBounce §7- §fMinecraft Hacked Client"

    // 布局常量
    private val BUTTON_WIDTH = 200
    private val BUTTON_HEIGHT = 24
    private val BUTTON_SPACING = 28

    // 动画相关变量
    private var initTime = 0L
    private val STAGGER_DELAY = 50L // 每个按钮的延迟

    override fun initGui() {
        // 重置动画计时器
        initTime = System.currentTimeMillis()

        buttonList.clear()

        val centerX = width / 2
        val startY = height / 4 + 48

        // 1. Singleplayer
        buttonList.add(WinUIButton(1, centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT, I18n.format("menu.singleplayer")))

        // 2. Multiplayer
        buttonList.add(WinUIButton(2, centerX - BUTTON_WIDTH / 2, startY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT, I18n.format("menu.multiplayer")))

        // 3. AltManager
        buttonList.add(WinUIButton(3, centerX - BUTTON_WIDTH / 2, startY + BUTTON_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT, "AltManager"))

        // 4. Client Settings
        buttonList.add(WinUIButton(6, centerX - BUTTON_WIDTH / 2, startY + BUTTON_SPACING * 3, BUTTON_WIDTH, BUTTON_HEIGHT, "Client Settings"))

        // --- 次要按钮行 ---
        val halfWidth = (BUTTON_WIDTH - 6) / 2
        val row4Y = startY + BUTTON_SPACING * 4
        val row5Y = startY + BUTTON_SPACING * 5

        // 5. Mods & Tools
        buttonList.add(WinUIButton(5, centerX - BUTTON_WIDTH / 2, row4Y, halfWidth, BUTTON_HEIGHT, "Mods"))
        buttonList.add(WinUIButton(7, centerX + 3, row4Y, halfWidth, BUTTON_HEIGHT, "Tools"))

        // 6. Options & Quit
        buttonList.add(WinUIButton(0, centerX - BUTTON_WIDTH / 2, row5Y, halfWidth, BUTTON_HEIGHT, I18n.format("menu.options")))
        buttonList.add(WinUIButton(4, centerX + 3, row5Y, halfWidth, BUTTON_HEIGHT, I18n.format("menu.quit")))

        // 7. Font Button
        buttonList.add(WinUIButton(8, width - 125, height - 30, 120, 24, getFontButtonText()))
    }

    private fun getFontButtonText(): String {
        return try {
            val fontName = Fonts.fontSemibold35.defaultFont.font.name
            "Font: $fontName"
        } catch (e: Exception) {
            "Font: Default"
        }
    }

    /**
     * 切换字体逻辑
     * @param offset 1 为下一个, -1 为上一个
     */
    private fun switchFont(offset: Int) {
        val button = buttonList.find { it.id == 8 } ?: return
        try {
            val fontList = Fonts.fonts
            if (fontList.isNotEmpty()) {
                val currentIndex = fontList.indexOf(Fonts.fontSemibold35)

                // 计算新索引 (循环列表)
                var newIndex = currentIndex + offset

                // 处理边界情况
                if (newIndex >= fontList.size) newIndex = 0
                if (newIndex < 0) newIndex = fontList.size - 1

                val newFont = fontList[newIndex]
                if (newFont is GameFontRenderer) {
                    Fonts.fontSemibold35 = newFont
                }
            }
            button.displayString = getFontButtonText()
        } catch (e: Exception) {
            LOGGER.error("Failed to switch font", e)
            button.displayString = "Font: Error"
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        drawRect(0, 0, width, height, Color(0, 0, 0, 80).rgb)

        GlStateManager.disableAlpha()
        GlStateManager.enableAlpha()

        val currentTime = System.currentTimeMillis()
        val timeElapsed = currentTime - initTime

        // 绘制标题
        val titleAnimProgress = min(1f, timeElapsed / 800f)
        val titleEase = easeOutCubic(titleAnimProgress)
        val titleAlpha = (titleEase * 255).toInt()
        val titleYOffset = (1f - titleEase) * -30f

        if (titleAlpha > 5) {
            val titleY = height / 4 - 40f + titleYOffset
            val titleColor = (titleAlpha shl 24) or 0xFFFFFF
            val subColor = (titleAlpha shl 24) or 0xAAAAAA

            safeDrawCenteredString(Fonts.fontBold180, CLIENT_NAME, width / 2F, titleY, titleColor, true)
            safeDrawCenteredString(Fonts.fontSemibold40, "${FireBounce.clientVersionText} (MC $MINECRAFT_VERSION)", width / 2F, titleY + 50F, subColor, true)
            safeDrawCenteredString(Fonts.fontSemibold35, splashText, width / 2F, titleY + 70F, titleColor, true)
        }

        // 更新按钮动画
        for ((index, button) in buttonList.withIndex()) {
            if (button is WinUIButton) {
                val buttonStartTime = index * STAGGER_DELAY
                val buttonActiveTime = max(0L, timeElapsed - buttonStartTime)
                val progress = min(1f, buttonActiveTime / 500f)
                val ease = easeOutCubic(progress)

                button.alphaMultiplier = ease
                button.animYOffset = (1f - ease) * 40f
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
            1 -> mc.displayGuiScreen(GuiSelectWorld(this))
            2 -> mc.displayGuiScreen(GuiMultiplayer(this))
            3 -> mc.displayGuiScreen(GuiAltManager(this))
            4 -> mc.shutdown()
            5 -> mc.displayGuiScreen(GuiModsMenu(this))
            6 -> mc.displayGuiScreen(GuiClientConfiguration(this))
            7 -> mc.displayGuiScreen(GuiTools(this))
            8 -> switchFont(1) // 左键点击：下一个字体
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        // 调用父类方法处理左键点击 (button 0)
        super.mouseClicked(mouseX, mouseY, mouseButton)

        // 自行处理右键点击 (button 1)
        if (mouseButton == 1) {
            val fontButton = buttonList.find { it.id == 8 }
            // 检查鼠标是否在字体按钮上
            if (fontButton != null && fontButton.visible &&
                mouseX >= fontButton.xPosition && mouseY >= fontButton.yPosition &&
                mouseX < fontButton.xPosition + fontButton.width && mouseY < fontButton.yPosition + fontButton.height) {

                switchFont(-1) // 右键点击：上一个字体

                // 播放点击音效
                mc.soundHandler.playSound(PositionedSoundRecord.create(ResourceLocation("gui.button.press"), 1.0f))
            }
        }
    }

    private fun easeOutCubic(x: Float): Float = 1f - (1f - x).pow(3)

    private fun safeDrawCenteredString(fontRenderer: GameFontRenderer, text: String, x: Float, y: Float, color: Int, shadow: Boolean) {
        try {
            if ((color shr 24 and 0xFF) < 5) return
            if (shadow) fontRenderer.drawCenteredString(text, x, y, color)
            else fontRenderer.drawCenteredString(text, x, y, color, false)
        } catch (e: Exception) {
            val strWidth = mc.fontRendererObj.getStringWidth(text)
            mc.fontRendererObj.drawString(text, x - strWidth / 2, y, color, shadow)
        }
    }

    class WinUIButton(buttonId: Int, x: Int, y: Int, width: Int, height: Int, buttonText: String) :
        GuiButton(buttonId, x, y, width, height, buttonText) {

        var alphaMultiplier: Float = 1f
        var animYOffset: Float = 0f

        override fun drawButton(mc: Minecraft, mouseX: Int, mouseY: Int) {
            if (!this.visible || alphaMultiplier <= 0.05f) return

            val renderY = yPosition + animYOffset
            val isAnimFinished = alphaMultiplier > 0.9f
            this.hovered = isAnimFinished && mouseX >= this.xPosition && mouseY >= this.yPosition &&
                    mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height

            val bgAlpha = if (hovered) 220 else 180
            val finalBgAlpha = (bgAlpha * alphaMultiplier).toInt().coerceIn(0, 255)
            val bgColor = if (hovered) (finalBgAlpha shl 24) or 0x3C3C3C else (finalBgAlpha shl 24) or 0x202020

            val textAlpha = if (this.enabled) 255 else 160
            val finalTextAlpha = (textAlpha * alphaMultiplier).toInt().coerceIn(4, 255)
            val textColor = if (this.enabled) {
                if (hovered) (finalTextAlpha shl 24) or 0xFFFFFF else (finalTextAlpha shl 24) or 0xE0E0E0
            } else {
                (finalTextAlpha shl 24) or 0xA0A0A0
            }

            GlStateManager.enableBlend()
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
            GlStateManager.blendFunc(770, 771)

            RenderUtils.drawRoundedRect(xPosition.toFloat(), renderY, (xPosition + width).toFloat(), renderY + height, bgColor, 4f)

            this.mouseDragged(mc, mouseX, mouseY)

            try {
                Fonts.fontSemibold35.drawCenteredString(displayString, xPosition + width / 2f, renderY + height / 2f - 3f, textColor)
            } catch (e: Exception) {
                mc.fontRendererObj.drawStringWithShadow(displayString, (xPosition + width / 2 - mc.fontRendererObj.getStringWidth(displayString) / 2).toFloat(),
                    (renderY + (height - 8) / 2), textColor)
            }
        }
    }
}