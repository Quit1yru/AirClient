/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.impl

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.FDPState
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.animations.Animation
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.animations.Direction
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.animations.impl.DecelerateAnimation
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.animations.impl.EaseInOutQuad
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.normal.Main
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.render.DrRenderUtils
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.render.DrRenderUtils.resetColor
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.Component

class ModuleRect(val module: Module) : Component() {

    val settingComponents = SettingComponents(module)

        private val toggleAnimation = EaseInOutQuad(300, 1.0, Direction.BACKWARDS)
        private val arrowAnimation = EaseInOutQuad(300, 1.0, Direction.BACKWARDS)
        private val hoverAnimation = DecelerateAnimation(300, 1.0, Direction.BACKWARDS)
    
        var settingAnimation: Animation? = null
        var openingAnimation: Animation? = null
    
        var x: Float = 0f
        var y: Float = 0f
        var width: Float = 0f
        var height: Float = 18f
        var panelLimitY: Float = 0f
        var alphaAnimation: Int = 0
    
        var clickX: Int = 0
        var clickY: Int = 0
    
        var settingSize: Double = 0.0
            private set
    
        fun initGui() {
            toggleAnimation.direction = if (module.state) Direction.FORWARDS else Direction.BACKWARDS
        }
    
        fun keyTyped(typedChar: Char, keyCode: Int) {
            if (FDPState.isExpanded(module)) {
                settingComponents.keyTyped(typedChar, keyCode)
            }
        }
    
        fun drawScreen(mouseX: Int, mouseY: Int) {
            try {
                val bgAlphaSetting = ClickGUI.fdpbgalpha // 使用ClickGUI中的设置
                val finalAlpha = (alphaAnimation * (bgAlphaSetting / 255f)).toInt().coerceIn(0, 255)
                // Clearly semi-transparent black base background (Alpha adjusted to be noticeable)
                val baseRectColor: Int = Color(10, 10, 10, (finalAlpha * 0.3f).toInt()).rgb
                val textColor: Int = Color(255, 255, 255, alphaAnimation).rgb // Pure white font (respect alpha)
    
                val accent = ClickGUI.generateColor(0) // 使用ClickGUI中的generateColor方法
                val accentWithAlpha = DrRenderUtils.applyOpacity(accent, finalAlpha / 255f)
    
                val hoveringModule = DrRenderUtils.isHovering(x, y, width, height, mouseX, mouseY)
                hoverAnimation.direction = if (hoveringModule) Direction.FORWARDS else Direction.BACKWARDS
    
                val hoveredRectColorInt: Int = DrRenderUtils.interpolateColor(
                    baseRectColor,
                    Color(60, 60, 60, (finalAlpha * 0.5f).toInt()).rgb,
                    hoverAnimation.output.toFloat()
                )
                RenderUtils.drawRect(x, y, x + width, y + height, hoveredRectColorInt)                        // High contrast accent overlay for enabled modules - now matches theme color but with transparency
                        val accentAlpha = (toggleAnimation.output.toFloat() * (finalAlpha / 255f) * 0.8f) 
                        val accentOverlayColor: Int = DrRenderUtils.applyOpacity(accentWithAlpha, accentAlpha).rgb
                        
                        RenderUtils.drawRect(x, y, x + width, y + height, accentOverlayColor)            
                        // Expanded settings area background: translucent dark gray - made darker for more contrast
                        val settingRectColor: Int = Color(10, 10, 10, (alphaAnimation * 0.85f).toInt()).rgb
                        val animVal: Any? = settingAnimation?.output
                        val animOut: Double = if (animVal is Number) animVal.toDouble() else 0.0
                        val expandedHeight = settingComponents.settingSize * animOut
            
                        if (FDPState.isExpanded(module) || (animOut > 0.0)) {
                            // Adjusting Y to match new height logic
                            RenderUtils.drawRect(x, y + height, x + width, y + height + (expandedHeight * 20f).toFloat(), settingRectColor)
            
                            if (ClickGUI.fdpbackback) { // 使用ClickGUI中的设置
                                val accentAlpha2 = (0.4f * toggleAnimation.output).toFloat() * (alphaAnimation / 255f)
                                RenderUtils.drawRect(x, y + height, x + width, y + height + (expandedHeight * 20f).toFloat(), DrRenderUtils.applyOpacity(accentWithAlpha, accentAlpha2).rgb)
                            }
            
                            settingComponents.x = x
                            settingComponents.y = y + height
                            settingComponents.width = width
                            settingComponents.rectHeight = 18f // 减少设置高度
                            settingComponents.panelLimitY = panelLimitY
                            settingComponents.alphaAnimation = alphaAnimation // Use full alpha for settings text
                            settingComponents.settingHeightScissor = settingAnimation
            
                            if (animOut < 1.0) {
                                GL11.glEnable(GL11.GL_SCISSOR_TEST)
                                DrRenderUtils.scissor(x.toDouble(), (y + height).toDouble(), width.toDouble(), expandedHeight * 20.0)
                                settingComponents.drawScreen(mouseX, mouseY)
                                GL11.glDisable(GL11.GL_SCISSOR_TEST)
                            } else {
                                settingComponents.drawScreen(mouseX, mouseY)
                            }
                        }
                        
                        // Draw module name and arrow AFTER everything to ensure highest priority
                resetColor()
                        Fonts.fontGoogleSans35.drawString(module.name, x + 5f, y + (height - Fonts.fontGoogleSans35.height.toFloat()) / 2f, textColor, false)
            
                        if (Keyboard.isKeyDown(Keyboard.KEY_TAB) && module.keyBind != 0) {
                            val keyName = Keyboard.getKeyName(module.keyBind)
                            Fonts.fontGoogleSans35.drawString(keyName, x + width - Fonts.fontGoogleSans35.getStringWidth(keyName) - 5f, y + (height - Fonts.fontGoogleSans35.height.toFloat()) / 2f, textColor, false)
                        } else {
                            val arrowSize = 6f
                            arrowAnimation.direction = if (FDPState.isExpanded(module)) Direction.FORWARDS else Direction.BACKWARDS
                            DrRenderUtils.setAlphaLimit(0f)
                            resetColor()
                            DrRenderUtils.drawClickGuiArrow(x + width - (arrowSize + 5f), y + (height / 2f) - 2f, arrowSize, arrowAnimation, textColor) 
                        }
                        
                        settingSize = expandedHeight        } catch (e: Exception) {}
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, button: Int) {
        val hoveringModule = isClickable(y, panelLimitY) &&
                DrRenderUtils.isHovering(x, y, width, height, mouseX, mouseY)
        if (hoveringModule) {
            when (button) {
                0 -> {
                    clickX = mouseX
                    clickY = mouseY
                    toggleAnimation.direction = if (!module.state) Direction.FORWARDS else Direction.BACKWARDS
                    module.toggle()
                }
                1 -> FDPState.toggleExpanded(module)
            }
        }
        if (FDPState.isExpanded(module)) {
            settingComponents.mouseClicked(mouseX, mouseY, button)
        }
    }

    fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (FDPState.isExpanded(module)) {
            settingComponents.mouseReleased(mouseX, mouseY, state)
        }
    }

    fun isClickable(currentY: Float, limitY: Float): Boolean {
        // Threshold check: the element must be within the scrollable area and below the header
        return currentY > limitY + 18f && currentY < (limitY + Main.allowedClickGuiHeight.toFloat() + 18f)
    }
}