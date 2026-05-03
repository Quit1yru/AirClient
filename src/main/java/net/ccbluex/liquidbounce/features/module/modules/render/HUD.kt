/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Element.Companion.MAX_GRADIENT_COLORS
import net.ccbluex.liquidbounce.utils.render.ColorSettingsFloat
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.ResourceLocation
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object HUD : Module("HUD", Category.RENDER, gameDetecting = false, defaultState = true, defaultHidden = true) {
    val customHotbar by boolean("CustomHotbar", true)

    val smoothHotbarSlot by boolean("SmoothHotbarSlot", true) { customHotbar }

    val roundedHotbarRadius by float("RoundedHotbar-Radius", 3F, 0F..5F) { customHotbar }

    val hotbarMode by choices("Hotbar-Color", arrayOf("Custom", "Rainbow", "Gradient"), "Custom") { customHotbar }
    val hbHighlightColors = ColorSettingsInteger(this, "Hotbar-Highlight-Colors", applyMax = true)
    { customHotbar }.with(a = 0)
    val hbBackgroundColors = ColorSettingsInteger(this, "Hotbar-Background-Colors")
    { customHotbar && hotbarMode == "Custom" }.with(a = 190)
    val gradientHotbarSpeed by float("Hotbar-Gradient-Speed", 1f, 0.5f..10f)
    { customHotbar && hotbarMode == "Gradient" }
    val maxHotbarGradientColors by int("Max-Hotbar-Gradient-Colors", 4, 1..MAX_GRADIENT_COLORS)
    { customHotbar && hotbarMode == "Gradient" }
    val bgGradColors = ColorSettingsFloat.create(this, "Hotbar-Gradient")
    { customHotbar && hotbarMode == "Gradient" && it <= maxHotbarGradientColors }
    val hbHighlightBorder by float("HotbarBorder-Highlight-Width", 2F, 0.5F..5F) { customHotbar }
    val hbHighlightBorderColors = ColorSettingsInteger(this, "HotbarBorder-Highlight-Colors")
    { customHotbar }.with(a = 255, g = 111, b = 255)
    val hbBackgroundBorder by float("HotbarBorder-Background-Width", 0.5F, 0.5F..5F) { customHotbar }
    val hbBackgroundBorderColors = ColorSettingsInteger(this, "HotbarBorder-Background-Colors")
    { customHotbar }.with(a = 0)

    val rainbowX by float("Rainbow-X", -1000F, -2000F..2000F) { customHotbar && hotbarMode == "Rainbow" }
    val rainbowY by float("Rainbow-Y", -1000F, -2000F..2000F) { customHotbar && hotbarMode == "Rainbow" }
    val gradientX by float("Gradient-X", -1000F, -2000F..2000F) { customHotbar && hotbarMode == "Gradient" }
    val gradientY by float("Gradient-Y", -1000F, -2000F..2000F) { customHotbar && hotbarMode == "Gradient" }

    val inventoryParticle by boolean("InventoryParticle", false)
    private val blur by boolean("Blur", false)
    private val fontChat by choices("FontChat", arrayOf("Off", "Minecraft", "Regular35", "Regular40", "Semibold35", "Semibold40"), "Minecraft")
    val chatCombine by boolean("ChatCombine", true)
    val chatAnimation by boolean("ChatAnimation", true)
    val chatAnimationSpeed by float("Chat-AnimationSpeed", 0.1f, 0.01f..1.0f) { chatAnimation }
    val chatRect by boolean("ChatRect", true) { chatAnimation }

    val customHealthBar by boolean("自定义血条", true)
    val healthBarStyle by choices("血条样式", arrayOf(
        "经典", "圆角", "渐变", "心形", "数字", "百分比",
        "彩虹", "霓虹", "极简", "动态", "分段", "双色"
    ), "经典") { customHealthBar }
    val healthBarWidth by float("血条宽度", 100F, 50F..200F) { customHealthBar }
    val healthBarHeight by float("血条高度", 10F, 5F..20F) { customHealthBar }
    val healthBarOffsetX by float("血条X偏移", 0F, -500F..500F) { customHealthBar }
    val healthBarOffsetY by float("血条Y偏移", 0F, -500F..500F) { customHealthBar }
    val healthColor1 by color("血条颜色1", Color(255, 50, 50)) { customHealthBar }
    val healthColor2 by color("血条颜色2", Color(255, 100, 100)) { customHealthBar && healthBarStyle == "渐变" }
    val healthBgColor by color("血条背景色", Color(50, 50, 50, 150)) { customHealthBar }
    val healthText by boolean("显示血量文字", true) { customHealthBar && healthBarStyle != "数字" && healthBarStyle != "百分比" }
    val healthTextShadow by boolean("血量文字阴影", true) { customHealthBar && healthText }
    val healthRoundedRadius by float("血条圆角", 3F, 0F..10F) { customHealthBar }
    val healthBorderWidth by float("血条边框粗细", 1F, 0F..3F) { customHealthBar }
    val healthBorderColor by color("血条边框颜色", Color(0, 0, 0, 100)) { customHealthBar && healthBorderWidth > 0 }
    val healthAnimationSpeed by float("血条动画速度", 2F, 0.5F..5F) { customHealthBar }

    val customFoodBar by boolean("自定义饥饿值", true)
    val foodBarStyle by choices("饥饿值样式", arrayOf(
        "经典", "圆角", "渐变", "数字", "百分比",
        "彩虹", "霓虹", "极简", "分段", "图标"
    ), "经典") { customFoodBar }
    val foodBarWidth by float("饥饿值宽度", 100F, 50F..200F) { customFoodBar }
    val foodBarHeight by float("饥饿值高度", 10F, 5F..20F) { customFoodBar }
    val foodBarOffsetX by float("饥饿值X偏移", 0F, -500F..500F) { customFoodBar }
    val foodBarOffsetY by float("饥饿值Y偏移", 0F, -500F..500F) { customFoodBar }
    val foodColor1 by color("饥饿值颜色1", Color(139, 90, 43)) { customFoodBar }
    val foodColor2 by color("饥饿值颜色2", Color(194, 124, 57)) { customFoodBar && foodBarStyle == "渐变" }
    val foodBgColor by color("饥饿值背景色", Color(50, 50, 50, 150)) { customFoodBar }
    val foodText by boolean("显示饥饿值文字", true) { customFoodBar && foodBarStyle != "数字" && foodBarStyle != "百分比" }
    val foodTextShadow by boolean("饥饿值文字阴影", true) { customFoodBar && foodText }
    val foodRoundedRadius by float("饥饿值圆角", 3F, 0F..10F) { customFoodBar }
    val foodBorderWidth by float("饥饿值边框粗细", 1F, 0F..3F) { customFoodBar }
    val foodBorderColor by color("饥饿值边框颜色", Color(0, 0, 0, 100)) { customFoodBar && foodBorderWidth > 0 }
    val foodAnimationSpeed by float("饥饿值动画速度", 2F, 0.5F..5F) { customFoodBar }

    private var displayHealth = 0f
    private var lastHealth = 0f
    private var displayFood = 0f
    private var lastFood = 0

    private val ICONS = ResourceLocation("textures/gui/icons.png")

    override fun onEnable() {
        MinecraftForge.EVENT_BUS.register(this)
    }

    override fun onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this)
    }

    @SubscribeEvent
    fun onRenderGameOverlay(event: RenderGameOverlayEvent.Pre) {
        if (!handleEvents()) return

        val player = mc.thePlayer ?: return
        val resolution = ScaledResolution(mc)
        val width = resolution.scaledWidth
        val height = resolution.scaledHeight

        if (event.type == RenderGameOverlayEvent.ElementType.HEALTH && customHealthBar) {
            event.isCanceled = true
            renderCustomHealthBar(player, width, height)
        }

        if (event.type == RenderGameOverlayEvent.ElementType.FOOD && customFoodBar) {
            event.isCanceled = true
            renderCustomFoodBar(player, width, height)
        }
    }

    private fun renderCustomHealthBar(player: net.minecraft.entity.player.EntityPlayer, width: Int, height: Int) {
        val health = player.health
        val maxHealth = player.maxHealth
        val absorption = player.absorptionAmount

        if (abs(health - lastHealth) > 0.5f || displayHealth == 0f) {
            displayHealth = health
        } else {
            displayHealth += (health - displayHealth) * healthAnimationSpeed * 0.1f
        }
        lastHealth = health

        val healthPercent = max(0f, min(1f, displayHealth / maxHealth))
        val barX = width / 2f - 91f + healthBarOffsetX
        val barY = height - 39f + healthBarOffsetY
        val barWidth = healthBarWidth
        val barHeight = healthBarHeight
        val style = healthBarStyle
        val color1 = healthColor1
        val color2 = healthColor2
        val bgColor = healthBgColor
        val radius = healthRoundedRadius
        val borderWidth = healthBorderWidth
        val borderColor = healthBorderColor

        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        val healthWidth = healthPercent * barWidth

        when (style) {
            "经典" -> {
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                if (healthWidth > 0) {
                    RenderUtils.drawGradientRect(barX.toInt(), barY.toInt(), (barX + healthWidth).toInt(), (barY + barHeight).toInt(), color1.rgb, color2.rgb, 0f)
                }
                if (absorption > 0) {
                    val absWidth = (absorption / maxHealth) * barWidth
                    RenderUtils.drawRoundedRect(barX + healthWidth, barY, barX + healthWidth + absWidth, barY + barHeight, Color(255, 200, 50).rgb, radius, RenderUtils.RoundedCorners.ALL)
                }
                if (borderWidth > 0) {
                    RenderUtils.drawRoundedBorder(barX, barY, barX + barWidth, barY + barHeight, borderWidth, borderColor.rgb, radius)
                }
            }
            "圆角" -> {
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                if (healthWidth > 0) {
                    RenderUtils.drawRoundedRect(barX, barY, barX + healthWidth, barY + barHeight, color1.rgb, radius, RenderUtils.RoundedCorners.ALL)
                }
                if (borderWidth > 0) {
                    RenderUtils.drawRoundedBorder(barX, barY, barX + barWidth, barY + barHeight, borderWidth, borderColor.rgb, radius)
                }
            }
            "渐变" -> {
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                if (healthWidth > 0) {
                    RenderUtils.drawGradientRect(barX.toInt(), barY.toInt(), (barX + healthWidth).toInt(), (barY + barHeight).toInt(), color1.rgb, color2.rgb, 0f)
                }
                if (borderWidth > 0) {
                    RenderUtils.drawRoundedBorder(barX, barY, barX + barWidth, barY + barHeight, borderWidth, borderColor.rgb, radius)
                }
            }
            "心形" -> {
                mc.textureManager.bindTexture(ICONS)
                GL11.glColor4f(1f, 1f, 1f, 1f)
                val maxHearts = ceil(maxHealth / 2f).toInt()
                val heartSize = 9
                val spacing = 2

                for (i in 0 until maxHearts) {
                    val heartX = barX + i * (heartSize + spacing)
                    val heartY = barY
                    val healthForHeart = health - i * 2

                    Gui.drawModalRectWithCustomSizedTexture(heartX.toInt(), heartY.toInt(), 16f, 0f, 9, 9, 256f, 256f)
                    if (healthForHeart >= 2) {
                        Gui.drawModalRectWithCustomSizedTexture(heartX.toInt(), heartY.toInt(), 52f, 0f, 9, 9, 256f, 256f)
                    } else if (healthForHeart >= 1) {
                        Gui.drawModalRectWithCustomSizedTexture(heartX.toInt(), heartY.toInt(), 61f, 0f, 9, 9, 256f, 256f)
                    }
                }
            }
            "数字" -> {
                val healthTextStr = "${health.toInt()}/${maxHealth.toInt()}"
                val absorptionText = if (absorption > 0) " +${absorption.toInt()}" else ""
                val numColor = when {
                    health <= maxHealth * 0.25 -> Color(255, 50, 50)
                    health <= maxHealth * 0.5 -> Color(255, 200, 50)
                    else -> Color(50, 255, 50)
                }
                Fonts.fontSemibold40.drawString(healthTextStr, barX, barY, numColor.rgb, healthTextShadow)
                val offset = Fonts.fontSemibold40.getStringWidth(healthTextStr)
                Fonts.fontSemibold40.drawString("/${maxHealth.toInt()}", barX + offset + 2, barY, Color.GRAY.rgb, healthTextShadow)
                if (absorption > 0) {
                    Fonts.fontSemibold40.drawString(absorptionText, barX + offset + Fonts.fontSemibold40.getStringWidth("/${maxHealth.toInt()}") + 4, barY, Color(255, 200, 50).rgb, healthTextShadow)
                }
            }
            "百分比" -> {
                val percentage = (healthPercent * 100).toInt()
                val pctText = "$percentage%"
                val pctColor = when {
                    percentage <= 25 -> Color(255, 50, 50)
                    percentage <= 50 -> Color(255, 200, 50)
                    else -> Color(50, 255, 50)
                }
                Fonts.fontSemibold40.drawString(pctText, barX, barY, pctColor.rgb, healthTextShadow)
            }
            "彩虹" -> {
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                if (healthWidth > 0) {
                    val rainbowSpeed = 3000f
                    for (i in 0 until healthWidth.toInt()) {
                        val hue = (System.currentTimeMillis() % rainbowSpeed.toInt()) / rainbowSpeed + i / 200f
                        val rainbowColor = Color.getHSBColor(hue % 1f, 0.8f, 1f)
                        RenderUtils.drawRect(barX + i, barY, barX + i + 1, barY + barHeight, rainbowColor.rgb)
                    }
                }
                if (borderWidth > 0) {
                    RenderUtils.drawRoundedBorder(barX, barY, barX + barWidth, barY + barHeight, borderWidth, borderColor.rgb, radius)
                }
            }
            "霓虹" -> {
                for (i in 3 downTo 1) {
                    val alpha = 50 * i
                    val glowColor = Color(color1.red, color1.green, color1.blue, alpha)
                    RenderUtils.drawRoundedRect(barX - i, barY - i, barX + barWidth + i, barY + barHeight + i, glowColor.rgb, radius + i, RenderUtils.RoundedCorners.ALL)
                }
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                if (healthWidth > 0) {
                    for (i in 2 downTo 1) {
                        val alpha = 100 * i
                        val glowColor = Color(color1.red, color1.green, color1.blue, alpha)
                        RenderUtils.drawRoundedRect(barX - i, barY - i, barX + healthWidth + i, barY + barHeight + i, glowColor.rgb, radius + i, RenderUtils.RoundedCorners.ALL)
                    }
                    RenderUtils.drawRoundedRect(barX, barY, barX + healthWidth, barY + barHeight, color1.rgb, radius, RenderUtils.RoundedCorners.ALL)
                }
            }
            "极简" -> {
                RenderUtils.drawRect(barX, barY, barX + barWidth, barY + 2, Color(50, 50, 50, 150).rgb)
                if (healthWidth > 0) {
                    RenderUtils.drawRect(barX, barY, barX + healthWidth, barY + 2, color1.rgb)
                }
            }
            "动态" -> {
                val time = System.currentTimeMillis() % 2000 / 2000f
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                if (healthWidth > 0) {
                    val shimmerWidth = 30f
                    val shimmerX = (time * (barWidth + shimmerWidth)) - shimmerWidth

                    RenderUtils.drawRoundedRect(barX, barY, barX + healthWidth, barY + barHeight, color1.rgb, radius, RenderUtils.RoundedCorners.ALL)

                    GL11.glColor4f(1f, 1f, 1f, 0.3f)
                    RenderUtils.drawRoundedRect(
                        barX + max(0f, min(shimmerX, healthWidth)),
                        barY,
                        min(barX + healthWidth, barX + shimmerX + shimmerWidth),
                        barY + barHeight,
                        Color.WHITE.rgb,
                        radius,
                        RenderUtils.RoundedCorners.ALL
                    )
                }
                if (borderWidth > 0) {
                    RenderUtils.drawRoundedBorder(barX, barY, barX + barWidth, barY + barHeight, borderWidth, borderColor.rgb, radius)
                }
            }
            "分段" -> {
                val segments = 10
                val segmentWidth = barWidth / segments - 2
                val healthPerSegment = maxHealth / segments

                for (i in 0 until segments) {
                    val segmentX = barX + i * (segmentWidth + 2)
                    RenderUtils.drawRoundedRect(segmentX, barY, segmentX + segmentWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)

                    val segmentHealth = health - i * healthPerSegment
                    if (segmentHealth > 0) {
                        val fillPercent = min(1f, segmentHealth / healthPerSegment)
                        val segColor = when {
                            i < segments * 0.3 -> Color(50, 255, 50)
                            i < segments * 0.6 -> Color(255, 200, 50)
                            else -> Color(255, 50, 50)
                        }
                        RenderUtils.drawRoundedRect(segmentX, barY, segmentX + segmentWidth * fillPercent, barY + barHeight, segColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                    }
                }
            }
            "双色" -> {
                val halfWidth = barWidth / 2
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                if (healthWidth > 0) {
                    if (healthWidth <= halfWidth) {
                        RenderUtils.drawRoundedRect(barX, barY, barX + healthWidth, barY + barHeight, color1.rgb, radius, RenderUtils.RoundedCorners.ALL)
                    } else {
                        RenderUtils.drawRoundedRect(barX, barY, barX + halfWidth, barY + barHeight, color1.rgb, radius, RenderUtils.RoundedCorners.ALL)
                        RenderUtils.drawRoundedRect(barX + halfWidth, barY, barX + healthWidth, barY + barHeight, color2.rgb, radius, RenderUtils.RoundedCorners.ALL)
                    }
                }
                if (borderWidth > 0) {
                    RenderUtils.drawRoundedBorder(barX, barY, barX + barWidth, barY + barHeight, borderWidth, borderColor.rgb, radius)
                }
            }
        }

        if (healthText && style != "数字" && style != "百分比") {
            val text = "${health.toInt()}/${maxHealth.toInt()}"
            val textX = barX + barWidth / 2 - Fonts.fontRegular35.getStringWidth(text) / 2
            val textY = barY + barHeight / 2 - Fonts.fontRegular35.FONT_HEIGHT / 2
            Fonts.fontRegular35.drawString(text, textX, textY, Color.WHITE.rgb, healthTextShadow)
        }

        GL11.glPopMatrix()
    }

    private fun renderCustomFoodBar(player: net.minecraft.entity.player.EntityPlayer, width: Int, height: Int) {
        val foodLevel = player.foodStats.foodLevel
        val maxFood = 20f

        if (abs(foodLevel - lastFood) > 1 || displayFood == 0f) {
            displayFood = foodLevel.toFloat()
        } else {
            displayFood += (foodLevel - displayFood) * foodAnimationSpeed * 0.1f
        }
        lastFood = foodLevel

        val foodPercent = max(0f, min(1f, displayFood / maxFood))
        val barX = width / 2f + 91f - foodBarWidth + foodBarOffsetX
        val barY = height - 39f + foodBarOffsetY
        val barWidth = foodBarWidth
        val barHeight = foodBarHeight
        val style = foodBarStyle
        val color1 = foodColor1
        val color2 = foodColor2
        val bgColor = foodBgColor
        val radius = foodRoundedRadius
        val borderWidth = foodBorderWidth
        val borderColor = foodBorderColor

        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        val foodWidth = foodPercent * barWidth

        when (style) {
            "经典" -> {
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                if (foodWidth > 0) {
                    RenderUtils.drawGradientRect(barX.toInt(), barY.toInt(), (barX + foodWidth).toInt(), (barY + barHeight).toInt(), color1.rgb, color2.rgb, 0f)
                }
                if (borderWidth > 0) {
                    RenderUtils.drawRoundedBorder(barX, barY, barX + barWidth, barY + barHeight, borderWidth, borderColor.rgb, radius)
                }
            }
            "圆角" -> {
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                if (foodWidth > 0) {
                    RenderUtils.drawRoundedRect(barX, barY, barX + foodWidth, barY + barHeight, color1.rgb, radius, RenderUtils.RoundedCorners.ALL)
                }
                if (borderWidth > 0) {
                    RenderUtils.drawRoundedBorder(barX, barY, barX + barWidth, barY + barHeight, borderWidth, borderColor.rgb, radius)
                }
            }
            "渐变" -> {
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                if (foodWidth > 0) {
                    RenderUtils.drawGradientRect(barX.toInt(), barY.toInt(), (barX + foodWidth).toInt(), (barY + barHeight).toInt(), color1.rgb, color2.rgb, 0f)
                }
                if (borderWidth > 0) {
                    RenderUtils.drawRoundedBorder(barX, barY, barX + barWidth, barY + barHeight, borderWidth, borderColor.rgb, radius)
                }
            }
            "数字" -> {
                val foodTextStr = "${displayFood.toInt()}/${maxFood.toInt()}"
                val numColor = when {
                    displayFood <= maxFood * 0.25 -> Color(255, 50, 50)
                    displayFood <= maxFood * 0.5 -> Color(255, 200, 50)
                    else -> color1
                }
                Fonts.fontSemibold40.drawString(foodTextStr, barX, barY, numColor.rgb, foodTextShadow)
            }
            "百分比" -> {
                val percentage = (foodPercent * 100).toInt()
                val pctText = "$percentage%"
                val pctColor = when {
                    percentage <= 25 -> Color(255, 50, 50)
                    percentage <= 50 -> Color(255, 200, 50)
                    else -> color1
                }
                Fonts.fontSemibold40.drawString(pctText, barX, barY, pctColor.rgb, foodTextShadow)
            }
            "彩虹" -> {
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                if (foodWidth > 0) {
                    val rainbowSpeed = 3000f
                    for (i in 0 until foodWidth.toInt()) {
                        val hue = (System.currentTimeMillis() % rainbowSpeed.toInt()) / rainbowSpeed + i / 200f + 0.1f
                        val rainbowColor = Color.getHSBColor(hue % 1f, 0.8f, 1f)
                        RenderUtils.drawRect(barX + i, barY, barX + i + 1, barY + barHeight, rainbowColor.rgb)
                    }
                }
                if (borderWidth > 0) {
                    RenderUtils.drawRoundedBorder(barX, barY, barX + barWidth, barY + barHeight, borderWidth, borderColor.rgb, radius)
                }
            }
            "霓虹" -> {
                for (i in 3 downTo 1) {
                    val alpha = 50 * i
                    val glowColor = Color(color1.red, color1.green, color1.blue, alpha)
                    RenderUtils.drawRoundedRect(barX - i, barY - i, barX + barWidth + i, barY + barHeight + i, glowColor.rgb, radius + i, RenderUtils.RoundedCorners.ALL)
                }
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                if (foodWidth > 0) {
                    for (i in 2 downTo 1) {
                        val alpha = 100 * i
                        val glowColor = Color(color1.red, color1.green, color1.blue, alpha)
                        RenderUtils.drawRoundedRect(barX - i, barY - i, barX + foodWidth + i, barY + barHeight + i, glowColor.rgb, radius + i, RenderUtils.RoundedCorners.ALL)
                    }
                    RenderUtils.drawRoundedRect(barX, barY, barX + foodWidth, barY + barHeight, color1.rgb, radius, RenderUtils.RoundedCorners.ALL)
                }
            }
            "极简" -> {
                RenderUtils.drawRect(barX, barY, barX + barWidth, barY + 2, Color(50, 50, 50, 150).rgb)
                if (foodWidth > 0) {
                    RenderUtils.drawRect(barX, barY, barX + foodWidth, barY + 2, color1.rgb)
                }
            }
            "分段" -> {
                val segments = 10
                val segmentWidth = barWidth / segments - 2
                val foodPerSegment = maxFood / segments

                for (i in 0 until segments) {
                    val segmentX = barX + i * (segmentWidth + 2)
                    RenderUtils.drawRoundedRect(segmentX, barY, segmentX + segmentWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)

                    val segmentFood = displayFood - i * foodPerSegment
                    if (segmentFood > 0) {
                        val fillPercent = min(1f, segmentFood / foodPerSegment)
                        val segColor = when {
                            i < segments * 0.3 -> color1
                            i < segments * 0.6 -> color2
                            else -> Color(255, 150, 50)
                        }
                        RenderUtils.drawRoundedRect(segmentX, barY, segmentX + segmentWidth * fillPercent, barY + barHeight, segColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                    }
                }
            }
            "图标" -> {
                mc.textureManager.bindTexture(ICONS)
                GL11.glColor4f(1f, 1f, 1f, 1f)
                val iconCount = 10
                val iconSize = 9
                val spacing = 2

                for (i in 0 until iconCount) {
                    val iconX = barX + i * (iconSize + spacing)
                    val foodForIcon = displayFood - i * 2
                    val isFull = foodForIcon >= 2
                    val isHalf = foodForIcon >= 1

                    Gui.drawModalRectWithCustomSizedTexture(iconX.toInt(), barY.toInt(), 16f, 27f, 9, 9, 256f, 256f)

                    if (isFull) {
                        Gui.drawModalRectWithCustomSizedTexture(iconX.toInt(), barY.toInt(), 52f, 27f, 9, 9, 256f, 256f)
                    } else if (isHalf) {
                        Gui.drawModalRectWithCustomSizedTexture(iconX.toInt(), barY.toInt(), 61f, 27f, 9, 9, 256f, 256f)
                    }
                }
            }
        }

        if (foodText && style != "数字" && style != "百分比") {
            val text = "${displayFood.toInt()}/${maxFood.toInt()}"
            val textX = barX + barWidth / 2 - Fonts.fontRegular35.getStringWidth(text) / 2
            val textY = barY + barHeight / 2 - Fonts.fontRegular35.FONT_HEIGHT / 2
            Fonts.fontRegular35.drawString(text, textX, textY, Color.WHITE.rgb, foodTextShadow)
        }

        GL11.glPopMatrix()
    }

    val onRender2D = handler<Render2DEvent> {
        if (mc.currentScreen is GuiHudDesigner)
            return@handler

        hud.render(false)
    }

    val onUpdate = handler<UpdateEvent> {
        hud.update()
    }

    val onKey = handler<KeyEvent> { event ->
        hud.handleKey('a', event.key)
    }

    val onScreen = handler<ScreenEvent>(always = true) { event ->
        if (mc.theWorld == null || mc.thePlayer == null) return@handler
        if (state && blur && !mc.entityRenderer.isShaderActive && event.guiScreen != null &&
            !(event.guiScreen is GuiChat || event.guiScreen is GuiHudDesigner)
        ) mc.entityRenderer.loadShader(
            ResourceLocation(CLIENT_NAME.lowercase() + "/blur.json")
        ) else if (mc.entityRenderer.shaderGroup != null &&
            "airclient/blur.json" in mc.entityRenderer.shaderGroup.shaderGroupName
        ) mc.entityRenderer.stopUseShader()
    }

    fun shouldModifyChatFont() = handleEvents() && fontChat != "Off"

    fun getChatFont() = when (fontChat) {
        "Minecraft" -> Fonts.minecraftFont
        "Regular35" -> Fonts.fontRegular35
        "Regular40" -> Fonts.fontRegular40
        "Semibold35" -> Fonts.fontSemibold35
        "Semibold40" -> Fonts.fontSemibold40
        else -> Fonts.fontSemibold40
    }
}
