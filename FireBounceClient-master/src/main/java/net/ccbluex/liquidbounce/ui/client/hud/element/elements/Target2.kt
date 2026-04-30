package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extra.ColorUtils
import net.ccbluex.liquidbounce.utils.render.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.withClipping
import net.ccbluex.liquidbounce.utils.render.StencilUtil
import net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting
import net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.*

@ElementInfo(name = "Target2")
class Target2 : Element("Target2") {
    private val hudStyle by choices(
        "Style",
        arrayOf(
            "Novoline",
            "Southside",
            "Styles",
            "Naven",
            "Exhibition",
            "Opai",
            "Augustus"
        ),
        "Novoline"
    )
    private val animSpeed by float("AnimationSpeed", 0.1F, 0.01F..0.5F)

    private val novolineColorMode by choices("Novoline-Color", arrayOf("Custom", "Health", "Rainbow"), "Health") { hudStyle == "Novoline" }
    private val novolineColorRed by int("Novoline-Red", 0, 0..255) { hudStyle == "Novoline" && novolineColorMode == "Custom" }
    private val novolineColorGreen by int("Novoline-Green", 120, 0..255) { hudStyle == "Novoline" && novolineColorMode == "Custom" }
    private val novolineColorBlue by int("Novoline-Blue", 255, 0..255) { hudStyle == "Novoline" && novolineColorMode == "Custom" }
    private val novolineColorSpec by boolean("Novoline-Gradient",true) {hudStyle == "Novoline"}
    private val novolineLeftColor by color("Novoline-left-Color", Color(0, 255, 150)) { novolineColorSpec }
    private val novolineRightColor by color("Novoline-right-Color", Color(10, 80, 120)) { novolineColorSpec }

    private val stylesShadow by boolean("Styles-Shadow", true) { hudStyle == "Styles" }

    private val opaiThemeColor by color("Opai-themeColor", Color(242,172,244)) { hudStyle == "Opai" }
    private val opaiBackGroundAlpha by int("Opai-BackGroundAlpha",120,0..255) { hudStyle == "Opai" }
    private val opaiShadowCheck by boolean("Opai-ShadowCheck",false) { hudStyle == "Opai" }
    private val opaiShadowStrengh by float("Opai-shadowStrengh",0.5f,0.0f..1.0f) { hudStyle == "Opai" && opaiShadowCheck }
    private val opaiVanishDelay by int("Opai-VanishDelay", 300, 0..500) { hudStyle == "Opai" }

    private val augustusBackgroundAlpha by int("Augustus-BackgroundAlpha", 60, 0..255) { hudStyle == "Augustus" }
    private val augustusGlowColor by color("Augustus-GlowColor", Color(0, 150, 255)) { hudStyle == "Augustus" }
    private val augustusGlowStrength by float("Augustus-GlowStrength", 0.3f, 0.0f..1.0f) { hudStyle == "Augustus" }
    private val augustusCornerRadius by float("Augustus-CornerRadius", 6f, 0f..20f) { hudStyle == "Augustus" }

    private val followTarget by boolean("FollowTarget", false)

    private val decimalFormat = DecimalFormat("0.0", DecimalFormatSymbols(Locale.ENGLISH))
    private var target: EntityLivingBase? = null
    private var lastTarget: EntityLivingBase? = null
    private var hue = 0.0f

    private var easingHealth = 0F
    private var southsideEasingHealth = 0F
    private var slideIn = 0F
    private var damageHealth = 0F
    private var stylesEasingHealth = 0F
    private var NavenEasingHealth = 0F

    private var opaiDelayCounter = 0
    private var opaiAnimX = 135F
    private var augustusEasingHealth = 0F

    private fun updateSouthsideEasingHealth(targetHealth: Float, maxHealth: Float) {
        // 添加对 NaN 和无穷大值的检查
        if (targetHealth.isNaN() || targetHealth.isInfinite() || maxHealth.isNaN() || maxHealth.isInfinite()) {
            return
        }
        
        val changeAmount = abs(southsideEasingHealth - targetHealth)
        var speed = 0.02f * deltaTime

        if (changeAmount > 5) {
            speed *= 2.0f
        } else if (changeAmount > 2) {
            speed *= 1.5f
        }

        if (abs(southsideEasingHealth - targetHealth) < 0.1f) {
            southsideEasingHealth = targetHealth
        } else if (southsideEasingHealth > targetHealth) {
            southsideEasingHealth -= min(speed * 1.2f, southsideEasingHealth - targetHealth)
        } else {
            southsideEasingHealth += min(speed, targetHealth - southsideEasingHealth)
        }
        southsideEasingHealth = southsideEasingHealth.coerceAtMost(maxHealth)
    }

    private fun renderSouthsideHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)

        val health = entity.health
        val maxHealth = entity.maxHealth
        val healthPercent = (health / maxHealth).coerceIn(0f, 1f)

        updateSouthsideEasingHealth(health, maxHealth)
        val easingHealthPercent = (southsideEasingHealth / maxHealth).coerceIn(0f, 1f)

        val name = entity.name
        val width = Fonts.fontSemibold40.getStringWidth(name) + 75f
        val presentWidth = easingHealthPercent * width
        val height = 40f

        GlStateManager.pushMatrix()

        val animOutput = slideIn
        GlStateManager.translate((x + width / 2) * (1 - animOutput).toDouble(), (y + 20) * (1 - animOutput).toDouble(), 0.0)
        GlStateManager.scale(animOutput, animOutput, animOutput)

        RenderUtils.drawRect(x, y, x + width, y + height, Color(0, 0, 0, 100).rgb)
        RenderUtils.drawRect(x, y, x + presentWidth, y + height, Color(230, 230, 230, 100).rgb)

        val healthColor = when {
            healthPercent > 0.5 -> Color(63, 157, 4, 150)
            healthPercent > 0.25 -> Color(255, 144, 2, 150)
            else -> Color(168, 1, 1, 150)
        }
        RenderUtils.drawRect(x, y + 12.5f, x + 3, y + 27.5f, healthColor.rgb)

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            drawHead(it.locationSkin, x.toInt() + 7, y.toInt() + 7, 26, 26, Color.WHITE)
        } ?: RenderUtils.drawRect(x + 6, y + 6, x + 34, y + 34, Color.BLACK.rgb)

        Fonts.fontSemibold40.drawString(name, x + 40, y + 7, Color(200, 200, 200, 255).rgb)
        Fonts.fontSemibold40.drawString("${health.toInt()} HP", x + 40, y + 22, Color(200, 200, 200, 255).rgb)

        val itemStack = entity.heldItem
        val itemX = x + Fonts.fontSemibold40.getStringWidth(name) + 50
        if (itemStack != null) {
            GlStateManager.pushMatrix()
            GlStateManager.translate(itemX, y + 12, 0f)
            GlStateManager.scale(1.5f, 1.5f, 1.5f)
            enableGUIStandardItemLighting()
            mc.renderItem.renderItemAndEffectIntoGUI(itemStack, 0, 0)
            disableStandardItemLighting()
            GlStateManager.popMatrix()
        } else {
            Fonts.fontSemibold40.drawString("?", x + Fonts.fontSemibold40.getStringWidth(name) + 55, y + 11, Color(200, 200, 200, 255).rgb)
        }

        GlStateManager.popMatrix()
        return Border(x, y, x + width, y + height)
    }

    override fun drawElement(): Border? {
        val kaTarget = KillAura.target

        val isEditing = mc.currentScreen is net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner

        if (isEditing) {
            target = mc.thePlayer
        } else if (kaTarget != null && kaTarget is EntityPlayer && !AntiBot.isBot(kaTarget)) {
            target = kaTarget
        } else if (mc.currentScreen is GuiChat) {
            target = mc.thePlayer
        } else if (target != null && (KillAura.target == null || !target!!.isEntityAlive || AntiBot.isBot(target!!))) {
            target = null
        }

        if (target != lastTarget) {
            if (lastTarget != null) {
                // 确保初始值不是 NaN
                val lastTargetHealth = if (lastTarget!!.health.isNaN() || lastTarget!!.health.isInfinite()) 0f else lastTarget!!.health
                easingHealth = lastTargetHealth
                damageHealth = lastTargetHealth
                augustusEasingHealth = lastTargetHealth
            } else if (target != null) {
                val targetHealth = if (target!!.health.isNaN() || target!!.health.isInfinite()) 0f else target!!.health
                easingHealth = targetHealth
                damageHealth = targetHealth
                augustusEasingHealth = targetHealth
            }
            if (target != null) {
                val targetHealth = if (target!!.health.isNaN() || target!!.health.isInfinite()) 0f else target!!.health
                southsideEasingHealth = targetHealth
                NavenEasingHealth = targetHealth
            }
        }

        lastTarget = target

        hue += 0.05f * deltaTime * 0.1f
        if (hue > 1F) hue = 0F

        slideIn = lerp(slideIn, if (target != null) 1F else 0F, animSpeed)
        
        // 在更新 augustusEasingHealth 前确保目标存在且健康值有效
        if (target != null) {
            val targetHealth = if (target!!.health.isNaN() || target!!.health.isInfinite()) 0f else target!!.health
            augustusEasingHealth = lerp(augustusEasingHealth, targetHealth, animSpeed)
        } else {
            augustusEasingHealth = lerp(augustusEasingHealth, 0F, animSpeed)
        }

        val x = 0f
        val y = 0f

        val offsetX = if (followTarget && target != null && target != mc.thePlayer) {
            val screenPos = projectEntity(target ?: return null)
            if (screenPos != null) {
                screenPos[0] - x
            } else {
                0f
            }
        } else {
            0f
        }

        val offsetY = if (followTarget && target != null && target != mc.thePlayer) {
            val screenPos = projectEntity(target ?: return null)
            if (screenPos != null) {
                screenPos[1] - y
            } else {
                0f
            }
        } else {
            0f
        }

        if (slideIn < 0.01F && target == null) {
            val (defaultWidth, defaultHeight) = when (hudStyle.lowercase(Locale.getDefault())) {
                "novoline" -> 120f to 46f
                "southside" -> 150f to 40f
                "styles" -> 132f to 44f
                "naven" -> 130f to 50f
                "exhibition" -> 120f to 45f
                "opai" -> 150f to 50f
                "augustus" -> 150f to 40f
                else -> 135f to 35f
            }
            return Border(x, y, x + defaultWidth, y + defaultHeight)
        }

        return when (hudStyle.lowercase(Locale.getDefault())) {
            "novoline" -> renderNovolineHUD(x + offsetX, y + offsetY)
            "southside" -> renderSouthsideHUD(x + offsetX, y + offsetY)
            "styles" -> renderStylesHUD(x + offsetX, y + offsetY)
            "naven" -> renderNavenHUD(x + offsetX, y + offsetY)
            "exhibition" -> renderExhibitionHUD(x + offsetX, y + offsetY)
            "opai" -> renderOpaiHUD(x + offsetX, y + offsetY)
            "augustus" -> renderAugustusHUD(x + offsetX, y + offsetY)
            else -> renderNovolineHUD(x + offsetX, y + offsetY)
        }
    }

    private fun renderAugustusHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)

        val avatarSize = 32f
        val avatarPadding = 4f
        val healthBarWidth = 100f
        val healthBarHeight = 20f
        val nameHeight = Fonts.fontSemibold35.FONT_HEIGHT
        val verticalSpacing = 4f

        val totalWidth = avatarSize + avatarPadding * 2 + healthBarWidth + 8f
        val totalHeight = avatarSize + avatarPadding * 2

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)

        GlowUtils.drawGlow(
            0f, 0f,
            totalWidth, totalHeight,
            (augustusGlowStrength * 15F).toInt(),
            augustusGlowColor
        )

        drawRoundedRect(0f, 0f, totalWidth, totalHeight, Color(30, 30, 30, augustusBackgroundAlpha).rgb, augustusCornerRadius)

        RenderUtils.drawRoundedBorderRect(
            0f, 0f, totalWidth, totalHeight, 1f,
            Color(augustusGlowColor.red, augustusGlowColor.green, augustusGlowColor.blue, 150).rgb,
            Color(0, 0, 0, 0).rgb,
            augustusCornerRadius
        )

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            Stencil.write()
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            drawRoundedRect(avatarPadding, avatarPadding, avatarPadding + avatarSize, avatarPadding + avatarSize, Color.WHITE.rgb, augustusCornerRadius)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(it.locationSkin, avatarPadding.toInt(), avatarPadding.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            Stencil.dispose()
        }

        val name = entity.name
        val nameX = avatarSize + avatarPadding * 2
        val availableHeightForText = avatarSize - healthBarHeight - verticalSpacing
        val nameY = avatarPadding + (availableHeightForText - nameHeight) / 2
        Fonts.fontSemibold35.drawString(name, nameX, nameY, Color.WHITE.rgb)

        val healthBarX = avatarSize + avatarPadding * 2
        val healthBarY = avatarPadding + avatarSize - healthBarHeight

        // 添加对 NaN 的检查和处理
        val safeAugustusEasingHealth = if (augustusEasingHealth.isNaN() || augustusEasingHealth.isInfinite()) 0f else augustusEasingHealth
        val safeMaxHealth = if (entity.maxHealth.isNaN() || entity.maxHealth.isInfinite()) 1f else entity.maxHealth
        
        val healthPercent = (safeAugustusEasingHealth / safeMaxHealth).coerceIn(0f, 1f)
        val healthBarFillWidth = healthBarWidth * healthPercent

        drawRoundedRect(healthBarX, healthBarY, healthBarX + healthBarWidth, healthBarY + healthBarHeight, Color(40, 40, 40, 200).rgb, 4f)

        val healthBarColor = ColorUtils.getHealthColor(safeAugustusEasingHealth, safeMaxHealth)
        drawRoundedRect(healthBarX, healthBarY, healthBarX + healthBarFillWidth, healthBarY + healthBarHeight, healthBarColor.rgb, 4f)

        GlStateManager.popMatrix()
        return Border(x, y, x + totalWidth, y + totalHeight)
    }

    private fun renderExhibitionHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)

        val width = 120F
        val height = 45F
        val modelSize = 40F
        val borderRadius = 5f

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)

        drawRoundedRect(0F, 0F, width, height, Color(10, 10, 10, 255).rgb, borderRadius)

        RenderUtils.drawRoundedBorderRect(0F, 0F, width, height, 1F, Color(40, 40, 40, 255).rgb, Color(0, 0, 0, 0).rgb, borderRadius)

        val modelX = 2.5F
        val modelY = (height - modelSize) / 2

        renderPlayerModel(entity, modelX, modelY, modelSize)

        val infoX = modelX + modelSize + 2F
        val infoWidth = width - infoX - 5F

        val name = entity.name
        val nameY = 5F

        mc.fontRendererObj.drawStringWithShadow(
            name,
            infoX,
            nameY,
            Color.WHITE.rgb
        )

        val healthText = "HP: ${decimalFormat.format(entity.health)}"
        val distance = mc.thePlayer.getDistanceToEntity(entity)
        val distanceText = "Dist: ${decimalFormat.format(distance)}"
        val infoText = "$healthText | $distanceText"

        val infoY = nameY + mc.fontRendererObj.FONT_HEIGHT + 2F

        GlStateManager.pushMatrix()
        GlStateManager.translate(infoX, infoY, 0F)
        GlStateManager.scale(0.8F, 0.8F, 0.8F)
        mc.fontRendererObj.drawStringWithShadow(
            infoText,
            0F,
            0F,
            Color.WHITE.rgb
        )
        GlStateManager.popMatrix()

        val barY = infoY + 8F
        val barHeight = 5F
        val barWidth = infoWidth
        val barBorderRadius = 2f

        val healthPercent = entity.health / entity.maxHealth

        val barColor = when {
            healthPercent > 2.0/3.0 -> Color(0, 180, 0)
            healthPercent > 1.0/3.0 -> Color(255, 255, 0)
            else -> Color(255, 0, 0)
        }

        drawRoundedRect(infoX, barY, infoX + barWidth, barY + barHeight, Color(40, 40, 40).rgb, barBorderRadius)

        val barFillWidth = barWidth * healthPercent
        drawRoundedRect(infoX, barY, infoX + barFillWidth, barY + barHeight, barColor.rgb, barBorderRadius)

        val segmentWidth = 4F
        val dividerWidth = 1F
        var currentX = infoX + segmentWidth

        while (currentX < infoX + barWidth) {
            RenderUtils.drawRect(
                currentX, barY,
                currentX + dividerWidth, barY + barHeight,
                Color(10, 10, 10).rgb
            )
            currentX += segmentWidth + dividerWidth
        }

        GlStateManager.popMatrix()
        return Border(x, y, x + width, y + height)
    }

    private fun renderPlayerModel(entity: EntityLivingBase, x: Float, y: Float, size: Float) {
        GlStateManager.pushMatrix()

        GlStateManager.translate(x + size / 2, y + size, 0F)
        GlStateManager.scale(size / 2, size / 2, size / 2)

        GlStateManager.rotate(180F, 0F, 0F, 1F)
        GlStateManager.rotate(30F, 0F, 1F, 0F)

        RenderHelper.enableStandardItemLighting()
        GlStateManager.enableDepth()
        GlStateManager.disableCull()

        val renderManager = mc.renderManager
        renderManager.isRenderShadow = false

        try {
            renderManager.renderEntityWithPosYaw(
                entity,
                0.0, 0.0, 0.0,
                0F,
                1.0F
            )
        } catch (_: Exception) {
            RenderUtils.drawRect(x, y, x + size, y + size, Color.RED.rgb)
        }

        renderManager.isRenderShadow = true
        GlStateManager.enableCull()
        GlStateManager.disableDepth()
        disableStandardItemLighting()

        GlStateManager.popMatrix()
    }

    private fun renderNovolineHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        val width = 120F
        val height = 46F

        easingHealth = lerp(easingHealth, entity.health, animSpeed)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)
        GlStateManager.translate(-x, -y, 0F)

        RenderUtils.drawRoundedBorderRect(x+1, y+1, x + width - 1, y + height - 1, 1f, Color(40, 40, 40,200).rgb, Color.BLACK.rgb, 0F)

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            drawHead(it.locationSkin, (x + 6).toInt(), (y + 6).toInt(), 34, 34, Color.WHITE)
        }

        Fonts.minecraftFont.drawString(entity.name, (x + 46).toInt(), (y + 8).toInt(), Color.WHITE.rgb)

        val healthPercent = (easingHealth / entity.maxHealth).coerceIn(0F, 1F)
        val healthBarWidth = (width - 52) * healthPercent
        val barColor = when(novolineColorMode) {
            "Custom" -> Color(novolineColorRed, novolineColorGreen, novolineColorBlue)
            "Rainbow" -> Color.getHSBColor(hue, 0.7f, 0.9f)
            else -> ColorUtils.getHealthColor(easingHealth, entity.maxHealth)
        }

        RenderUtils.drawRect(x + 46, y + 22, x + width - 6, y + 32, Color(45, 45, 45).rgb)
        if (!novolineColorSpec){
            RenderUtils.drawRect(x + 46, y + 22, x + 46 + healthBarWidth, y + 32, barColor.rgb)
        }else{
            RenderUtils.drawGradientRect(x+46,y+22, x+46+healthBarWidth,y+32, novolineLeftColor.rgb, novolineRightColor.rgb,0f)
        }

        val healthPercentage = (easingHealth / entity.maxHealth * 100).coerceIn(0F, 100F)
        val healthText = "${decimalFormat.format(healthPercentage)}%"
        Fonts.minecraftFont.drawString(healthText, (x + 66).toInt(), (y + 24).toInt(), Color.WHITE.rgb)

        GlStateManager.popMatrix()
        return Border(x, y, x + width, y + height)
    }

    private fun renderStylesHUD(x: Float, y: Float): Border {
        val entity = target ?: return Border(0f, 0f, 0f, 0f)
        val width = (38 + Fonts.fontRegular35.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()
        val height = 44f

        stylesEasingHealth = lerp(stylesEasingHealth, entity.health, animSpeed)

        stylesEasingHealth = lerp(stylesEasingHealth, entity.health, animSpeed * 1.4F)

        if (stylesShadow) {
            ShowShadow(x, y, width + 14f, height, 0.3F)
        }

        RenderUtils.drawRect(x, y, x + width + 14f, y + height, Color(0, 0, 0, 120).rgb)

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            drawHead(it.locationSkin, x.toInt() + 3, y.toInt() + 3, 30, 30, Color.WHITE)
        }

        Fonts.fontRegular35.drawString(entity.name, x + 34.5f, y + 4f, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString("Health: ${"%.1f".format(stylesEasingHealth)}", x + 34.5f, y + 14f, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString("Distance: ${"%.1f".format(mc.thePlayer.getDistanceToEntity(entity))}m", x + 34.5f, y + 24f, Color.WHITE.rgb)

        RenderUtils.drawRect(x + 2.5f, y + 35.5f, x + width + 11.5f, y + 37.5f, Color(0, 0, 0, 200).rgb)
        RenderUtils.drawRect(x + 3f, y + 36f, x + 3f + (stylesEasingHealth / entity.maxHealth) * (width + 8.5f), y + 37f, Color(0,255,150))

        RenderUtils.drawRect(x + 2.5f, y + 39.5f, x + width + 11.5f, y + 41.5f, Color(0, 0, 0, 200).rgb)
        RenderUtils.drawRect(x + 3f, y + 40f, x + 3f + (entity.totalArmorValue / 20f) * (width + 8.5f), y + 41f, Color(77, 128, 255).rgb)

        return Border(x, y, x + width + 14f, y + height)
    }

    private fun renderOpaiHUD(x: Float, y: Float): Border {
        val killAuraTarget = KillAura.target.takeIf { it is EntityPlayer }
        val shouldRender = KillAura.handleEvents() && killAuraTarget != null || mc.currentScreen is GuiChat
        val target = killAuraTarget ?: if (opaiDelayCounter >= opaiVanishDelay) {
            mc.thePlayer
        } else {
            lastTarget ?: mc.thePlayer
        }

        if (shouldRender) {
            opaiDelayCounter = 0
        } else {
            opaiDelayCounter++
        }

        if (!shouldRender && opaiDelayCounter >= opaiVanishDelay) return Border(0f, 0f, 0f, 0f)

        val targetName = target?.name + "  "
        val targetNameWidth = Fonts.fontSemibold35.getStringWidth(targetName)
        val targetHealth = target?.health?.toInt() ?: 0
        val targetHealthWidth = Fonts.fontSemibold35.getStringWidth(targetHealth.toString())
        val textsDrawBegin = 3.5f + 30f + 3.5f
        val allTextLen = targetNameWidth + targetHealthWidth
        val resultProgressWidth = max(135f, textsDrawBegin + allTextLen + 8f)
        val publicXY: Pair<Float, Float> = Pair(3.5f * 2 + resultProgressWidth, 3.5f + 30f + 3.5f + 5f + 3.5f)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)

        if (opaiShadowCheck) {
            ShowShadow(0f, 0f, publicXY.first, publicXY.second, opaiShadowStrengh)
        }

        RenderUtils.drawRoundedBorderRect(0f, 0f, publicXY.first - 3.5f, publicXY.second, 0.2f,
            Color(0, 0, 0, opaiBackGroundAlpha).rgb, Color(0, 0, 0, opaiBackGroundAlpha).rgb, 5f)

        if (target != null && target is EntityLivingBase) {
            drawOpaiHead(target, 3.5f, 3.5f)
        }

        val progressBarLength = if (target != null) resultProgressWidth / target.maxHealth * target.health else 0f
        opaiAnimX = AnimationUtil.base(opaiAnimX.toDouble(), progressBarLength.toDouble(), 0.2).toFloat()

        RenderUtils.drawRoundedBorderRect(3.5f, 3.5f + 30f + 3.5f, resultProgressWidth, 3.5f + 30f + 3.5f + 5f, 0.3f,
            Color(0, 0, 0, 200).rgb, Color(0, 0, 0, 200).rgb, 5f)

        RenderUtils.drawRoundedBorderRect(3.5f, 3.5f + 30f + 3.5f, opaiAnimX, 3.5f + 30f + 3.5f + 5f, 0.3f,
            Color(opaiThemeColor.red, opaiThemeColor.green, opaiThemeColor.blue, 150).rgb,
            Color(opaiThemeColor.red, opaiThemeColor.green, opaiThemeColor.blue, 150).rgb, 4F)

        RenderUtils.drawRoundedBorderRect(3.5f, 3.5f + 30f + 3.5f, progressBarLength, 3.5f + 30f + 3.5f + 5f, 0.3f,
            opaiThemeColor.rgb, opaiThemeColor.rgb, 4F)

        Fonts.fontSemibold35.drawString(targetName, textsDrawBegin + 3.5f, 3.5f * 2, Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString(targetHealth.toString(), textsDrawBegin + targetNameWidth + 3.5f, 3.5f * 2 - 1F, opaiThemeColor.rgb)

        if (target != null) {
            val armorX = textsDrawBegin
            val armorY = 3.5f + 30f - 18
            drawOpaiArmor(armorX, armorY, target)
        }

        GlStateManager.popMatrix()
        return Border(x, y, x + publicXY.first, y + publicXY.second)
    }

    private fun drawOpaiHead(target: EntityLivingBase, x: Float, y: Float) {
        val texture = mc.renderManager.getEntityRenderObject<Entity>(target)
            ?.getEntityTexture(target) ?: return

        withClipping(main = {
            drawRoundedRect(x, y, x + 30f, y + 30f, 0, 5f)
        }, toClip = {
            drawHead(
                texture, x.toInt(), y.toInt(),
                30, 30,
                Color.WHITE
            )
        })
    }

    private fun drawOpaiArmor(x: Float, y: Float, target: EntityLivingBase) {
        if (target !is EntityPlayer) return
        GlStateManager.pushMatrix()
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        enableGUIStandardItemLighting()
        var offsetX = x
        val renderItem = mc.renderItem
        for (index in 3 downTo 0) {
            val stack = target.inventory.armorInventory[index] ?: continue
            renderItem.renderItemIntoGUI(stack, offsetX.toInt(), y.toInt())
            renderItem.renderItemOverlays(mc.fontRendererObj, stack, offsetX.toInt(), y.toInt())
            offsetX += 18f
        }
        disableStandardItemLighting()
        glDisable(GL_BLEND)
        GlStateManager.popMatrix()
    }

    private fun drawCircleArc(x: Float, y: Float, radius: Float, lineWidth: Float, startAngle: Float, endAngle: Float, color: Color) {
        glPushMatrix()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(lineWidth)

        glColor4f(color.red / 255F, color.green / 255F, color.blue / 255F, color.alpha / 255F)

        glBegin(GL_LINE_STRIP)
        for (i in (startAngle / 360 * 100).toInt()..(endAngle / 360 * 100).toInt()) {
            val angle = (i / 100.0 * 360.0 * (PI / 180)).toFloat()
            glVertex2f(x + sin(angle) * radius, y + cos(angle) * radius)
        }
        glEnd()

        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glPopMatrix()
        glColor4f(1f, 1f, 1f, 1f)
    }

    private fun drawCircle(x: Float, y: Float, radius: Float, color: Int) {
        val side = (radius * 2).toInt()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_POLYGON_SMOOTH)
        glBegin(GL_TRIANGLE_FAN)
        RenderUtils.glColor(Color(color))
        for (i in 0..side) {
            val angle = i * (Math.PI * 2) / side
            glVertex2d(x + sin(angle) * radius, y + cos(angle) * radius)
        }
        glEnd()
        glDisable(GL_POLYGON_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
    }

    private fun lerp(start: Float, end: Float, speed: Float): Float = start + (end - start) * speed * (deltaTime / (1000F / 60F))

    private fun getRainbowColor(): Color = Color.getHSBColor(hue, 1f, 1f)

    private fun renderNavenHUD(x: Float, y: Float): Border {
        val entity = target ?: return Border(0f, 0f, 0f, 0f)
        val width = 130f
        val height = 50f

        NavenEasingHealth = lerp(NavenEasingHealth, entity.health, animSpeed)

        NavenEasingHealth = lerp(NavenEasingHealth, entity.health, animSpeed * 1.1F)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)

        ShowShadow(0F, 0F, width, height, 0.3F)

        drawRoundedRect(0F, 0F, width, height, Color(30, 30, 30, 180).rgb, 5f)

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin?.let {
            drawHead(it, 7, 7, 30, 30, Color.WHITE)
        }

        val barX1 = 5f
        val barY1 = height - 10f
        val barX2 = width - 5f
        val barY2 = barY1 + 3f
        drawRoundedRect(barX1, barY1, barX2, barY2, Color(0, 0, 0, 200).rgb, 2f)

        val healthPercent = NavenEasingHealth / entity.maxHealth
        val fillX2 = barX1 + (barX2 - barX1) * healthPercent
        drawRoundedRect(barX1, barY1, fillX2, barY2, Color(160, 42, 42).rgb, 2f)

        Fonts.fontRegular35.drawString(entity.name, 40f, 10f, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString("Health: ${"%.2f".format(NavenEasingHealth)}", 40f, 22f, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString("Distance: ${"%.2f".format(entity.getDistanceToEntity(mc.thePlayer))}", 40f, 30f, Color.WHITE.rgb)

        GlStateManager.popMatrix()
        return Border(x, y, x + width, y + height)
    }

    private fun drawRoundedHead(skinLocation: ResourceLocation, x: Int, y: Int, width: Int, height: Int, color: Color, radius: Float = 6f) {
        Stencil.write()
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        drawRoundedRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), color.rgb, radius)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        Stencil.erase(true)
        drawHead(skinLocation, x, y, width, height, color)
        Stencil.dispose()
    }

    private fun drawHead(skinLocation: ResourceLocation, x: Int, y: Int, width: Int, height: Int, color: Color) {
        RenderUtils.drawHead(skinLocation, x, y, 8f, 8f, 8, 8, width, height, 64f, 64f, color)
    }

    private fun ShowShadow(startX: Float, startY: Float, width: Float, height: Float, shadowStrengh: Float) {
        GlowUtils.drawGlow(
            startX, startY,
            width, height,
            (shadowStrengh * 13F).toInt(),
            Color(0, 0, 0, 100)
        )
    }

    object Stencil {
        fun write() {
            StencilUtil.initStencilToWrite()
        }
        fun erase(invert: Boolean) {
            StencilUtil.readStencilBuffer(1)
        }
        fun dispose() {
            StencilUtil.uninitStencilBuffer()
        }
    }

    private fun projectEntity(entity: Entity): FloatArray? {
        try {
            val renderX = mc.renderManager.renderPosX
            val renderY = mc.renderManager.renderPosY
            val renderZ = mc.renderManager.renderPosZ

            val x = entity.posX - renderX
            val y = entity.posY + entity.height / 2f - renderY
            val z = entity.posZ - renderZ

            val viewport = org.lwjgl.BufferUtils.createIntBuffer(16)
            val modelView = org.lwjgl.BufferUtils.createFloatBuffer(16)
            val projection = org.lwjgl.BufferUtils.createFloatBuffer(16)
            val screenCoords = org.lwjgl.BufferUtils.createFloatBuffer(3)

            glGetFloat(GL_MODELVIEW_MATRIX, modelView)
            glGetFloat(GL_PROJECTION_MATRIX, projection)
            glGetInteger(GL_VIEWPORT, viewport)

            val result = org.lwjgl.util.glu.GLU.gluProject(x.toFloat(), y.toFloat(), z.toFloat(), modelView, projection, viewport, screenCoords)

            if (result) {
                val screenX = screenCoords.get(0)
                val screenY = org.lwjgl.opengl.Display.getHeight() - screenCoords.get(1)
                val screenZ = screenCoords.get(2)

                if (screenZ >= 0.0 && screenZ < 1.0) {
                    return floatArrayOf(screenX, screenY, screenZ)
                }
            }
            return null
        } catch (_: Exception) {
            return null
        }
    }
}