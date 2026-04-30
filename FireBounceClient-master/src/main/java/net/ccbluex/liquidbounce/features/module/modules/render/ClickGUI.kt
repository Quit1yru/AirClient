/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.FireBounce.clickGui
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.*
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.FDPDropdownClickGUI
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.minecraft.network.play.server.S2EPacketCloseWindow
import org.lwjgl.input.Keyboard
import java.awt.Color

object ClickGUI : Module("ClickGUI", Category.RENDER, Keyboard.KEY_RSHIFT, canBeEnabled = false) {
    private val fdpDropdownClickGui = FDPDropdownClickGUI()

    private val style by choices(
        "Style",
        arrayOf("LiquidBounce","LiquidBounce2", "Null", "Slowly", "Black", "New", "FDPDropdown"),
        "LiquidBounce2"
    ).onChanged {
        updateStyle()
    }
    var scale by float("Scale", 0.8f, 0.5f..1.5f)
    val maxElements by int("MaxElements", 15, 1..30)
    val fadeSpeed by float("FadeSpeed", 1f, 0.5f..4f)
    val scrolls by boolean("Scrolls", true)
    val spacedModules by boolean("SpacedModules", false)
    val panelsForcedInBoundaries by boolean("PanelsForcedInBoundaries", false)

    private val color by color("Color", Color(0, 160, 255)) { style !in arrayOf("Slowly", "Black") }

    // FDP Dropdown specific settings
    var fdpscale by float("FDPScale", 1.0f, 0.5f..2.0f) { style == "FDPDropDown" }
    private val fdpcolor by color("FDPColor", Color(0, 160, 255)) { style == "FDPDropDown" }
    val fdpcm by choices("FDPColorMode", arrayOf("Custom", "Rainbow", "Sky", "Fade", "Mix"), "Custom") { style == "FDPDropDown" }
    val fdpch by int("FDPClickHeight", 300, 100..1000) { style == "FDPDropDown" }
    val fdpscrollmode by choices("FDPScrollMode", arrayOf("Value", "Screen"), "Screen") { style == "FDPDropDown" }
    val fdpradius by float("FDPRoundedRectRadius", 3f, 0f..10f) { style == "FDPDropDown" }
    val fdpbgalpha by int("FDPBackgroundAlpha", 80, 0..255) { style == "FDPDropDown" }
    val fdpheadercolor by boolean("FDPHeaderColor", true) { style == "FDPDropDown" }
    val fdpblur by boolean("FDPBlur", true) { style == "FDPDropDown" }
    val fdpblurstrength by float("FDPBlurStrength", 10f, 0f..20f) { style == "FDPDropDown" }
    val fdpbackback by boolean("FDPBackBack", false){ style == "FDPDropDown" }

    val guiColor
        get() = color.rgb


    fun generateColor(offset: Int): Color {
        if (style.equals("FDPDropdown", ignoreCase = true)) {
            val mode = fdpcm
            val c = fdpcolor
            return when (mode) {
                "Rainbow" -> ColorUtils.rainbow(offset * 300L)
                "Sky" -> ColorUtils.rainbow(offset * 300L) 
                "Fade" -> ColorUtils.fade(c, offset, 100)
                "Mix" -> ColorUtils.rainbow(offset * 300L)
                else -> c
            }
        }
        return color
    }
    
    val fdpGuiColor: Int
        get() = generateColor(0).rgb

    override fun onEnable() {
        if (style.equals("FDPDropdown", ignoreCase = true)) {
            mc.displayGuiScreen(fdpDropdownClickGui)
            return
        }
        updateStyle()
        mc.displayGuiScreen(clickGui)
        Keyboard.enableRepeatEvents(true)
    }

    private fun updateStyle() {
        if (style.equals("FDPDropdown", ignoreCase = true)) return
        clickGui.style = when (style) {
            "LiquidBounce" -> LiquidBounceStyle
            "LiquidBounce2" -> LiquidBounceStyle2
            "Null" -> NullStyle
            "Slowly" -> SlowlyStyle
            "Black" -> BlackStyle
            "New" -> NewStyle
            else -> LiquidBounceStyle2
        }
    }

    val onPacket = handler<PacketEvent>(always = true) { event ->
        if (event.packet is S2EPacketCloseWindow && (mc.currentScreen is ClickGui || mc.currentScreen is FDPDropdownClickGUI)) {
            event.cancelEvent()
        }
    }
}
