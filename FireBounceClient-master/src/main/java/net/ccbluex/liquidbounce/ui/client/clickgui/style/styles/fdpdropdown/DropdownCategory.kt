/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.impl.ModuleRect
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.FDPState
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.animations.Animation
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.animations.Direction
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.animations.impl.DecelerateAnimation
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.normal.Main
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.normal.Screen
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.render.DrRenderUtils
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.render.StencilUtil
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.ScaledResolution
import java.awt.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Represents a panel for a single [Category], containing modules in that category.
 */
class DropdownCategory(private val category: Category) : Screen {

    private val rectWidth = 140f
    private val categoryRectHeightBase = 16f  // 减少分类标题高度
    private val categoryRectHeight: Float
        get() = categoryRectHeightBase + (ClickGUI.fdpradius * 1.5f)  // 减少圆角影响
    
    var animation: Animation? = null
    var openingAnimation: Animation? = null

    private var moduleAnimMap = HashMap<ModuleRect, Animation>()
    private var moduleRects: MutableList<ModuleRect>? = null

    override fun initGui() {
        if (moduleRects == null) {
            moduleRects = mutableListOf<ModuleRect>().apply {
                Main.getModulesInCategory(category)
                    .sortedBy { it.name }
                    .forEach { module ->
                        val moduleRect = ModuleRect(module)
                        add(moduleRect)
                        moduleAnimMap[moduleRect] = DecelerateAnimation(300, 1.0)
                    }
            }
        }
        moduleRects?.forEach { it.initGui() }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        moduleRects?.forEach { it.keyTyped(typedChar, keyCode) }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int) {
        try {
            val animOutput: Double = (animation?.output ?: 0.0)
            val alphaAnimation: Int = (255.0 * animOutput).toInt().coerceIn(0, 255)
            if (alphaAnimation <= 0) return

            val radius: Float = ClickGUI.fdpradius

            // Category bar background color - significantly reduced opacity for transparency
            val categoryRectColor = if (ClickGUI.fdpheadercolor) {
                val themeColor = ClickGUI.generateColor(0) // 使用ClickGUI中的generateColor方法
                Color(themeColor.red, themeColor.green, themeColor.blue, (alphaAnimation * 0.45f).toInt()).rgb
            } else {
                Color(20, 20, 20, (alphaAnimation * 0.5f).toInt()).rgb
            }
            val textColor = Color(255, 255, 255, alphaAnimation).rgb

            val drag = FDPState.getDrag(category)
            val x = drag.x
            val y = drag.y

            drag.onDraw(mouseX, mouseY)

            val allowedHeight: Float = if (ClickGUI.fdpscrollmode == "Value") {
                ClickGUI.fdpch.toFloat()
            } else {
                val sr = ScaledResolution(mc)
                (2f * sr.scaledHeight.toFloat() / 3f)
            }
            Main.allowedClickGuiHeight = allowedHeight

            var totalContentHeight = 0.0
            moduleRects?.forEach { moduleRect ->
                val mAnim = moduleAnimMap[moduleRect]
                val mAnimOut = mAnim?.output ?: 0.0
                totalContentHeight += 1.0 + (moduleRect.settingComponents.settingSize * mAnimOut)
            }
            val finalDisplayHeight: Float = min(allowedHeight, (totalContentHeight * 20.0).toFloat())

            // --- Background Blur ---
            val blurEnabled = ClickGUI.fdpblur
            val blurStr = ClickGUI.fdpblurstrength
            
            if (blurEnabled && alphaAnimation > 0 && finalDisplayHeight > 0f) {
                StencilUtil.initStencilToWrite()
                try {
                    RenderUtils.drawRoundedRect(x, y, x + rectWidth, y + categoryRectHeight, -1, radius, RenderUtils.RoundedCorners.ALL)
                    StencilUtil.readStencilBuffer(1)
                    BlurUtils.blurArea(x, y, rectWidth, categoryRectHeight + finalDisplayHeight, blurStr)
                } finally {
                    StencilUtil.uninitStencilBuffer()
                }
            }

            RenderUtils.drawRoundedRect(x, y, x + rectWidth, y + categoryRectHeight, categoryRectColor, radius, RenderUtils.RoundedCorners.TOP_ONLY)

            DrRenderUtils.resetColor()
            Fonts.fontGoogleSans40.drawString(category.name, x + 5f, y + getMiddleOfBox(Fonts.fontGoogleSans40.height, categoryRectHeight), textColor, false)

            val iconSize = 16
            try {
                RenderUtils.drawImage(category.iconResourceLocation, (x + rectWidth - 21f).toInt(), (y + getMiddleOfBox(iconSize, categoryRectHeight)).toInt(), iconSize, iconSize)
            } catch (e: Exception) {}

            if (category.name.equals("Client", ignoreCase = true)) {
                Fonts.fontGoogleSans40.drawString("b", x + rectWidth.toInt() - (Fonts.fontGoogleSans40.getStringWidth("b") + 5f),
                    y + getMiddleOfBox(Fonts.fontGoogleSans40.height, categoryRectHeight), textColor, false)
            }

            val hoveringMods = DrRenderUtils.isHovering(x, y + categoryRectHeight, rectWidth, finalDisplayHeight, mouseX, mouseY)

            StencilUtil.initStencilToWrite()
            try {
                RenderUtils.drawRoundedRect(x, y + categoryRectHeight, x + rectWidth, y + categoryRectHeight + finalDisplayHeight, -1, radius, RenderUtils.RoundedCorners.BOTTOM_ONLY)
                StencilUtil.readStencilBuffer(1)

                val scrollObj = FDPState.getScroll(category)
                val scroll = scrollObj.scroll.toDouble()
                var count = 0.0

                moduleRects?.forEach { moduleRect ->
                    try {
                        val mAnim = moduleAnimMap[moduleRect]
                        val mAnimOut: Double = (mAnim?.output ?: 0.0)
                        moduleAnimMap[moduleRect]?.direction = if (FDPState.isExpanded(moduleRect.module)) Direction.FORWARDS else Direction.BACKWARDS

                        moduleRect.settingAnimation = mAnim
                        moduleRect.alphaAnimation = alphaAnimation
                        moduleRect.x = x
                        moduleRect.height = 20f
                        moduleRect.panelLimitY = y
                        moduleRect.openingAnimation = openingAnimation
                        moduleRect.y = (y + categoryRectHeight + (count * 18.0) + (roundToHalf(scroll) as Double)).toFloat()
                        moduleRect.width = rectWidth

                        moduleRect.drawScreen(mouseX, mouseY)
                        count += 1.0 + moduleRect.settingSize
                    } catch (e: Exception) {}
                }

                if (hoveringMods) {
                    scrollObj.onScroll(30)
                    val hiddenHeight = ((count * 18.0) - finalDisplayHeight.toDouble())
                    scrollObj.maxScroll = max(0.0, hiddenHeight).toFloat()
                }
            } finally {
                StencilUtil.uninitStencilBuffer()
            }
        } catch (e: Throwable) {
            println("Critical error in DropdownCategory: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun getMiddleOfBox(height: Int, boxHeight: Float): Float = (boxHeight - height.toFloat()) / 2f
    
    private fun roundToHalf(d: Double): Double = (d * 2.0).roundToInt() / 2.0 // Corrected this, it was using .toInt() / 2.0

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int) {
        val drag = FDPState.getDrag(category)
        val canDrag = DrRenderUtils.isHovering(drag.x, drag.y, rectWidth, categoryRectHeight, mouseX, mouseY)
        drag.onClick(mouseX, mouseY, button, canDrag)
        moduleRects?.forEach { it.mouseClicked(mouseX, mouseY, button) }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        FDPState.getDrag(category).onRelease(state)
        moduleRects?.forEach { it.mouseReleased(mouseX, mouseY, state) }
    }
}