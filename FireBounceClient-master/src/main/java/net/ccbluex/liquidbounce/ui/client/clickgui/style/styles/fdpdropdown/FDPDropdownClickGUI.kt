/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.SideGui.SideGui
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.impl.SettingComponents
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.animations.Animation
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.animations.Direction
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.animations.impl.DecelerateAnimation
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.animations.impl.EaseBackIn
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.normal.Main
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.render.DrRenderUtils
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

/**
 * ClickGUI FDP
 */
class FDPDropdownClickGUI : GuiScreen() {

    private val sideGui = SideGui()

    private lateinit var openingAnimation: Animation
    private lateinit var fadeAnimation: EaseBackIn
    private lateinit var configHover: DecelerateAnimation

    private val hudIcon = ResourceLocation("liquidbounce/custom_hud_icon.png")

    private var categoryPanels: MutableList<DropdownCategory>? = null
    private var isInitialized = false

    override fun initGui() {
        try {
            if (categoryPanels == null || Main.reloadModules || !isInitialized) {
                categoryPanels = mutableListOf<DropdownCategory>().apply {
                    Category.entries.forEach { category ->
                        add(DropdownCategory(category))
                    }
                }
                Main.reloadModules = false
                isInitialized = true
            }

            sideGui.initGui()

            fadeAnimation = EaseBackIn(400, 1.0, 2.0f)
            openingAnimation = EaseBackIn(400, 0.4, 2.0f)
            configHover = DecelerateAnimation(250, 1.0)

            categoryPanels?.forEach { panel ->
                panel.animation = fadeAnimation
                panel.openingAnimation = openingAnimation
                panel.initGui()
            }
        } catch (e: Exception) {
            println("Error initializing FDPDropdownClickGUI: ${e.message}")
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        try {
            if (keyCode == 1) {
                openingAnimation.direction = Direction.BACKWARDS
                fadeAnimation.direction = openingAnimation.direction
            }
            sideGui.keyTyped(typedChar, keyCode)
            categoryPanels?.forEach { it.keyTyped(typedChar, keyCode) }
        } catch (e: Exception) {
            println("Error handling key input: ${e.message}")
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        try {
            assumeNonVolatile {
                if (Mouse.isButtonDown(0) && mouseX in 5..50 && mouseY in (height - 50)..(height - 5)) {
                    mc.displayGuiScreen(GuiHudDesigner())
                }
                RenderUtils.drawImage(hudIcon, 9, height - 41, 32, 32)
                if (openingAnimation.isDone && openingAnimation.direction == Direction.BACKWARDS) {
                    mc.displayGuiScreen(null)
                    return@assumeNonVolatile
                }
                val sr = ScaledResolution(mc)
                val guiScale = ClickGUI.fdpscale // 使用ClickGUI中的设置
                val animVal: Any? = openingAnimation.output
                val animOut: Double = if (animVal is Number) animVal.toDouble() else 0.0
                val finalScale = (animOut + 0.6) * guiScale.toDouble()
                SettingComponents.scale = finalScale.toFloat()
                val transformedMouseX = sr.scaledWidth / 2f + (mouseX - sr.scaledWidth / 2f) / finalScale
                val transformedMouseY = sr.scaledHeight / 2f + (mouseY - sr.scaledHeight / 2f) / finalScale

                val focusedConfigGui = sideGui.focused
                val effectiveMouseX = if (focusedConfigGui) 0 else transformedMouseX.toInt()
                val effectiveMouseY = if (focusedConfigGui) 0 else transformedMouseY.toInt()

                DrRenderUtils.scale(sr.scaledWidth / 2f, sr.scaledHeight / 2f, finalScale.toFloat()) {
                    categoryPanels?.forEach { 
                        try {
                            it.drawScreen(effectiveMouseX, effectiveMouseY) 
                        } catch (_: Throwable) {}
                    }
                }
                
                val sideAnimVal: Any? = fadeAnimation.output
                val sideAnimOut: Double = if (sideAnimVal is Number) sideAnimVal.toDouble() else 0.0
                sideGui.drawScreen(mouseX, mouseY, partialTicks, (255.0 * sideAnimOut).toInt().coerceIn(0, 255))
            }
            super.drawScreen(mouseX, mouseY, partialTicks)
        } catch (_: Exception) {
            // println("Error during FDP GUI rendering: ${e.message}")
        }
        
        // 处理鼠标滚轮事件，类似主ClickGui的实现
        if (Mouse.hasWheel()) {
            val wheel = Mouse.getDWheel()
            if (wheel != 0) {
                handleScroll(wheel)
            }
        }
    }

    private fun handleScroll(wheel: Int) {
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
            var newScale = ClickGUI.fdpscale
            // 根据滚轮方向调整缩放
            if (wheel > 0) {
                newScale += 0.05f
            } else if (wheel < 0) {
                newScale -= 0.05f
            }
            // 限制缩放范围在0.5f到2.0f之间
            newScale = newScale.coerceIn(0.5f, 2.0f)
            // 通过赋值操作更新ClickGUI模块中的fdpscale值
            ClickGUI.fdpscale = newScale
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        try {
            val oldFocus = sideGui.focused
            sideGui.mouseClicked(mouseX, mouseY, mouseButton)
            if (!oldFocus) {
                val sr = ScaledResolution(mc)
                val finalScale = (openingAnimation.output + 0.6f) * ClickGUI.fdpscale // 使用ClickGUI中的设置
                val transformedMouseX = sr.scaledWidth / 2f + (mouseX - sr.scaledWidth / 2f) / finalScale
                val transformedMouseY = sr.scaledHeight / 2f + (mouseY - sr.scaledHeight / 2f) / finalScale
                categoryPanels?.forEach { it.mouseClicked(transformedMouseX.toInt(), transformedMouseY.toInt(), mouseButton) }
            }
        } catch (e: Exception) {
            println("Error handling mouse click: ${e.message}")
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        try {
            val oldFocus = sideGui.focused
            sideGui.mouseReleased(mouseX, mouseY, state)
            if (!oldFocus) {
                val sr = ScaledResolution(mc)
                val finalScale = (openingAnimation.output + 0.6f) * ClickGUI.fdpscale // 使用ClickGUI中的设置
                val transformedMouseX = sr.scaledWidth / 2f + (mouseX - sr.scaledWidth / 2f) / finalScale
                val transformedMouseY = sr.scaledHeight / 2f + (mouseY - sr.scaledHeight / 2f) / finalScale
                categoryPanels?.forEach { it.mouseReleased(transformedMouseX.toInt(), transformedMouseY.toInt(), state) }
            }
        } catch (e: Exception) {
            println("Error handling mouse release: ${e.message}")
        }
    }

    fun resetGui() {
        isInitialized = false
        categoryPanels?.clear()
        categoryPanels = null
    }

    fun exitGui() {
        openingAnimation.direction = Direction.BACKWARDS
        fadeAnimation.direction = Direction.BACKWARDS
    }

    override fun doesGuiPauseGame() = false
}