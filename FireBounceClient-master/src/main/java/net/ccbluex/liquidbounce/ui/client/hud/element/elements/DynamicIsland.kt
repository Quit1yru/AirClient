/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
@file:Suppress("unused", "KotlinConstantConditions", "KotlinConstantConditions", "SameParameterValue")

package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.OldGrimGapple
import net.ccbluex.liquidbounce.features.module.modules.world.ChestStealer
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold2
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extensions.getPing
import net.ccbluex.liquidbounce.utils.render.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.EntityLivingBase
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

@Suppress("KotlinConstantConditions")
@ElementInfo(name = "DynamicIsland")
class DynamicIsland : Element("DynamicIsland") {

    private val width by float("Width", 160f, 100f..250f)
    private val height by float("Height", 26f, 18f..60f)
    private val cornerRadius by float("CornerRadius", 14f, 8f..20f)
    private val animationSpeed by float("AnimationSpeed", 0.15f, 0.05f..0.3f)

    private val backgroundColor by color("BackgroundColor", Color(0, 0, 0, 200))
    private val textColor by color("TextColor", Color.WHITE)
    private val accentColor by color("AccentColor", Color(41, 75, 255))
    private val successColor by color("SuccessColor", Color(76, 175, 80))
    private val errorColor by color("ErrorColor", Color(244, 67, 54))
    private val warningColor by color("WarningColor", Color(255, 152, 0))
    private val glowBaseColor by color("GlowBaseColor", Color(41, 75, 255))
    private val constantGlow by boolean("ConstantGlow", true)
    private val extensionBackgroundColor by color("ExtensionBackgroundColor", Color(30, 30, 30, 180))

    private val progressBarHeight by float("ProgressBarHeight", 4f, 2f..8f)
    private val progressBarEnabledColor by color("ProgressBarEnabledColor", Color(76, 175, 80))
    private val progressBarDisabledColor by color("ProgressBarDisabledColor", Color(244, 67, 54))
    private val progressBarInfoColor by color("ProgressBarInfoColor", Color(41, 75, 255))
    private val progressBarWarningColor by color("ProgressBarWarningColor", Color(255, 152, 0))
    private val progressBarErrorColor by color("ProgressBarErrorColor", Color(244, 67, 54))
    private val progressBarCustomColor by color("ProgressBarCustomColor", Color(41, 75, 255))

    private val showScaffoldProgressBar by boolean("ShowScaffoldProgressBar", true)
    private val scaffoldProgressBarColor by color("ScaffoldProgressBarColor", Color(41, 75, 255))
    private val scaffoldProgressBarBackgroundColor by color("ScaffoldProgressBarBackgroundColor", Color(255, 255, 255, 30))
    private val scaffoldProgressBarHeight by float("ScaffoldProgressBarHeight", 3f, 1f..6f)

    private val shakeAnimation by boolean("ShakeAnimation", true)
    private val shakeIntensity by float("ShakeIntensity", 4f, 1f..10f)
    private val shakeDuration by int("ShakeDuration", 300, 100..1000)

    private val glowEffect by boolean("GlowEffect", true)
    private val glowIntensity by float("GlowIntensity", 0.8f, 0.1f..2f)
    private val glowBlurRadius by int("GlowBlurRadius", 10, 5..20)

    private val showFPS by boolean("ShowFPS", true)
    private val showClientName by boolean("ShowClientName", true)
    private val showPlayerName by boolean("ShowPlayerName", true)
    private val showTime by boolean("ShowTime", false)
    private val showModuleStatus by boolean("ShowModuleStatus", true)
    private val showScaffoldBlocks by boolean("ShowScaffoldBlocks", true)
    private val showKillAuraTarget by boolean("ShowKillAuraTarget", true)
    private val showChestItems by boolean("ShowChestItems", true)
    private val breathingEffect by boolean("BreathingEffect", true)
    private val pulseOnNotification by boolean("PulseOnNotification", true)
    private val autoWidth by boolean("AutoWidth", true)
    private val autoHeight by boolean("AutoHeight", true)

    private val multipleModuleNotifications by boolean("MultipleModuleNotifications", true)
    private val stateTransitionEffect by boolean("StateTransitionEffect", true)
    private val maxNotifications by int("MaxNotifications", 5, 1..10)

    private val notificationDuration by int("NotificationDuration", 80, 0..200)
    private val scaffoldUpdateInterval by int("ScaffoldUpdateInterval", 10, 5..20)

    private val mainFont by font("MainFont", Fonts.fontRegular30)

    private val acrylicEffect by boolean("AcrylicEffect", true)
    private val backgroundColorAlpha by int("BackgroundAlpha", 180, 0..255)
    private val backgroundBlur by boolean("BackgroundBlur", true)
    private val blurStrength by float("BlurStrength", 10f, 1f..20f)

    private var currentMainWidth = 0f
    private var currentHeight = 0f
    private var currentNotifications = mutableListOf<Notification>()
    private var pulseAnimation = 0f
    private var breathAnimation = 0f
    private var glowAnimation = 0f
    private var lastModuleStates = mutableMapOf<String, Boolean>()
    private var stateTransitionAnimation = 0f
    private var isInTransition = false
    private var transitionStartTime = 0L

    private var lastScaffoldBlockCount = -1
    private var scaffoldUpdateTimer = 0
    private var scaffoldWasEnabled = false
    private var shouldShowScaffoldExtension = false

    private var scaffoldMaxBlocks = 100
    private var scaffoldProgressMaxInitialized = false
    private var lastHandleEventTime = 0L

    private var shouldShowKillAuraTarget = false
    private var lastTarget: EntityLivingBase? = null

    private var currentTotalHeight = 0f
    private var targetTotalHeight = 0f
    private var notificationsVerticalOffset = 0f
    private var scaffoldVerticalOffset = 0f
    private var killauraVerticalOffset = 0f

    private var shakeAnimationProgress = 0f
    private var isShaking = false
    private var shakeStartTime = 0L
    private var shakeOffsetX = 0f
    private var shakeOffsetY = 0f

    private var lastTargetMainWidth = 0f
    private var widthAnimationStartTime = 0L
    private var widthAnimationDuration = 200L
    private var isWidthAnimating = false

    private val showGappleIndicator by boolean("ShowGappleIndicator", true)
    private val gappleIndicatorSize by float("GappleIndicatorSize", 24f, 18f..35f)
    private val gappleProgressColor by color("GappleProgressColor", Color(255, 215, 0, 255))
    private val gappleProgressBackgroundColor by color("GappleProgressBackgroundColor", Color(255, 255, 255, 50))
    private val gappleProgressThickness by float("GappleProgressThickness", 3.5f, 2f..6f)
    private val gappleSeparationDistance by float("GappleSeparationDistance", 10f, 4f..20f)
    private val gappleAnimationSpeed by float("GappleAnimationSpeed", 0.15f, 0.05f..0.3f)

    private var gappleIndicatorX = 0f
    private var gappleIndicatorY = 0f
    private var gappleSeparationProgress = 0f
    private var gappleScaleAnimation = 1f

    private var lastFrameTime = 0L
    private var deltaTime = 0f
    private var smoothMainWidth = 0f
    private var smoothTotalHeight = 0f

    enum class NotificationType {
        MODULE_ENABLED,
        MODULE_DISABLED,
        CUSTOM,
        INFO,
        WARNING,
        ERROR
    }

    data class Notification(
        val type: NotificationType,
        val message: String,
        val duration: Int = 80,
        val timestamp: Long = System.currentTimeMillis(),
        val moduleName: String = "",
        var switchAnimation: Float = 0f,
        var currentWidth: Float = 0f
    )

    private fun lerp(start: Float, end: Float, speed: Float): Float {
        if (abs(end - start) < 0.01f) return end
        val t = (1.0 - (1.0 - speed).pow(deltaTime * 60.0)).toFloat()
        return start + (end - start) * t.coerceIn(0f, 1f)
    }

    private fun smoothStep(t: Float): Float {
        return t * t * (3f - 2f * t)
    }

    private fun easeInOutCubic(x: Float): Float {
        return if (x < 0.5f) 4 * x * x * x else 1 - (-2 * x + 2).pow(3) / 2
    }

    private fun applyDynamicAlpha(baseColor: Color, alphaMultiplier: Float = 1f): Color {
        val userAlpha = baseColor.alpha / 255f
        val finalAlpha = userAlpha * alphaMultiplier.coerceIn(0f, 1f)
        return Color(
            baseColor.red,
            baseColor.green,
            baseColor.blue,
            (finalAlpha * 255).toInt()
        )
    }

    override fun drawElement(): Border {
        val currentTime = System.currentTimeMillis()
        if (lastFrameTime == 0L) lastFrameTime = currentTime
        deltaTime = (currentTime - lastFrameTime) / 1000f
        deltaTime = deltaTime.coerceIn(0.001f, 0.1f)
        lastFrameTime = currentTime

        val isChestStealerSilentGui = ChestStealer.handleEvents() && ChestStealer.silentGUI
        val isChestOpen = mc.currentScreen is GuiChest

        if (isChestStealerSilentGui && isChestOpen && showChestItems) {
            updateChestSlots()
            return drawChestItemsView()
        }

        val targetMainWidth = if (autoWidth) calculateMainContentWidth() else width

        if (abs(targetMainWidth - lastTargetMainWidth) > 0.1f) {
            isWidthAnimating = true
            widthAnimationStartTime = currentTime
            lastTargetMainWidth = targetMainWidth
        }

        updateAnimations()

        if (isWidthAnimating) {
            val elapsed = currentTime - widthAnimationStartTime
            if (elapsed < widthAnimationDuration) {
                val progress = elapsed.toFloat() / widthAnimationDuration
                val easedProgress = smoothStep(progress)
                smoothMainWidth = lerp(smoothMainWidth, targetMainWidth, easedProgress * 0.5f)
            } else {
                isWidthAnimating = false
                smoothMainWidth = targetMainWidth
            }
        } else {
            smoothMainWidth = lerp(smoothMainWidth, targetMainWidth, animationSpeed)
        }

        currentMainWidth = smoothMainWidth

        val basePanelHeight = 26f
        val notificationsHeight = if (multipleModuleNotifications && currentNotifications.size > 1) {
            (currentNotifications.size - 1) * 26f
        } else {
            0f
        }
        val scaffoldExtensionHeight = if (shouldShowScaffoldExtension) 32f else 0f
        val killAuraTargetHeight = if (shouldShowKillAuraTarget) 32f else 0f

        targetTotalHeight = basePanelHeight + notificationsHeight + scaffoldExtensionHeight + killAuraTargetHeight
        smoothTotalHeight = lerp(smoothTotalHeight, targetTotalHeight, animationSpeed)
        currentTotalHeight = smoothTotalHeight

        val targetNotificationsOffset = if (notificationsHeight > 0) 0f else -notificationsHeight
        val targetScaffoldOffset = if (shouldShowScaffoldExtension)
            (if (notificationsHeight > 0) 0f else -32f)
        else -32f
        val targetKillauraOffset = if (shouldShowKillAuraTarget)
            (if (shouldShowScaffoldExtension) 0f else -32f)
        else -32f

        notificationsVerticalOffset = lerp(notificationsVerticalOffset, targetNotificationsOffset, animationSpeed)
        scaffoldVerticalOffset = lerp(scaffoldVerticalOffset, targetScaffoldOffset, animationSpeed)
        killauraVerticalOffset = lerp(killauraVerticalOffset, targetKillauraOffset, animationSpeed)

        val shakeOffset = if (isShaking) {
            calculateShakeOffset()
        } else {
            Pair(0f, 0f)
        }

        val mainPanelTop = -basePanelHeight / 2 + shakeOffset.second
        val mainPanelBottom = mainPanelTop + basePanelHeight

        val notificationsTop = mainPanelBottom
        val notificationsBottom = notificationsTop + notificationsHeight

        val scaffoldTop = notificationsBottom
        val scaffoldBottom = scaffoldTop + scaffoldExtensionHeight

        val killAuraTop = scaffoldBottom
        val killAuraBottom = killAuraTop + killAuraTargetHeight

        val totalTop = mainPanelTop
        val totalBottom = killAuraBottom

        val breathOffset = if (breathingEffect) (sin(breathAnimation) * 1.5f) else 0f
        val adjustedMainTop = mainPanelTop + breathOffset
        val adjustedMainBottom = mainPanelBottom + breathOffset

        updateScaffoldInfo()
        updateKillAuraTargetInfo()

        val overallWidth = calculateOverallWidth()

        if (stateTransitionEffect && isInTransition) {
            drawStateTransitionEffect(totalTop, totalBottom, overallWidth)
        }

        if (glowEffect && overallWidth > 0f && currentTotalHeight > 0f) {
            drawGlowEffect(totalTop, totalBottom, overallWidth, currentTotalHeight)
        }

        GL11.glPushMatrix()
        GL11.glTranslatef(shakeOffset.first, 0f, 0f)

        drawDynamicBackground(
            totalTop, totalBottom, overallWidth,
            basePanelHeight, notificationsHeight, scaffoldExtensionHeight, killAuraTargetHeight,
            adjustedMainTop, adjustedMainBottom
        )

        if (pulseAnimation > 0 && currentMainWidth > 0f) {
            GlowUtils.drawGlow(
                -currentMainWidth / 2 - 6f, adjustedMainTop - 6f,
                currentMainWidth + 12f, basePanelHeight + 12f,
                (glowBlurRadius * pulseAnimation).toInt(),
                applyDynamicAlpha(accentColor, pulseAnimation)
            )
        }

        if (multipleModuleNotifications && currentNotifications.size > 1) {
            drawMultipleNotificationsContent(notificationsTop, notificationsBottom, overallWidth)
        }

        if (shouldShowScaffoldExtension) {
            drawScaffoldExtension(scaffoldTop, scaffoldBottom, overallWidth)
        }

        if (shouldShowKillAuraTarget) {
            drawKillAuraTargetExtension(killAuraTop, killAuraBottom, overallWidth)
        }

        if (showGappleIndicator) {
            drawGappleIndicator()
        }

        if (currentNotifications.isNotEmpty()) {
            if (multipleModuleNotifications && currentNotifications.size > 1) {
                val mainNotification = currentNotifications.first()
                drawSingleNotificationContent(adjustedMainTop, adjustedMainBottom, mainNotification, overallWidth)
            } else {
                val notification = currentNotifications.first()
                drawSingleNotificationContent(adjustedMainTop, adjustedMainBottom, notification, overallWidth)
            }
        } else {
            drawNormalContent(adjustedMainTop, adjustedMainBottom, currentMainWidth)
        }

        GL11.glPopMatrix()

        updateNotificationSystem()
        checkForNotifications()

        return Border(
            -overallWidth / 2,
            totalTop,
            overallWidth / 2,
            totalBottom
        )
    }

    private fun calculateMainContentWidth(): Float {
        var totalWidth = 0f

        if (currentNotifications.isEmpty()) {
            val parts = mutableListOf<String>()

            if (showClientName) parts.add("FireBounce")
            if (showFPS) parts.add("${Minecraft.getDebugFPS()}fps")
            if (showPlayerName && mc.thePlayer != null) parts.add(mc.thePlayer.name)
            if (showTime) parts.add(getCurrentTime())

            val ping = getPing()
            parts.add("${ping}ms")

            if (parts.isNotEmpty()) {
                val content = parts.joinToString(" · ")
                totalWidth = mainFont.getStringWidth(content) + 30f
            }
        } else {
            if (multipleModuleNotifications && currentNotifications.size > 1) {
                totalWidth = calculateMultipleNotificationsWidth()
            } else {
                val notification = currentNotifications.firstOrNull()
                if (notification != null) {
                    val textWidth = mainFont.getStringWidth(notification.message)
                    totalWidth = 40f + textWidth + 16f
                    totalWidth = max(totalWidth, 140f)
                }
            }
        }

        return if (totalWidth > 0) max(totalWidth, width) else width
    }

    private fun calculateOverallWidth(): Float {
        var maxWidth = currentMainWidth

        if (multipleModuleNotifications && currentNotifications.size > 1) {
            for (notification in currentNotifications.drop(1)) {
                maxWidth = max(maxWidth, calculateNotificationWidth(notification))
            }
        }

        if (shouldShowScaffoldExtension) {
            maxWidth = max(maxWidth, calculateScaffoldExtensionWidth())
        }

        if (shouldShowKillAuraTarget) {
            maxWidth = max(maxWidth, calculateKillAuraTargetWidth())
        }

        return maxWidth
    }

    private fun calculateScaffoldExtensionWidth(): Float {
        val blockCount = net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.blocksAmount()
        val bps = String.format("%.2f", net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed * 20.0)
        val message = when {
            blockCount <= 0 -> "No blocks! | Speed: ${bps}b/s"
            blockCount <= 8 -> "$blockCount blocks left | Speed: ${bps}b/s"
            else -> "$blockCount blocks left | Speed: ${bps}b/s"
        }

        val textWidth = mainFont.getStringWidth(message)
        return max(40f + textWidth + 20f, 160f)
    }

    private fun calculateKillAuraTargetWidth(): Float {
        if (KillAura.target == null) return 0f

        val target = KillAura.target
        val playerName = target?.displayName?.formattedText ?: return 0f

        val nameWidth = mainFont.getStringWidth(playerName)

        return max(40f + nameWidth + 40f, 180f)
    }

    private fun calculateShakeOffset(): Pair<Float, Float> {
        if (!isShaking) return Pair(0f, 0f)

        val elapsed = System.currentTimeMillis() - shakeStartTime
        if (elapsed >= shakeDuration) {
            isShaking = false
            shakeAnimationProgress = 0f
            return Pair(0f, 0f)
        }

        val progress = elapsed.toFloat() / shakeDuration
        shakeAnimationProgress = progress

        val intensity = shakeIntensity * (1 - progress)

        val time = elapsed.toFloat() / 50f
        val shakeX = (sin(time * 2.3f) * 0.7f + sin(time * 1.7f) * 0.3f) * intensity
        val shakeY = (cos(time * 1.5f) * 0.5f + sin(time * 2.1f) * 0.5f) * intensity * 0.5f

        return Pair(shakeX, shakeY)
    }

    private fun triggerShake() {
        if (!shakeAnimation) return

        isShaking = true
        shakeStartTime = System.currentTimeMillis()
        shakeAnimationProgress = 0f
    }

    private fun drawDynamicBackground(
        totalTop: Float, totalBottom: Float, overallWidth: Float,
        basePanelHeight: Float, notificationsHeight: Float, scaffoldExtensionHeight: Float, killAuraTargetHeight: Float,
        adjustedMainTop: Float, adjustedMainBottom: Float
    ) {
        if (overallWidth <= 0f) return

        val hasNotifications = notificationsHeight > 0f && multipleModuleNotifications && currentNotifications.size > 1
        val hasScaffold = scaffoldExtensionHeight > 0f
        val hasKillAuraTarget = killAuraTargetHeight > 0f

        if (currentMainWidth > 0f || hasNotifications || hasScaffold || hasKillAuraTarget) {
            val x1 = -overallWidth / 2
            val y1 = totalTop
            val x2 = overallWidth / 2
            val y2 = totalBottom

            if (backgroundBlur) {
                net.ccbluex.liquidbounce.utils.render.InternalBlurShader.blurArea(
                    x1, y1, x2 - x1, y2 - y1, blurStrength
                )
            }

            if (acrylicEffect) {
                val baseAlpha = backgroundColorAlpha
                val bgColor = Color(backgroundColor.red, backgroundColor.green, backgroundColor.blue, baseAlpha)

                drawRoundedRect(
                    x1, y1,
                    x2, y2,
                    bgColor.rgb,
                    cornerRadius,
                    RenderUtils.RoundedCorners.ALL
                )
            } else {
                drawRoundedRect(
                    x1, y1,
                    x2, y2,
                    backgroundColor.rgb,
                    cornerRadius,
                    RenderUtils.RoundedCorners.ALL
                )
            }
        }
    }

    private fun drawMultipleNotificationsContent(notificationsTop: Float, notificationsBottom: Float, overallWidth: Float) {
        val extraNotifications = currentNotifications.drop(1)
        if (extraNotifications.isEmpty()) return

        val itemHeight = 26f
        var currentTop = notificationsTop + notificationsVerticalOffset

        for ((index, notification) in extraNotifications.withIndex()) {
            val targetWidth = calculateNotificationWidth(notification)
            notification.currentWidth = lerp(notification.currentWidth, targetWidth, animationSpeed)

            val itemWidth = notification.currentWidth
            val itemTop = currentTop
            val itemBottom = itemTop + itemHeight

            drawNotificationItem(notification, itemTop, itemBottom, index + 1, itemWidth, overallWidth)

            currentTop = itemBottom
        }
    }

    private fun drawSingleNotificationContent(panelTop: Float, panelBottom: Float, notification: Notification, overallWidth: Float) {
        val targetWidth = calculateNotificationWidth(notification)
        drawNotificationItem(notification, panelTop, panelBottom, 0, targetWidth, overallWidth)
    }

    private fun drawNotificationItem(notification: Notification, top: Float, bottom: Float, index: Int, itemWidth: Float, overallWidth: Float) {
        val progress = 1f - ((System.currentTimeMillis() - notification.timestamp) / (notification.duration * 50f)).coerceIn(0f, 1f)
        val textAlpha = if (progress > 0.05f) 255 else (255 * progress * 20f).toInt()

        val iconSize = 16f
        val itemCenterY = (top + bottom) / 2
        val adjustedCenterY = itemCenterY - 2f

        val switchIconX = -overallWidth / 2 + 8f
        val switchIconY = adjustedCenterY - iconSize / 2

        drawNewSwitchIcon(notification.type, switchIconX, switchIconY, iconSize, textAlpha, notification.switchAnimation)

        val textX = -overallWidth / 2 + 30f
        val textY = adjustedCenterY - mainFont.FONT_HEIGHT / 2 + 1

        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        if (textAlpha > 100) {
            mainFont.drawString(
                notification.message, textX + 1, textY + 1,
                Color(0, 0, 0, (textAlpha * 0.3).toInt()).rgb, false
            )
        }

        mainFont.drawString(
            notification.message, textX, textY,
            applyDynamicAlpha(textColor, textAlpha / 255f).rgb, false
        )

        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()

        val progressBarMargin = 12f
        val progressBarStartX = -overallWidth / 2 + progressBarMargin
        val progressBarEndX = overallWidth / 2 - progressBarMargin
        val progressBarTotalWidth = progressBarEndX - progressBarStartX
        val progressBarY = bottom - 8f

        val progressBarColor = when (notification.type) {
            NotificationType.MODULE_ENABLED -> progressBarEnabledColor
            NotificationType.MODULE_DISABLED -> progressBarDisabledColor
            NotificationType.INFO -> progressBarInfoColor
            NotificationType.WARNING -> progressBarWarningColor
            NotificationType.ERROR -> progressBarErrorColor
            else -> progressBarCustomColor
        }

        drawRoundedRect(
            progressBarStartX, progressBarY,
            progressBarEndX, progressBarY + progressBarHeight,
            Color(255, 255, 255, 38).rgb,
            progressBarHeight / 2
        )

        val currentProgressWidth = progressBarTotalWidth * progress
        val progressBarForegroundEndX = progressBarStartX + currentProgressWidth

        drawRoundedRect(
            progressBarStartX, progressBarY,
            progressBarForegroundEndX, progressBarY + progressBarHeight,
            progressBarColor.rgb,
            progressBarHeight / 2
        )
    }

    private fun drawNewSwitchIcon(type: NotificationType, x: Float, y: Float, size: Float, alpha: Int, switchProgress: Float) {
        val switchWidth = size * 1.2f
        val switchHeight = size * 0.5f
        val circleSize = size * 0.4f

        val switchX = x
        val switchY = y + (size - switchHeight) / 2

        val backgroundColor = when (type) {
            NotificationType.MODULE_ENABLED -> applyDynamicAlpha(successColor, alpha / 255f * 0.3f)
            NotificationType.MODULE_DISABLED -> applyDynamicAlpha(errorColor, alpha / 255f * 0.3f)
            else -> applyDynamicAlpha(accentColor, alpha / 255f * 0.3f)
        }

        val circleColor = when (type) {
            NotificationType.MODULE_ENABLED -> applyDynamicAlpha(successColor, alpha / 255f)
            NotificationType.MODULE_DISABLED -> applyDynamicAlpha(errorColor, alpha / 255f)
            else -> applyDynamicAlpha(accentColor, alpha / 255f)
        }

        drawRoundedRect(
            switchX, switchY,
            switchX + switchWidth, switchY + switchHeight,
            backgroundColor.rgb,
            switchHeight / 2
        )

        val minCircleX = switchX + 2f
        val maxCircleX = switchX + switchWidth - circleSize - 2f
        val circleRange = maxCircleX - minCircleX

        val circleX = when (type) {
            NotificationType.MODULE_ENABLED -> minCircleX + circleRange * switchProgress
            NotificationType.MODULE_DISABLED -> maxCircleX - circleRange * switchProgress
            else -> switchX + (switchWidth - circleSize) * 0.5f
        }
        val circleY = switchY + (switchHeight - circleSize) / 2

        drawRoundedRect(
            circleX, circleY,
            circleX + circleSize, circleY + circleSize,
            circleColor.rgb,
            circleSize / 2
        )
    }

    private fun drawStateTransitionEffect(totalTop: Float, totalBottom: Float, width: Float) {
        val transitionProgress = stateTransitionAnimation
        if (transitionProgress <= 0f) return

        val transitionColor = applyDynamicAlpha(accentColor, transitionProgress * 0.3f)
        drawRoundedRect(
            -width / 2, totalTop,
            width / 2, totalBottom,
            transitionColor.rgb,
            cornerRadius
        )
    }

    private fun drawGlowEffect(totalTop: Float, totalBottom: Float, width: Float, height: Float) {
        val baseGlowAlpha = if (constantGlow) {
            (80 * glowIntensity).toInt().coerceAtMost(120)
        } else {
            (glowAnimation * 80 * glowIntensity).toInt()
        }

        if (baseGlowAlpha <= 0) return

        val glowColor = applyDynamicAlpha(glowBaseColor, baseGlowAlpha / 255f)

        if (width > 0f && height > 0f) {
            GlowUtils.drawGlow(
                -width / 2, totalTop,
                width, height,
                glowBlurRadius,
                glowColor
            )
        }
    }

    private fun drawScaffoldExtension(extensionTop: Float, extensionBottom: Float, overallWidth: Float) {
        val actualExtensionTop = extensionTop
        val actualExtensionBottom = extensionBottom

        if (actualExtensionTop >= actualExtensionBottom) return

        val textAlpha = 255

        val blockCount = net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.blocksAmount()
        val bps = String.format("%.2f", net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed * 20.0)

        val (iconColor, message) = when {
            blockCount <= 0 -> Pair(errorColor, "No blocks! | Speed: ${bps}b/s")
            blockCount <= 8 -> Pair(warningColor, "$blockCount blocks left | Speed: ${bps}b/s")
            else -> Pair(successColor, "$blockCount blocks left | Speed: ${bps}b/s")
        }

        val iconSize = 16f
        val extensionCenterY = (actualExtensionTop + actualExtensionBottom) / 2

        val iconX = -overallWidth / 2 + 12f
        val iconY = extensionCenterY - iconSize / 2 - 4f

        drawBlockIcon(iconX, iconY, iconSize, textAlpha, iconColor)

        val textX = iconX + iconSize + 8f
        val textY = extensionCenterY - mainFont.FONT_HEIGHT / 2 - 4f

        mainFont.drawString(
            message,
            textX,
            textY,
            applyDynamicAlpha(textColor, textAlpha / 255f).rgb,
            false
        )

        if (showScaffoldProgressBar && scaffoldMaxBlocks > 0 && scaffoldProgressMaxInitialized) {
            val progressBarMargin = 12f
            val progressBarStartX = -overallWidth / 2 + progressBarMargin
            val progressBarEndX = overallWidth / 2 - progressBarMargin
            val progressBarTotalWidth = progressBarEndX - progressBarStartX

            val progressBarY = textY + mainFont.FONT_HEIGHT + 4f

            val progress = if (scaffoldMaxBlocks > 0) {
                (blockCount.toFloat() / scaffoldMaxBlocks).coerceIn(0f, 1f)
            } else {
                0f
            }

            drawRoundedRect(
                progressBarStartX, progressBarY,
                progressBarEndX, progressBarY + scaffoldProgressBarHeight,
                scaffoldProgressBarBackgroundColor.rgb,
                scaffoldProgressBarHeight / 2
            )

            if (progress > 0) {
                val progressBarWidth = progressBarTotalWidth * progress
                val progressBarForegroundEndX = progressBarStartX + progressBarWidth

                val barColor = when {
                    progress > 0.6f -> successColor
                    progress > 0.3f -> warningColor
                    else -> errorColor
                }

                drawRoundedRect(
                    progressBarStartX, progressBarY,
                    progressBarForegroundEndX, progressBarY + scaffoldProgressBarHeight,
                    barColor.rgb,
                    scaffoldProgressBarHeight / 2
                )
            }
        }
    }

    private fun drawKillAuraTargetExtension(extensionTop: Float, extensionBottom: Float, overallWidth: Float) {
        val actualExtensionTop = extensionTop
        val actualExtensionBottom = extensionBottom

        if (actualExtensionTop >= actualExtensionBottom) return

        val target = KillAura.target ?: return

        val health = net.ccbluex.liquidbounce.utils.attack.EntityUtils.getHealth(target)
        val maxHealth = target.maxHealth
        val healthPercent = (health / maxHealth).coerceIn(0f, 1f)

        val entityName = getEntityDisplayName(target)

        val extensionCenterY = (actualExtensionTop + actualExtensionBottom) / 2
        val iconSize = 20f

        val iconX = -overallWidth / 2 + 12f
        val iconY = extensionCenterY - iconSize / 2

        drawPlayerHead(iconX, iconY, iconSize, 255)

        val textStartX = iconX + iconSize + 10f
        val nameY = extensionCenterY - mainFont.FONT_HEIGHT - 1
        val barHeight = 4f
        val barY = extensionCenterY + 1

        val barWidth = overallWidth / 2 - textStartX - 12f

        mainFont.drawString(
            entityName,
            textStartX,
            nameY,
            textColor.rgb,
            false
        )

        drawRoundedRect(
            textStartX, barY,
            textStartX + barWidth, barY + barHeight,
            Color(0, 0, 0, 100).rgb,
            barHeight / 2
        )

        val foregroundWidth = barWidth * healthPercent
        val barColor = when {
            healthPercent > 0.6f -> successColor
            healthPercent > 0.3f -> warningColor
            else -> errorColor
        }

        if (foregroundWidth > 0) {
            drawRoundedRect(
                textStartX, barY,
                textStartX + foregroundWidth, barY + barHeight,
                barColor.rgb,
                barHeight / 2
            )
        }
    }

    private fun calculateMultipleNotificationsWidth(): Float {
        var maxWidth = 140f
        for (notification in currentNotifications) {
            val textWidth = mainFont.getStringWidth(notification.message)
            val notificationWidth = 40f + textWidth + 16f
            maxWidth = max(maxWidth, notificationWidth)
        }
        return maxWidth
    }

    private fun calculateNotificationWidth(notification: Notification): Float {
        val textWidth = mainFont.getStringWidth(notification.message)
        return max(40f + textWidth + 16f, 140f)
    }

    private fun drawBlockIcon(x: Float, y: Float, size: Float, alpha: Int, color: Color) {
        val stack = mc.thePlayer?.inventory?.getCurrentItem() ?:
        mc.thePlayer?.inventory?.getStackInSlot(net.ccbluex.liquidbounce.utils.inventory.SilentHotbar.currentSlot)

        GL11.glPushMatrix()
        GL11.glTranslatef(x, y, 0f)
        GL11.glScalef(size / 16f, size / 16f, 1f)

        GlStateManager.enableRescaleNormal()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        RenderHelper.enableGUIStandardItemLighting()

        if (stack != null && stack.item is ItemBlock) {
            GL11.glColor4f(1f, 1f, 1f, alpha / 255f)
            mc.renderItem.renderItemAndEffectIntoGUI(stack, 0, 0)
        } else {
            val barrierStack = net.minecraft.item.ItemStack(net.minecraft.init.Blocks.barrier)
            mc.renderItem.renderItemAndEffectIntoGUI(barrierStack, 0, 0)
        }

        RenderHelper.disableStandardItemLighting()
        GlStateManager.disableRescaleNormal()
        GlStateManager.disableBlend()
        GL11.glColor4f(1f, 1f, 1f, 1f)
        GL11.glPopMatrix()
    }

    private fun getEntityDisplayName(entity: EntityLivingBase): String {
        return when {
            entity.hasCustomName() -> entity.customNameTag
            entity is net.minecraft.entity.player.EntityPlayer -> entity.name
            else -> {
                val className = entity.javaClass.simpleName
                when {
                    className.startsWith("Entity") -> className.substring(6)
                    else -> className
                }
            }
        }
    }

    private fun drawPlayerHead(x: Float, y: Float, size: Float, alpha: Int) {
        val target = KillAura.target ?: return

        try {
            GL11.glPushMatrix()
            GL11.glTranslatef(x, y, 0f)
            GL11.glScalef(size / 16f, size / 16f, 1f)

            GlStateManager.enableRescaleNormal()
            GlStateManager.enableBlend()
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
            RenderHelper.enableGUIStandardItemLighting()

            GL11.glColor4f(1f, 1f, 1f, alpha / 255f)

            val itemStack = when (target) {
                is net.minecraft.entity.player.EntityPlayer -> {
                    val playerHead = net.minecraft.item.ItemStack(net.minecraft.init.Items.skull, 1, 3)
                    val nbt = net.minecraft.nbt.NBTTagCompound()
                    val skullOwner = net.minecraft.nbt.NBTTagCompound()
                    skullOwner.setString("Id", target.uniqueID.toString())
                    nbt.setTag("SkullOwner", skullOwner)
                    playerHead.tagCompound = nbt
                    playerHead
                }
                is net.minecraft.entity.monster.EntityZombie -> net.minecraft.item.ItemStack(net.minecraft.init.Items.skull, 1, 2)
                is net.minecraft.entity.monster.EntitySkeleton -> {
                    val skeleton = net.minecraft.item.ItemStack(net.minecraft.init.Items.skull, 1, 0)
                    if (target.skeletonType == 1) {
                        net.minecraft.item.ItemStack(net.minecraft.init.Items.skull, 1, 1)
                    } else {
                        skeleton
                    }
                }
                is net.minecraft.entity.monster.EntityCreeper -> net.minecraft.item.ItemStack(net.minecraft.init.Items.skull, 1, 4)
                is net.minecraft.entity.monster.EntityEnderman -> net.minecraft.item.ItemStack(net.minecraft.init.Items.ender_eye)
                is net.minecraft.entity.monster.EntitySpider -> net.minecraft.item.ItemStack(net.minecraft.init.Items.spider_eye)
                is net.minecraft.entity.monster.EntityCaveSpider -> net.minecraft.item.ItemStack(net.minecraft.init.Items.spider_eye)
                is net.minecraft.entity.monster.EntityBlaze -> net.minecraft.item.ItemStack(net.minecraft.init.Items.blaze_rod)
                is net.minecraft.entity.monster.EntityGhast -> net.minecraft.item.ItemStack(net.minecraft.init.Items.ghast_tear)
                is net.minecraft.entity.monster.EntityMagmaCube -> net.minecraft.item.ItemStack(net.minecraft.init.Items.magma_cream)
                is net.minecraft.entity.monster.EntitySlime -> net.minecraft.item.ItemStack(net.minecraft.init.Items.slime_ball)
                is net.minecraft.entity.monster.EntityWitch -> net.minecraft.item.ItemStack(net.minecraft.init.Items.potionitem)
                is net.minecraft.entity.monster.EntityGuardian -> net.minecraft.item.ItemStack(net.minecraft.init.Items.prismarine_shard)
                is net.minecraft.entity.boss.EntityWither -> net.minecraft.item.ItemStack(net.minecraft.init.Items.skull, 1, 1)
                is net.minecraft.entity.monster.EntityIronGolem -> net.minecraft.item.ItemStack(net.minecraft.init.Items.iron_ingot)
                is net.minecraft.entity.monster.EntitySnowman -> net.minecraft.item.ItemStack(net.minecraft.init.Items.snowball)
                is net.minecraft.entity.passive.EntityAnimal -> {
                    when (target) {
                        is net.minecraft.entity.passive.EntityCow -> net.minecraft.item.ItemStack(net.minecraft.init.Items.beef)
                        is net.minecraft.entity.passive.EntityPig -> net.minecraft.item.ItemStack(net.minecraft.init.Items.porkchop)
                        is net.minecraft.entity.passive.EntityChicken -> net.minecraft.item.ItemStack(net.minecraft.init.Items.feather)
                        is net.minecraft.entity.passive.EntityWolf -> net.minecraft.item.ItemStack(net.minecraft.init.Items.bone)
                        is net.minecraft.entity.passive.EntityOcelot -> net.minecraft.item.ItemStack(net.minecraft.init.Items.fish)
                        is net.minecraft.entity.passive.EntityHorse -> net.minecraft.item.ItemStack(net.minecraft.init.Items.leather)
                        else -> net.minecraft.item.ItemStack(net.minecraft.init.Items.bone)
                    }
                }
                is net.minecraft.entity.passive.EntityVillager -> net.minecraft.item.ItemStack(net.minecraft.init.Items.emerald)
                else -> {
                    try {
                        val spawnEgg = net.minecraft.item.ItemStack(net.minecraft.init.Items.spawn_egg)
                        spawnEgg
                    } catch (e: Exception) {
                        net.minecraft.item.ItemStack(net.minecraft.init.Items.bone)
                    }
                }
            }

            mc.renderItem.renderItemAndEffectIntoGUI(itemStack, 0, 0)

            RenderHelper.disableStandardItemLighting()
            GlStateManager.disableRescaleNormal()
            GlStateManager.disableBlend()
            GL11.glColor4f(1f, 1f, 1f, 1f)
            GL11.glPopMatrix()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val defaultIcon = net.minecraft.item.ItemStack(net.minecraft.init.Items.bone)
                mc.renderItem.renderItemAndEffectIntoGUI(defaultIcon, 0, 0)
            } catch (e2: Exception) {
            }
            try { GL11.glPopMatrix() } catch (ignored: Exception) {}
            GlStateManager.disableRescaleNormal()
            GlStateManager.disableBlend()
            GL11.glColor4f(1f, 1f, 1f, 1f)
        }
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm").format(Date())
    }

    private fun updateAnimations() {
        breathAnimation += 0.05f
        if (breathAnimation > Math.PI * 2) breathAnimation = 0f

        if (pulseAnimation > 0) {
            pulseAnimation = lerp(pulseAnimation, 0f, 0.05f)
        }

        if (isInTransition) {
            val elapsed = System.currentTimeMillis() - transitionStartTime
            if (elapsed < 300) {
                stateTransitionAnimation = smoothStep(elapsed / 300f)
            } else if (elapsed < 600) {
                stateTransitionAnimation = 1f - smoothStep((elapsed - 300) / 300f)
            } else {
                isInTransition = false
                stateTransitionAnimation = 0f
            }
        }

        for (notification in currentNotifications) {
            if (notification.type == NotificationType.MODULE_ENABLED || notification.type == NotificationType.MODULE_DISABLED) {
                if (notification.switchAnimation < 1f) {
                    notification.switchAnimation = lerp(notification.switchAnimation, 1f, 0.15f)
                }
            }
        }

        glowAnimation = if (!constantGlow) {
            (0.5f + sin(breathAnimation + 0.5) * 0.3f).toFloat()
        } else {
            1.0f
        }

        if (showGappleIndicator) {
            updateGappleIndicator()
        }
    }

    private fun updateGappleIndicator() {
        val gappleModule = OldGrimGapple

        val isEating = gappleModule.isEatingGapple
        val shouldShow = gappleModule.shouldShowIndicator
        val shouldBeVisible = isEating && shouldShow && gappleModule.state

        if (shouldBeVisible) {
            gappleSeparationProgress = lerp(gappleSeparationProgress, 1f, gappleAnimationSpeed)
            gappleScaleAnimation = lerp(gappleScaleAnimation, 1f, gappleAnimationSpeed * 1.5f)
        } else {
            gappleSeparationProgress = lerp(gappleSeparationProgress, 0f, gappleAnimationSpeed)
            if (gappleSeparationProgress <= 0.01f) {
                gappleScaleAnimation = lerp(gappleScaleAnimation, 0f, gappleAnimationSpeed * 1.5f)
            }
        }

        gappleSeparationProgress = gappleSeparationProgress.coerceIn(0f, 1f)
        gappleScaleAnimation = gappleScaleAnimation.coerceIn(0f, 1f)
    }

    private fun drawSmoothCircle(centerX: Float, centerY: Float, radius: Float, color: Color) {
        val segments = 100
        val anglePerSegment = (2 * Math.PI) / segments

        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

        GL11.glBegin(GL11.GL_TRIANGLE_FAN)
        GL11.glVertex2f(centerX, centerY)

        for (i in 0..segments) {
            val angle = i * anglePerSegment
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            GL11.glVertex2f(x, y)
        }

        GL11.glEnd()
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
    }

    private fun drawGappleIndicator() {
        val gappleModule = OldGrimGapple

        val progress = gappleModule.eatingProgress

        if (gappleScaleAnimation <= 0f) return

        val basePanelHeight = 26f
        val mainPanelTop = -basePanelHeight / 2
        val mainPanelBottom = mainPanelTop + basePanelHeight
        val panelCenterY = (mainPanelTop + mainPanelBottom) / 2

        val baseX = currentMainWidth / 2 + 6f
        val targetX = baseX + gappleSeparationDistance * gappleSeparationProgress
        val centerY = panelCenterY

        val baseIndicatorSize = gappleIndicatorSize
        val indicatorSize = baseIndicatorSize * gappleScaleAnimation
        val halfSize = indicatorSize / 2

        val alpha = (255 * gappleSeparationProgress * gappleScaleAnimation).toInt().coerceIn(0, 255)

        if (alpha <= 0) return

        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        val bgColor = applyDynamicAlpha(backgroundColor, alpha / 255f)
        drawSmoothCircle(targetX, centerY, halfSize, bgColor)

        val progressRadius = halfSize - 2
        val progressBgColor = applyDynamicAlpha(gappleProgressBackgroundColor, alpha / 255f)
        drawProgressRing(targetX, centerY, progressRadius, 1f, progressBgColor, gappleProgressThickness)

        if (progress > 0) {
            val progressColor = applyDynamicAlpha(gappleProgressColor, alpha / 255f)
            drawProgressRing(targetX, centerY, progressRadius, progress, progressColor, gappleProgressThickness)
        }

        if (gappleScaleAnimation > 0.1f) {
            drawGappleIcon(targetX, centerY, indicatorSize * 0.65f, alpha)
        }

        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()

        gappleIndicatorX = targetX
        gappleIndicatorY = centerY
    }

    private fun drawProgressRing(centerX: Float, centerY: Float, radius: Float, progress: Float, color: Color, thickness: Float) {
        val segments = 100
        val anglePerSegment = (2 * Math.PI) / segments
        val progressAngle = 2 * Math.PI * progress

        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
        GL11.glLineWidth(thickness)

        GL11.glBegin(GL11.GL_LINE_STRIP)

        for (i in 0..segments) {
            val angle = i * anglePerSegment
            if (angle > progressAngle) break

            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            GL11.glVertex2f(x, y)
        }

        GL11.glEnd()
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
    }

    private fun drawGappleIcon(centerX: Float, centerY: Float, size: Float, alpha: Int = 255) {
        val halfSize = size / 2

        GL11.glPushMatrix()
        GL11.glTranslatef(centerX - halfSize, centerY - halfSize, 0f)
        GL11.glScalef(size / 16f, size / 16f, 1f)

        GlStateManager.enableRescaleNormal()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        RenderHelper.enableGUIStandardItemLighting()

        val gappleStack = net.minecraft.item.ItemStack(net.minecraft.init.Items.golden_apple, 1, 0)

        GL11.glColor4f(1f, 1f, 1f, alpha / 255f)

        mc.renderItem.renderItemAndEffectIntoGUI(gappleStack, 0, 0)
        RenderHelper.disableStandardItemLighting()
        GlStateManager.disableRescaleNormal()
        GlStateManager.disableBlend()
        GL11.glColor4f(1f, 1f, 1f, 1f)
        GL11.glPopMatrix()
    }

    private fun updateNotificationSystem() {
        if (currentNotifications.isNotEmpty()) {
            val currentTime = System.currentTimeMillis()
            currentNotifications.removeAll { notification ->
                currentTime - notification.timestamp > notification.duration * 50L
            }
        }
    }

    private fun updateScaffoldInfo() {
        if (!showScaffoldBlocks) {
            shouldShowScaffoldExtension = false
            return
        }

        val isScaffoldEnabled = Scaffold.state || Scaffold2.state
        val shouldShowBlocks = Scaffold.showBlockCount || Scaffold2.showBlockCount
        val canShowScaffoldInfo = isScaffoldEnabled && shouldShowBlocks

        if (!canShowScaffoldInfo) {
            shouldShowScaffoldExtension = false
            scaffoldWasEnabled = false
            scaffoldProgressMaxInitialized = false
            return
        }

        scaffoldWasEnabled = true

        val currentTime = System.currentTimeMillis()
        val isHandlingEvent = Scaffold.handleEvents() || Scaffold2.handleEvents()

        if (isHandlingEvent && !scaffoldProgressMaxInitialized) {
            val blockCount = net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.blocksAmount()
            scaffoldMaxBlocks = max(blockCount, 100)
            scaffoldProgressMaxInitialized = true
            lastHandleEventTime = currentTime
            shouldShowScaffoldExtension = true

            triggerShake()
        }

        scaffoldUpdateTimer++
        if (scaffoldUpdateTimer < scaffoldUpdateInterval) return
        scaffoldUpdateTimer = 0

        val blockCount = net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.blocksAmount()

        if (blockCount != lastScaffoldBlockCount) {
            lastScaffoldBlockCount = blockCount
            shouldShowScaffoldExtension = true

            if (scaffoldProgressMaxInitialized && blockCount > scaffoldMaxBlocks) {
                scaffoldMaxBlocks = blockCount
                triggerShake()
            }
        }

        if (scaffoldProgressMaxInitialized && currentTime - lastHandleEventTime > 10000L) {
            scaffoldProgressMaxInitialized = false
        }
    }

    private fun updateKillAuraTargetInfo() {
        if (!showKillAuraTarget) {
            shouldShowKillAuraTarget = false
            return
        }

        val target = KillAura.target
        shouldShowKillAuraTarget = target != null
        lastTarget = target
    }

    private fun drawNormalContent(panelTop: Float, panelBottom: Float, panelWidth: Float) {
        if (panelWidth <= 0f) return

        val targetMainWidth = if (autoWidth) calculateMainContentWidth() else width
        val textProgress = if (targetMainWidth > 0f) (currentMainWidth / targetMainWidth).coerceIn(0f, 1f) else 0f

        if (textProgress < 0.3f) return

        val textAlpha = ((textProgress - 0.3f) / 0.7f * 255).toInt().coerceIn(0, 255)
        if (textAlpha <= 0) return

        val parts = mutableListOf<String>()

        if (showClientName) parts.add("FireBounce")
        if (showFPS) parts.add("${Minecraft.getDebugFPS()} FPS")
        if (showPlayerName && mc.thePlayer != null) parts.add(mc.thePlayer.name)
        if (showTime) parts.add(getCurrentTime())

        val ping = getPing()
        parts.add("${ping}ms")

        if (parts.isEmpty()) return

        val content = parts.joinToString(" · ")
        val textX = -mainFont.getStringWidth(content) / 2f
        val panelCenterY = (panelTop + panelBottom) / 2
        val textY = panelCenterY - mainFont.FONT_HEIGHT / 2 + 1

        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        val textColorWithAlpha = Color(textColor.red, textColor.green, textColor.blue, textAlpha)
        val shadowColor = Color(0, 0, 0, (textAlpha * 0.3).toInt())

        mainFont.drawString(content, textX + 0.5f, textY + 0.5f, shadowColor.rgb, false)
        mainFont.drawString(content, textX, textY, textColorWithAlpha.rgb, false)

        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
    }

    private fun getPing(): Int {
        return if (mc.thePlayer != null) mc.thePlayer.getPing() else -1
    }

    private fun checkForNotifications() {
        if (!showModuleStatus) return
        checkModuleStatusChanges()
    }

    private fun checkModuleStatusChanges() {
        ModuleManager.forEach { module ->
            val lastState = lastModuleStates[module.name]
            if (lastState != null && lastState != module.state) {
                val type = if (module.state) NotificationType.MODULE_ENABLED else NotificationType.MODULE_DISABLED
                val onOffText = "ON".padEnd(3)
                val message = if (module.state) "${module.name} $onOffText" else "${module.name} OFF"

                if (stateTransitionEffect) {
                    triggerStateTransition()
                }

                val targetWidth = calculateNotificationWidth(Notification(type, message))
                val newNotification = Notification(
                    type,
                    message,
                    notificationDuration,
                    moduleName = module.name,
                    switchAnimation = 0f,
                    currentWidth = targetWidth,
                    timestamp = System.currentTimeMillis()
                )
                currentNotifications.add(newNotification)

                if (pulseOnNotification) triggerPulse()

                triggerShake()

                if (currentNotifications.size > maxNotifications) {
                    currentNotifications.removeAt(0)
                }
            }
            lastModuleStates[module.name] = module.state
        }
    }

    private fun triggerStateTransition() {
        isInTransition = true
        stateTransitionAnimation = 0f
        transitionStartTime = System.currentTimeMillis()
    }

    private fun triggerPulse() {
        pulseAnimation = 1f
    }

    private fun addNotification(notification: Notification) {
        currentNotifications.add(notification)
        if (currentNotifications.size > maxNotifications) {
            currentNotifications.removeAt(0)
        }
    }

    @Suppress("unused")
    fun showCustomNotification(message: String, duration: Int = notificationDuration) {
        val targetWidth = calculateNotificationWidth(Notification(NotificationType.CUSTOM, message))
        addNotification(Notification(NotificationType.CUSTOM, message, duration, moduleName = "custom_${System.currentTimeMillis()}", switchAnimation = 0f, currentWidth = targetWidth))
    }

    override fun updateElement() {
        if (lastModuleStates.isEmpty()) {
            ModuleManager.forEach { module ->
                lastModuleStates[module.name] = module.state
            }
        }
    }

    private var chestSlots: List<Slot>? = null

    private fun updateChestSlots() {
        if (mc.currentScreen is GuiChest) {
            val guiChest = mc.currentScreen as GuiChest
            val container = guiChest.inventorySlots

            chestSlots = container.inventorySlots.filter { slot ->
                slot.inventory != mc.thePlayer?.inventory &&
                        slot.slotNumber < container.inventorySlots.size - 36
            }
        } else {
            chestSlots = null
        }
    }

    private fun drawChestItemsView(): Border {
        val slots = chestSlots ?: return Border(0f, 0f, 0f, 0f)

        if (slots.isEmpty()) {
            return Border(0f, 0f, 0f, 0f)
        }

        val slotSize = 18f
        val padding = 5f
        val columns = 9
        val rows = (slots.size + 8) / 9

        val totalWidth = columns * slotSize + (columns + 1) * padding
        val totalHeight = rows * slotSize + (rows + 1) * padding

        val x = -totalWidth / 2
        val y = -totalHeight / 2

        drawRoundedRect(
            x, y,
            x + totalWidth, y + totalHeight,
            backgroundColor.rgb,
            cornerRadius,
            RenderUtils.RoundedCorners.ALL
        )

        GlStateManager.enableRescaleNormal()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        RenderHelper.enableGUIStandardItemLighting()

        slots.forEachIndexed { index, slot ->
            if (slot.stack != null) {
                val col = index % columns
                val row = index / columns

                val itemX = x + padding + col * (slotSize + padding)
                val itemY = y + padding + row * (slotSize + padding)

                mc.renderItem.renderItemAndEffectIntoGUI(slot.stack, itemX.toInt() + 1, itemY.toInt() + 1)
                mc.renderItem.renderItemOverlayIntoGUI(mc.fontRendererObj, slot.stack, itemX.toInt() + 1, itemY.toInt() + 1, null)
            }
        }

        RenderHelper.disableStandardItemLighting()
        GlStateManager.disableRescaleNormal()
        GlStateManager.disableBlend()

        return Border(x, y, x + totalWidth, y + totalHeight)
    }
}