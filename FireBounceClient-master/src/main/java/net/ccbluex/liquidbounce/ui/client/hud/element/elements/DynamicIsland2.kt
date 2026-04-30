/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
@file:Suppress("unused", "KotlinConstantConditions", "SameParameterValue")

package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.client.ServerUtils
import net.ccbluex.liquidbounce.utils.render.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawImage
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.*

@ElementInfo(name = "DynamicIsland2")
class DynamicIsland2 : Element("DynamicIsland2") {

    // 基本设置
    private val clientName by text("ClientName", "Opai")
    private val backgroundColorAlpha by int("BackgroundAlpha", 160, 0..255)
    private val cornerRadius by float("CornerRadius", 14f, 8f..20f)
    private val animationTension by float("AnimationTension", 0.05f, 0.01f..1.0f)
    private val animationFriction by float("AnimationFriction", 0.3f, 0.01f..1.0f)

    // 样式设置
    private val styles by choices("Style", arrayOf("Normal", "NewOpai", "Opal"), "Normal")
    private val accentColor by color("AccentColor", Color(41, 75, 255))

    // 服务器IP设置
    private val customIP by boolean("CustomIP", false)
    private val serverIP by text("ServerIP", "hidden.ip") { customIP }

    // 阴影设置
    private val shadowEnabled by boolean("Shadow", false)
    private val shadowRadius by float("ShadowRadius", 15f, 1f..50f)

    // 模糊设置
    private val blurEnabled by boolean("Blur", true)
    private val blurStrength by float("BlurStrength", 10f, 1f..50f)

    // 通知设置
    private val notificationsEnabled by boolean("Notifications", true)
    private val notificationDuration by int("NotificationDuration", 3000, 1000..10000)

    // Scaffold设置
    private val scaffoldEnabled by boolean("Scaffold", true)
    private val scaffoldThemeColor by color("ScaffoldTheme", Color(65, 130, 225))
    private val maxBlocks by int("MaxBlocks", 576, 64..576)

    // 箱子设置
    private val chestEnabled by boolean("Chest", true)
    private val chestRoundRadius by float("ChestRoundRadius", 4f, 0f..8f)

    // BPS更新间隔
    private val bpsUpdateInterval by int("BPSUpdateInterval", 100, 50..500)

    // 动画状态
    private var animX = 0f
    private var animY = 0f
    private var animWidth = 100f
    private var animHeight = 28f

    private var velocityX = 0f
    private var velocityY = 0f
    private var velocityWidth = 0f
    private var velocityHeight = 0f

    // 涟漪动画
    private data class SlotRipple(val x: Float, val y: Float, val startTime: Long)
    private val slotRipples = CopyOnWriteArrayList<SlotRipple>()
    private val prevSlotItems = HashMap<Int, ItemStack?>()
    private var lastChestContainerHash = 0

    // 常量
    private val ITEM_NOTIFY_HEIGHT = 38f
    private val NORMAL_WATERMARK_HEIGHT = 28f

    // 移动计算
    private var prevPosX = 0.0
    private var prevPosZ = 0.0
    private var animatedBPS = 0.0
    private var lastBPSUpdateTime = 0L
    private var displayedBPS = 0.0

    // 进度条动画
    private var progressBarWidth = 0f
    private var progressBarVelocity = 0f

    // 通知系统
    private val prevModuleStates = HashMap<net.ccbluex.liquidbounce.features.module.Module, Boolean>()
    private val notifications = CopyOnWriteArrayList<ModuleNotification>()

    // 弹簧动画函数
    private fun spring(current: Float, target: Float, velocity: Float): Pair<Float, Float> {
        val displacement = target - current
        val force = displacement * animationTension
        val drag = velocity * animationFriction
        val acceleration = force - drag
        val newVelocity = velocity + acceleration
        val newPosition = current + newVelocity
        return newPosition to newVelocity
    }

    private fun getSafePing(): Int {
        val player = mc.thePlayer ?: return 0
        return mc.netHandler?.getPlayerInfo(player.uniqueID)?.responseTime ?: 0
    }

    private fun drawCircle(x: Float, y: Float, radius: Float, color: Color) {
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)

        GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

        GL11.glBegin(GL11.GL_POLYGON)
        for (i in 0..360 step 10) {
            val theta = i * Math.PI / 180
            GL11.glVertex2d((x + radius * cos(theta)), (y + radius * sin(theta)))
        }
        GL11.glEnd()

        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    private fun drawCenteredDot(x: Float, textBaseY: Float) {
        val dotY = textBaseY - Fonts.fontGoogleSans40.FONT_HEIGHT / 2 + 3f
        Fonts.fontGoogleSans40.drawString("·", x - 3f, dotY, Color(180, 180, 180, 255).rgb)
    }

    private fun drawShadow(x: Float, y: Float, w: Float, h: Float) {
        if (shadowEnabled) {
            GlowUtils.drawGlow(x, y, w, h, shadowRadius.toInt(), Color(0, 0, 0, 120))
        }
    }

    private enum class Direction { FORWARDS, BACKWARDS }

    private class EaseOutExpo(private val duration: Long, private val end: Double) {
        private var start = 0.0
        private var startTime = System.currentTimeMillis()
        private var direction = Direction.FORWARDS

        fun setDirection(dir: Direction) {
            if (this.direction != dir) {
                this.direction = dir
                startTime = System.currentTimeMillis()
                start = getOutput()
            }
        }

        fun getOutput(): Double {
            val progress = (System.currentTimeMillis() - startTime).toDouble() / duration
            val result = when (direction) {
                Direction.FORWARDS -> if (progress >= 1.0) end else ((-2.0).pow(-10 * progress) + 1) * end
                Direction.BACKWARDS -> if (progress >= 1.0) 0.0 else (2.0.pow(-10 * progress) * end)
            }
            return result.coerceIn(0.0, end)
        }
    }

    private class SwitchAnimationState {
        private val animation = EaseOutExpo(300, 1.0)

        fun updateState(state: Boolean) = animation.setDirection(if (state) Direction.FORWARDS else Direction.BACKWARDS)
        fun getOutput() = animation.getOutput()
    }

    private abstract inner class Notification(
        val id: String = UUID.randomUUID().toString(),
        var title: String,
        var message: String,
        var createTime: Long = System.currentTimeMillis(),
        var duration: Long = 3000L
    ) {
        var isMarkedForDelete = false
        abstract fun draw(x: Float, y: Float)

        open fun updateState(newMsg: String, newEnable: Boolean, newDuration: Long) {
            this.message = newMsg
            this.createTime = System.currentTimeMillis()
            this.duration = newDuration
        }

        fun getHeight() = ITEM_NOTIFY_HEIGHT

        fun update() {
            if (System.currentTimeMillis() > createTime + duration) {
                isMarkedForDelete = true
            }
        }
    }

    private inner class ModuleNotification(
        t: String,
        m: String,
        d: Long,
        var enabled: Boolean,
        val moduleName: String
    ) : Notification(title = t, message = m, duration = d) {
        val anim = SwitchAnimationState()

        init {
            anim.updateState(enabled)
        }

        override fun updateState(newMsg: String, newEnable: Boolean, newDuration: Long) {
            super.updateState(newMsg, newEnable, newDuration)
            this.enabled = newEnable
            anim.updateState(newEnable)
        }

        override fun draw(x: Float, y: Float) {
            drawToggleButton(x, y, 0f, enabled, anim)
            drawToggleText(x, y, Pair(title, message), 0f)
        }
    }

    private fun drawToggleButton(startX: Float, startY: Float, containerH: Float, moduleState: Boolean, animationState: SwitchAnimationState) {
        val btnH = 19f
        val btnW = 30f
        val margin = 3f
        val radius = btnH / 2
        val btnStartY = startY + (ITEM_NOTIFY_HEIGHT - btnH) / 2

        animationState.updateState(moduleState)
        val anim = animationState.getOutput()

        val trackColor = if (moduleState)
            Color(accentColor.red, accentColor.green, accentColor.blue, 255)
        else
            Color(45, 45, 45, 255)

        drawRoundedBorderRect(startX, btnStartY, startX + btnW, btnStartY + btnH,
            0.1f, trackColor.rgb, trackColor.rgb, radius)

        val knobSize = btnH - margin * 2
        val knobX = startX + margin + (btnW - margin * 2 - knobSize) * anim.toFloat()
        val knobColor = if (moduleState) Color.WHITE.rgb else Color(100, 100, 100, 255).rgb

        drawRoundedBorderRect(knobX, btnStartY + margin, knobX + knobSize,
            btnStartY + margin + knobSize, 0.1f, knobColor, knobColor, knobSize / 2)
    }

    private fun drawToggleText(startX: Float, startY: Float, textPair: Pair<String, String>, containerH: Float) {
        val titleH = 9f
        val textStartX = startX + 30f + 8f
        val center = startY + ITEM_NOTIFY_HEIGHT / 2

        Fonts.fontGoogleSans40.drawString(textPair.first, textStartX, center - titleH + 1f, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString(textPair.second, textStartX, center + 3f, Color.WHITE.rgb)
    }

    private fun showModuleNotification(title: String, message: String, enabled: Boolean, moduleName: String) {
        val existing = notifications.find { it.moduleName == moduleName }
        val duration = notificationDuration.toLong()

        if (existing != null) {
            existing.updateState(message, enabled, duration)
        } else {
            notifications.add(ModuleNotification(title, message, duration, enabled, moduleName))
        }
    }

    private fun updateNotifications() {
        notifications.forEach { it.update() }
        notifications.removeAll { it.isMarkedForDelete }
    }

    private fun calculateMaxNotificationWidth(): Float {
        if (notifications.isEmpty()) return 0f

        var maxWidth = 0f
        val fixedElementWidth = 30f + 15f + 30f

        for (notification in notifications) {
            val titleWidth = Fonts.fontGoogleSans40.getStringWidth(notification.title).toFloat()
            val descWidth = Fonts.fontRegular35.getStringWidth(notification.message).toFloat()
            val textWidth = max(titleWidth, descWidth)
            val totalWidth = fixedElementWidth + textWidth
            maxWidth = max(maxWidth, totalWidth)
        }

        return maxWidth
    }

    private fun calculateNormal3Info(): Normal3Info {
        val username = mc.session?.username ?: "Unknown"
        val fps = Minecraft.getDebugFPS()
        val ping = getSafePing()
        val ipStr = if (customIP) serverIP else ServerUtils.remoteIp

        val clientNameWidth = Fonts.fontGoogleSans40.getStringWidth(clientName).toFloat()
        val usernameWidth = Fonts.fontGoogleSans40.getStringWidth(username).toFloat() - 1f
        val pingStr = "${ping}ms"
        val pingTextWidth = Fonts.fontGoogleSans40.getStringWidth(pingStr).toFloat()
        val toTextWidth = Fonts.fontGoogleSans40.getStringWidth("  to  ").toFloat()
        val serverIpWidth = Fonts.fontGoogleSans40.getStringWidth(ipStr).toFloat()
        val fpsStr = "${fps}fps"
        val fpsTextWidth = Fonts.fontGoogleSans40.getStringWidth(fpsStr).toFloat()

        val padding = 13f
        val icon = 15f
        val space = 8f
        val dot = 13f
        val dotW = 4f

        val width = padding + icon + space + clientNameWidth + dot + dotW +
                icon + space + usernameWidth + dot + dotW +
                icon + space + pingTextWidth + toTextWidth + serverIpWidth + dot + 3f +
                icon + space + fpsTextWidth + padding

        return Normal3Info(
            width = width,
            username = username,
            clientNameWidth = clientNameWidth,
            usernameWidth = usernameWidth,
            pingStr = pingStr,
            pingTextWidth = pingTextWidth,
            toTextWidth = toTextWidth,
            ipStr = ipStr,
            serverIpWidth = serverIpWidth,
            fpsStr = fpsStr,
            fpsTextWidth = fpsTextWidth
        )
    }

    data class Normal3Info(
        val width: Float,
        val padding: Float = 13f,
        val elementSpacing: Float = 8f,
        val dotSpacing: Float = 13f,
        val username: String,
        val clientNameWidth: Float,
        val usernameWidth: Float,
        val pingStr: String,
        val pingTextWidth: Float,
        val toTextWidth: Float,
        val ipStr: String,
        val serverIpWidth: Float,
        val fpsStr: String,
        val fpsTextWidth: Float
    )

    private fun renderNormalContent(x: Float, y: Float, w: Float, h: Float) {
        val username = mc.session.username
        val fps = Minecraft.getDebugFPS()
        val ping = getSafePing()
        val colorRGB = Color(accentColor.red, accentColor.green, accentColor.blue, 255)
        val text = " | $username | ${fps}fps | ${ping}ms"
        val mainText = clientName

        val itemHeight = 38f
        val wCalc = 20f + 18f + 5f + Fonts.fontGoogleSans40.getStringWidth(mainText + text) + 10f
        val startX = (w - wCalc) / 2

        drawShadow(startX, y, wCalc, itemHeight)
        drawRoundedBorderRect(startX, y, startX + wCalc, y + itemHeight, 0.5f,
            Color(10, 10, 10, backgroundColorAlpha).rgb,
            Color(30, 30, 30, backgroundColorAlpha).rgb, itemHeight / 2)

        drawImage(ResourceLocation("liquidbounce/logo_icon.png"),
            (startX + 5).toInt(), (y + (itemHeight - 18) / 2).toInt(), 18, 18, colorRGB)

        Fonts.fontGoogleSans40.drawString(mainText, startX + 28, y + (itemHeight - 9) / 2 + 1, colorRGB.rgb)
        Fonts.fontGoogleSans40.drawString(text, startX + 28 + Fonts.fontGoogleSans40.getStringWidth(mainText),
            y + (itemHeight - 9) / 2 + 1, -1)
    }

    private fun renderNormal3Content(x: Float, y: Float, w: Float, h: Float) {
        val info = calculateNormal3Info()
        val textBaseY = y + (h - Fonts.fontGoogleSans40.FONT_HEIGHT) / 2 + Fonts.fontGoogleSans40.FONT_HEIGHT - 8
        val iconSize = 15f
        val iconY = y + (h - iconSize) / 2
        var currentX = x + info.padding

        val colorRGB = Color(accentColor.red, accentColor.green, accentColor.blue, 255)

        // 绘制客户端图标
        drawImage(ResourceLocation("liquidbounce/watermark_images/logo_icon.png"),
            currentX, iconY, 15, 15, colorRGB)
        currentX += 15f + info.elementSpacing

        // 绘制客户端名称
        Fonts.fontGoogleSans40.drawString(clientName, currentX, textBaseY, colorRGB.rgb)
        currentX += info.clientNameWidth + info.dotSpacing

        // 绘制分隔点
        drawCenteredDot(currentX, textBaseY)
        currentX += 4f

        // 绘制用户图标
        drawImage(ResourceLocation("liquidbounce/watermark_images/user.png"),
            currentX, iconY, 15, 15, Color.WHITE)
        currentX += 15f + info.elementSpacing

        // 绘制用户名
        Fonts.fontGoogleSans40.drawString(info.username, currentX - 1f, textBaseY, Color.WHITE.rgb)
        currentX += info.usernameWidth + info.dotSpacing

        // 绘制分隔点
        drawCenteredDot(currentX, textBaseY)
        currentX += 4f

        // 绘制延迟图标
        drawImage(ResourceLocation("liquidbounce/watermark_images/ms.png"),
            currentX, iconY, 15, 15, Color.GREEN)
        currentX += 15f + info.elementSpacing

        // 绘制延迟文本
        Fonts.fontGoogleSans40.drawString(info.pingStr, currentX, textBaseY, Color.GREEN.rgb)
        currentX += info.pingTextWidth

        // 绘制"to"文本
        Fonts.fontGoogleSans40.drawString("  to  ", currentX, textBaseY, Color.WHITE.rgb)
        currentX += info.toTextWidth

        // 绘制服务器IP
        Fonts.fontGoogleSans40.drawString(info.ipStr, currentX, textBaseY, Color.WHITE.rgb)
        currentX += info.serverIpWidth + info.dotSpacing

        // 绘制分隔点
        drawCenteredDot(currentX - 1, textBaseY)
        currentX += 3f

        // 绘制FPS图标
        drawImage(ResourceLocation("liquidbounce/watermark_images/fps.png"),
            currentX, iconY, 15, 15, Color.WHITE)
        currentX += 15f + info.elementSpacing

        // 绘制FPS文本
        Fonts.fontGoogleSans40.drawString(info.fpsStr, currentX, textBaseY, Color.WHITE.rgb)
    }

    private fun renderScaffoldContent(x: Float, y: Float, w: Float, h: Float) {
        val totalBlockCount = mc.thePlayer.inventory.mainInventory
            .filterNotNull()
            .filter { it.item is ItemBlock }
            .sumOf { it.stackSize }

        val percentage = (totalBlockCount.toFloat() / maxBlocks.toFloat()).coerceIn(0f, 1f)
        val targetBPS = displayedBPS

        animatedBPS += (targetBPS - animatedBPS) * 0.15 * (Minecraft.getDebugFPS() / 20.0).coerceIn(0.1, 2.0)

        val padding = 8f
        val cornerRadius = 6f
        val iconSize = 32f
        val iconBgX = x + padding
        val iconBgY = y + padding

        val themeColor = Color(scaffoldThemeColor.red, scaffoldThemeColor.green, scaffoldThemeColor.blue, 200)
        drawRoundedRect(iconBgX, iconBgY, iconBgX + iconSize, iconBgY + iconSize,
            themeColor.rgb, cornerRadius - 1)

        val blockImgSize = 24
        drawImage(ResourceLocation("liquidbounce/watermark_images/block.png"),
            (iconBgX + (iconSize - blockImgSize) / 2).toInt(),
            (iconBgY + (iconSize - blockImgSize) / 2 + 1).toInt(),
            blockImgSize, blockImgSize, Color.WHITE)

        val textX = iconBgX + iconSize + 8f
        val titleY = y + padding + 2f

        Fonts.fontGoogleSans40.drawString("Scaffold Toggled", textX, titleY, Color.WHITE.rgb)

        val bpsText = String.format("%.2f", if (animatedBPS < 0.01) 0.0 else animatedBPS)
        Fonts.fontRegular40.drawString("$totalBlockCount blocks - $bpsText block/s",
            textX, titleY + Fonts.fontGoogleSans45.FONT_HEIGHT + 2f, Color(200, 200, 200).rgb)

        val barHeight = 8f
        val barY = y + h - barHeight - padding
        val maxBarWidth = w - (padding * 2)
        val targetBarWidth = maxBarWidth * percentage

        val (nextBarW, vBar) = spring(progressBarWidth, targetBarWidth, progressBarVelocity)
        progressBarWidth = nextBarW.coerceIn(0f, maxBarWidth)
        progressBarVelocity = vBar

        drawRoundedRect(x + padding, barY, x + padding + maxBarWidth, barY + barHeight,
            Color(60, 60, 70, 180).rgb, 3f)

        val lighter = Color(
            (scaffoldThemeColor.red + 50).coerceAtMost(255),
            (scaffoldThemeColor.green + 50).coerceAtMost(255),
            (scaffoldThemeColor.blue + 50).coerceAtMost(255),
            255
        )

        drawRoundedRect(x + padding, barY, x + padding + progressBarWidth, barY + barHeight,
            lighter.rgb, 3f)
    }

    private fun renderNotificationStack(x: Float, y: Float, w: Float, h: Float) {
        var currentYOffset = 0f
        val centerXOffset = 10f

        for (notification in notifications) {
            val rowY = y + currentYOffset
            notification.draw(x + centerXOffset, rowY)
            currentYOffset += ITEM_NOTIFY_HEIGHT
        }
    }

    private fun renderChestContent(x: Float, y: Float, w: Float, h: Float, slots: List<Slot>) {
        val padding = 8f
        val slotSize = 16
        val rippleDuration = 600L
        val maxRadius = 16f

        GlStateManager.enableRescaleNormal()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting()

        try {
            slots.forEachIndexed { index, slot ->
                val stack = slot.stack

                val col = index % 9
                val row = index / 9
                val itemX = (x + padding + col * slotSize).toInt()
                val itemY = (y + padding + row * slotSize).toInt()

                val prevStack = prevSlotItems[index]
                if (prevSlotItems.containsKey(index)) {
                    val isChanged = when {
                        stack == null && prevStack == null -> false
                        stack == null || prevStack == null -> true
                        else -> !ItemStack.areItemStacksEqual(stack, prevStack) || stack.stackSize != prevStack.stackSize
                    }

                    if (isChanged) {
                        slotRipples.add(SlotRipple((itemX + 8).toFloat(), (itemY + 8).toFloat(), System.currentTimeMillis()))
                    }
                }

                prevSlotItems[index] = stack?.copy()

                if (stack != null) {
                    if (mc.currentScreen is net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner) {
                        GL11.glDisable(GL11.GL_DEPTH_TEST)
                    }

                    mc.renderItem.renderItemAndEffectIntoGUI(stack, itemX, itemY)
                    mc.renderItem.renderItemOverlays(mc.fontRendererObj, stack, itemX, itemY)

                    if (mc.currentScreen is net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner) {
                        GL11.glEnable(GL11.GL_DEPTH_TEST)
                    }
                }
            }

            net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting()
            GL11.glDisable(GL11.GL_DEPTH)

            val currentTime = System.currentTimeMillis()
            val iterator = slotRipples.iterator()

            while (iterator.hasNext()) {
                val ripple = iterator.next()
                val timeAlive = currentTime - ripple.startTime

                if (timeAlive > rippleDuration) {
                    slotRipples.remove(ripple)
                } else {
                    val progress = timeAlive.toFloat() / rippleDuration.toFloat()
                    val ease = 1f - (1f - progress).pow(3)
                    val radius = maxRadius * ease
                    val alpha = (150 * (1f - progress)).toInt().coerceIn(0, 255)

                    if (alpha > 0) {
                        drawCircle(ripple.x, ripple.y, radius, Color(255, 255, 255, alpha))
                    }
                }
            }

            GL11.glEnable(GL11.GL_DEPTH)

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting()
            GlStateManager.enableAlpha()
            GlStateManager.disableBlend()
            GlStateManager.disableLighting()
        }
    }

    override fun drawElement(): Border {
        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)

        updateNotifications()

        val scaledScreen = ScaledResolution(mc)
        val screenWidth = scaledScreen.scaledWidth
        val screenHeight = scaledScreen.scaledHeight
        val startY = (screenHeight / 20).toFloat()

        val scaffoldModule = Scaffold

        val isChestOpen = mc.currentScreen is GuiChest && chestEnabled
        val chestSlots = if (isChestOpen) {
            (mc.currentScreen as GuiChest).inventorySlots?.inventorySlots
                ?.filter { it.inventory != mc.thePlayer?.inventory } ?: emptyList()
        } else emptyList()

        if (!isChestOpen) {
            prevSlotItems.clear()
            slotRipples.clear()
            lastChestContainerHash = 0
        } else {
            val currentContainerId = (mc.currentScreen as GuiChest).inventorySlots.windowId
            if (currentContainerId != lastChestContainerHash) {
                prevSlotItems.clear()
                slotRipples.clear()
                lastChestContainerHash = currentContainerId
            }
        }

        var targetWidth = 0f
        var targetHeight = 0f
        var targetX = 0f
        var targetY = startY
        var renderMode = "NONE"

        when {
            chestSlots.isNotEmpty() -> {
                renderMode = "CHEST"
                val columns = 9
                val rows = (chestSlots.size + 8) / 9
                val padding = 8f
                val slotSize = 16f
                targetWidth = columns * slotSize + padding * 2
                targetHeight = rows * slotSize + padding * 2
                targetX = (screenWidth - targetWidth) / 2
                targetY = startY.coerceIn(5f, screenHeight - targetHeight - 5f)
            }

            scaffoldModule.state && scaffoldEnabled -> {
                renderMode = "SCAFFOLD"
                targetWidth = 190f
                targetHeight = 58f
                targetX = (screenWidth - targetWidth) / 2
            }

            notifications.isNotEmpty() && notificationsEnabled && styles == "NewOpai" -> {
                renderMode = "NOTIFY_STACK"
                val maxWidth = calculateMaxNotificationWidth()
                targetWidth = max(maxWidth, 180f)
                targetHeight = (notifications.size * ITEM_NOTIFY_HEIGHT)
                targetX = (screenWidth - targetWidth) / 2
            }

            else -> {
                when (styles) {
                    "NewOpai" -> {
                        renderMode = "NORMAL_OPAI"
                        val info = calculateNormal3Info()
                        targetWidth = info.width
                        targetHeight = NORMAL_WATERMARK_HEIGHT
                        targetX = (screenWidth - targetWidth) / 2
                    }

                    "Normal" -> {
                        // 对于Normal样式，我们不需要动画容器，直接绘制
                        renderNormalContent(0f, startY, screenWidth.toFloat(), NORMAL_WATERMARK_HEIGHT)
                        if (notifications.isNotEmpty() && notificationsEnabled) {
                            renderNotificationStack(0f, startY + NORMAL_WATERMARK_HEIGHT + 2f, screenWidth.toFloat(), 0f)
                        }
                        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE)
                        GL11.glDisable(GL11.GL_LINE_SMOOTH)
                        GL11.glEnable(GL11.GL_TEXTURE_2D)
                        GL11.glDisable(GL11.GL_BLEND)
                        GL11.glPopMatrix()

                        // 返回一个空的边界
                        return Border(0f, 0f, 0f, 0f)
                    }

                    "Opal" -> {
                        // Opal style implementation
                        if (notifications.isNotEmpty() && notificationsEnabled) {
                            renderNotificationStack(0f, startY + 32f, screenWidth.toFloat(), 0f)
                        }
                        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE)
                        GL11.glDisable(GL11.GL_LINE_SMOOTH)
                        GL11.glEnable(GL11.GL_TEXTURE_2D)
                        GL11.glDisable(GL11.GL_BLEND)
                        GL11.glPopMatrix()

                        // 返回一个空的边界
                        return Border(0f, 0f, 0f, 0f)
                    }
                }
            }
        }

        if (renderMode != "NONE") {
            val (nextW, vW) = spring(animWidth, targetWidth, velocityWidth)
            animWidth = nextW.coerceAtLeast(0f)
            velocityWidth = vW

            val (nextH, vH) = spring(animHeight, targetHeight, velocityHeight)
            animHeight = nextH.coerceAtLeast(0f)
            velocityHeight = vH

            val (nextX, vX) = spring(animX, targetX, velocityX)
            animX = nextX
            velocityX = vX

            val (nextY, vY) = spring(animY, targetY, velocityY)
            animY = nextY
            velocityY = vY

            val currentRadius = if (animHeight > 30f) {
                if (renderMode == "CHEST") chestRoundRadius else 8f
            } else {
                animHeight / 2f
            }

            val drawX = animX
            val drawY = animY
            val drawW = animWidth
            val drawH = animHeight

            drawShadow(drawX, drawY, drawW, drawH)

            if (blurEnabled) {
                GlStateManager.pushMatrix()
                try {
                    val blurClass = Class.forName("net.ccbluex.liquidbounce.utils.render.InternalBlurShader")
                    val blurAreaMethod = blurClass.getMethod("blurArea", Float::class.java, Float::class.java, Float::class.java, Float::class.java, Float::class.java)
                    blurAreaMethod.invoke(null, drawX, drawY, drawW, drawH, blurStrength)
                } catch (e: Exception) {
                    // Fallback if blur shader is not available
                }
                GlStateManager.popMatrix()
            }

            drawRoundedBorderRect(
                drawX, drawY,
                drawX + drawW, drawY + drawH,
                0.1f,
                Color(0, 0, 0, backgroundColorAlpha).rgb,
                Color(0, 0, 0, backgroundColorAlpha).rgb,
                currentRadius
            )

            when (renderMode) {
                "SCAFFOLD" -> renderScaffoldContent(drawX, drawY, drawW, drawH)
                "NOTIFY_STACK" -> renderNotificationStack(drawX, drawY, drawW, drawH)
                "NORMAL_OPAI" -> renderNormal3Content(drawX, drawY, drawW, drawH)
                "CHEST" -> renderChestContent(drawX, drawY, drawW, drawH, chestSlots)
            }
        }

        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()

        return Border(
            animX,
            animY,
            animX + animWidth,
            animY + animHeight
        )
    }

    override fun updateElement() {
        if (mc.thePlayer == null || mc.theWorld == null) return

        val distanceX = mc.thePlayer.posX - prevPosX
        val distanceZ = mc.thePlayer.posZ - prevPosZ
        val currentCalculatedBPS = sqrt(distanceX.pow(2) + distanceZ.pow(2)) * 20.0

        if (System.currentTimeMillis() - lastBPSUpdateTime >= bpsUpdateInterval) {
            displayedBPS = currentCalculatedBPS
            lastBPSUpdateTime = System.currentTimeMillis()
        }

        prevPosX = mc.thePlayer.posX
        prevPosZ = mc.thePlayer.posZ

        if (notificationsEnabled) {
            for (module in ModuleManager) {
                if (!prevModuleStates.containsKey(module)) {
                    prevModuleStates[module] = module.state
                    continue
                }

                val prevState = prevModuleStates[module]!!
                val currentState = module.state

                if (prevState != currentState) {
                    prevModuleStates[module] = currentState

                    val titleText = "Module Toggled"
                    val modName = module.name
                    val stateText = if (currentState) "§aEnabled" else "§cDisabled"
                    val message = "§l$modName§r §fhas been §l$stateText§r §f!"

                    showModuleNotification(titleText, message, currentState, module.name)
                }
            }
        }
    }
}