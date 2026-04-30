package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.render.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedGradientRectCorner
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.item.ItemAppleGold
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import java.awt.Color
import kotlin.math.roundToInt

object OldGrimGapple : Module("OldGrimGapple", Category.COMBAT) {
    private val c by int("C03PacketPlayer", 32, 32..40)
    private val eatHealth by int("EatHealth", 12, 1..18)
    private val autoEat by boolean("AutoGapple", true)
    private val progressBar2 by boolean("ProgressBar2", false)
    private val stopWhenNoTarget by boolean("StopWhenNoTarget", true)
    private val stuck by boolean("Stuck", false)
    val isEatingGapple by boolean("DisplayStateInDynamicIsland", true)

    private val checkAbsorption by boolean("CheckAbsorption", true)
    private val minAbsorption by int("MinAbsorption", 0, 0..8)

    var eatingProgress = 0f
        private set

    var shouldShowIndicator = false
        private set

    private var x = 0.0
    private var y = 0.0
    private var z = 0.0
    private var cancelMove = false
    private var r = false
    private var ticks = 0
    private var pauseTicks = 0
    private var yaw = 0f
    private var pitch = 0f
    private var shouldEat = false
    var isEating = false

    private var slot = -1

    override fun onEnable() {
        shouldEat = false
        isEating = false
        eatingProgress = 0f
        shouldShowIndicator = false
        ticks = 0
        pauseTicks = 0
        stopStuck()
    }

    override fun onDisable() {
        ticks = 0
        pauseTicks = 0
        shouldEat = false
        isEating = false
        eatingProgress = 0f
        shouldShowIndicator = false
        stopStuck()
    }

    val onWorld = handler<WorldEvent> { event ->
        shouldEat = false
        isEating = false
        eatingProgress = 0f
        shouldShowIndicator = false
        ticks = 0
        pauseTicks = 0
        stopStuck()
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is C03PacketPlayer && cancelMove && ticks < c) {
            if (packet is C05PacketPlayerLook) {
                yaw = packet.yaw
                pitch = packet.pitch
            }
            ticks++
            event.cancelEvent()
        }
    }

    val onMove = handler<MoveEvent> { event ->
        if (cancelMove) {
            event.cancelEvent()
        }
    }

    val onUpdate = handler<UpdateEvent> {
        // 更新玩家视角（用于release函数）
        yaw = mc.thePlayer.rotationYaw
        pitch = mc.thePlayer.rotationPitch

        slot = getGApple()

        val shouldContinueEating = checkShouldEat()

        if (shouldContinueEating && slot >= 0) {
            isEating = true

            // 处理暂停逻辑
            if (pauseTicks > 0) {
                pauseTicks--
                if (pauseTicks <= 0 && stuck) {
                    stopStuck()
                }
                eatingProgress = 0f
                return@handler
            }

            // 处理stuck逻辑
            if (stuck && !cancelMove && pauseTicks == 0) {
                stuck()
            }

            // 增加计数器（无论是否开启stuck）
            if (ticks < c) {
                if (cancelMove) {
                    // 如果开启了stuck，ticks在onPacket中增加
                } else {
                    // 如果没开启stuck，直接增加ticks
                    ticks++
                }
            }

            eatingProgress = ticks.toFloat() / c.toFloat()
            shouldShowIndicator = true

            // 检查是否可以吃苹果
            if (ticks >= c) {
                // 切换到金苹果槽位并吃苹果
                sendPacket(C09PacketHeldItemChange(slot))
                sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getStackInSlot(slot)))

                // 释放之前积累的包（如果开启了stuck）
                if (stuck) {
                    release()
                }

                // 切换回原槽位
                sendPacket(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem)))

                // 重置计数器并设置暂停
                ticks = 0
                pauseTicks = 2  // 暂停2tick防止连续吃
                eatingProgress = 0f

                // 每20tick发送一次聊天消息
                if (mc.thePlayer.ticksExisted % 20 == 0) {
                    chat("§6Auto Eating...")
                }
            }
        } else {
            // 停止吃苹果
            shouldEat = false
            isEating = false
            if (stuck) {
                stopStuck()
            }
            ticks = 0
            pauseTicks = 0
            eatingProgress = 0f
            shouldShowIndicator = false

            if (shouldContinueEating && slot < 0) {
                if (mc.thePlayer.ticksExisted % 40 == 0) {
                    chat("§4NoGapple!")
                }
            }
        }
    }

    val onRender2D = handler<Render2DEvent> {
        if (isEating && progressBar2) {
            val scaledScreen = ScaledResolution(mc)
            val width = scaledScreen.scaledWidth.toFloat()
            val height = scaledScreen.scaledHeight.toFloat()
            drawProgressBar(width, height)
        }
    }

    private fun stuck() {
        if (!r) {
            x = mc.thePlayer.motionX
            y = mc.thePlayer.motionY
            z = mc.thePlayer.motionZ
            r = true
        }
        cancelMove = true
    }

    private fun stopStuck() {
        cancelMove = false
        if (r) {
            mc.thePlayer.motionX = x
            mc.thePlayer.motionY = y
            mc.thePlayer.motionZ = z
            r = false
        }
    }

    private fun release() {
        sendPacket(C05PacketPlayerLook(yaw, pitch, mc.thePlayer.onGround))
        repeat((ticks - 1).coerceAtLeast(0)) {
            sendPacket(C03PacketPlayer(mc.thePlayer.onGround))
        }
    }

    private fun getGApple(): Int {
        for (i in 0..8) {
            val stack = mc.thePlayer.inventory.getStackInSlot(i)
            if (stack != null && stack.item is ItemAppleGold) {
                return i
            }
        }
        return -1
    }

    /**
     * 检查是否应该吃金苹果
     */
    private fun checkShouldEat(): Boolean {
        // 如果关闭了自动吃，返回false
        if (!autoEat) {
            return false
        }

        val hasTarget = checkKillAuraTarget()
        val healthOk = checkHealthCondition()
        val absorptionOk = checkAbsorptionCondition()

        if (healthOk && hasTarget && absorptionOk && !shouldEat) {
            chat("§aAuto eating started")
            shouldEat = true
            isEating = true
        }

        if ((!healthOk || !hasTarget || !absorptionOk) && shouldEat) {
            chat("§eAuto eating stopped")
            shouldEat = false
            isEating = false
        }

        return shouldEat
    }

    /**
     * 检查血量条件
     */
    private fun checkHealthCondition(): Boolean {
        val currentHealth = mc.thePlayer.health.roundToInt()
        return currentHealth <= eatHealth
    }

    /**
     * 检查吸收值条件
     */
    private fun checkAbsorptionCondition(): Boolean {
        if (!checkAbsorption) {
            return true
        }

        // 获取玩家的吸收值
        val absorptionAmount = mc.thePlayer.absorptionAmount

        return absorptionAmount <= minAbsorption
    }

    /**
     * 检查KillAura是否有目标
     */
    private fun checkKillAuraTarget(): Boolean {
        return !stopWhenNoTarget || KillAura.target != null
    }


    private fun drawProgressBar(width: Float, height: Float) {
        val progressLength = 140F
        val startY = height / 4 * 3
        val startX = width / 2 - progressLength / 2

        val progressRatio = (ticks.toFloat() / c.toFloat()).coerceIn(0f, 1f)
        val currentProgress = progressLength * progressRatio
        val progressPercent = (progressRatio * 100).toInt()

        // 添加阴影效果
        showShadow(startX - 2, startY - 2, progressLength + 4, 11F, 0.3F)

        // 绘制进度条背景
        drawRoundedRect(startX, startY, startX + progressLength, startY + 7F, Color(0, 0, 0, 128).rgb, 2F)

        // 绘制进度条前景
        if (currentProgress != 0f) {
            drawRoundedGradientRectCorner(
                startX, startY,
                startX + currentProgress, startY + 7F,
                3f,
                Color(76, 157, 240, 255).rgb,
                Color(53, 200, 167, 255).rgb
            )
        }

        // 在进度条右侧显示百分比
        val percentText = "$progressPercent%"
        Fonts.fontGoogleSans35.drawString(
            percentText,
            startX + progressLength + 5,
            startY + 0,
            Color(255, 255, 255, 255).rgb,
            true
        )
    }
    private fun showShadow(startX: Float, startY: Float, width: Float, height: Float, shadowStrength: Float) {
        GlowUtils.drawGlow(
            startX, startY,
            width, height,
            (shadowStrength * 13F).toInt(),
            Color(0, 0, 0, 120)
        )
    }}