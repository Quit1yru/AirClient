package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiChat
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.roundToInt

@ElementInfo(name = "Target3")
class Target3 : Element("Target3") {

    // 设置选项
    private val backgroundColor by color("BackgroundColor", Color(20, 20, 20, 180))
    private val healthBarColor by color("HealthBarColor", Color(255, 80, 80))
    private val healthBarBackgroundColor by color("HealthBarBgColor", Color(50, 50, 50, 150))
    private val healthBarOutlineColor by color("HealthBarOutline", Color(30, 30, 30, 200))
    private val positiveDiffColor by color("PositiveDiffColor", Color(80, 255, 80))
    private val negativeDiffColor by color("NegativeDiffColor", Color(255, 80, 80))
    private val textColor by color("TextColor", Color.WHITE)
    private val nameColor by color("NameColor", Color(255, 215, 0))
    private val font by font("Font", Fonts.minecraftFont)
    private val headSize by int("HeadSize", 32, 16..48)
    private val elementWidth by int("Width", 180, 120..300)
    private val elementHeight by int("Height", 40, 30..60)
    private val healthBarHeight by int("HealthBarHeight", 6, 2..10)
    private val vanishDelay by int("VanishDelay", 400, 0..1000)

    // 动画相关变量
    private var delayCounter = 0
    private var lastTarget: EntityLivingBase? = null
    private var animatedHealthPercent = 0f
    private var lastRenderTime = 0L

    override fun drawElement(): Border {
        val target = getTarget() ?: return Border(0f, 0f, elementWidth.toFloat(), elementHeight.toFloat())

        // 计算时间差用于动画
        val currentTime = System.currentTimeMillis()
        lastRenderTime = currentTime

        // 计算血量信息
        val targetHealth = target.health + target.absorptionAmount
        val myHealth = mc.thePlayer.health + mc.thePlayer.absorptionAmount
        val healthDiff = myHealth - targetHealth
        val maxHealth = target.maxHealth + target.absorptionAmount
        val realHealthPercent = targetHealth / maxHealth

        // 平滑动画：血量百分比
        animatedHealthPercent += (realHealthPercent - animatedHealthPercent) * 0.2f

        // 保存OpenGL状态
        glPushMatrix()
        glPushAttrib(GL_ALL_ATTRIB_BITS)

        try {
            // 启用混合
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

            // 1. 绘制背景矩形
            RenderUtils.drawRect(0f, 0f, elementWidth.toFloat(), elementHeight.toFloat(), backgroundColor.rgb)

            // 2. 绘制左侧头像
            val padding = 4f
            val headX = padding
            val headY = (elementHeight - headSize) / 2f

            drawHead(target, headX, headY, headSize.toFloat())

            // 3. 绘制玩家名称（头像右侧）
            val playerName = target.name
            val nameText = "Name: $playerName"
            val contentX = headX + headSize + padding
            val nameY = headY + 2f
            font.drawString(nameText, contentX.roundToInt(), nameY.roundToInt(), nameColor.rgb)

            // 4. 绘制真实血量值（名称下方）
            val healthText = String.format("%.1f", targetHealth)
            val healthTextY = nameY + font.FONT_HEIGHT + 2f
            font.drawString(healthText, contentX.roundToInt(), healthTextY.roundToInt(), textColor.rgb)

            // 5. 绘制血量差（最右侧，与血量值同一行）
            val diffText = if (healthDiff >= 0) {
                String.format("+%.1f", healthDiff)
            } else {
                String.format("%.1f", healthDiff)
            }

            val diffColor = if (healthDiff >= 0) positiveDiffColor else negativeDiffColor
            val diffTextWidth = font.getStringWidth(diffText)
            val diffX = elementWidth - padding - diffTextWidth

            font.drawString(diffText, diffX.roundToInt(), healthTextY.roundToInt(), diffColor.rgb)

            // 6. 绘制血条（在背景底部内部，美观的现代风格）
            val healthBarY = elementHeight - healthBarHeight - padding  // 在背景内部底部
            val healthBarStartX = padding  // 从左侧padding开始
            val healthBarEndX = elementWidth - padding  // 到右侧padding结束
            val healthBarWidth = healthBarEndX - healthBarStartX

            // 确保血条宽度为正
            if (healthBarWidth > 0) {
                // 绘制血条外框
                RenderUtils.drawRect(
                    healthBarStartX - 1f,
                    healthBarY - 1f,
                    healthBarEndX + 1f,
                    healthBarY + healthBarHeight + 1f,
                    healthBarOutlineColor.rgb
                )

                // 血条背景
                RenderUtils.drawRect(
                    healthBarStartX,
                    healthBarY,
                    healthBarEndX,
                    healthBarY + healthBarHeight,
                    healthBarBackgroundColor.rgb
                )

                // 血条填充（使用动画百分比）
                if (animatedHealthPercent > 0) {
                    val filledWidth = healthBarWidth * animatedHealthPercent.coerceIn(0f, 1f)

                    // 绘制渐变效果
                    RenderUtils.drawRect(
                        healthBarStartX,
                        healthBarY,
                        healthBarStartX + filledWidth,
                        healthBarY + healthBarHeight,
                        healthBarColor.rgb
                    )

                    // 在血条顶部添加高光效果
                    RenderUtils.drawRect(
                        healthBarStartX,
                        healthBarY,
                        healthBarStartX + filledWidth,
                        healthBarY + 1f,
                        Color(255, 140, 140).rgb
                    )
                }
            }

        } finally {
            // 恢复OpenGL状态
            glPopAttrib()
            glPopMatrix()
        }

        return Border(0f, 0f, elementWidth.toFloat(), elementHeight.toFloat())
    }

    private fun drawHead(target: EntityLivingBase, x: Float, y: Float, size: Float) {
        val texture = mc.renderManager.getEntityRenderObject<Entity>(target)?.getEntityTexture(target) ?: return

        glPushMatrix()
        glPushAttrib(GL_ALL_ATTRIB_BITS)

        try {
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            // 绘制头像
            RenderUtils.drawHead(
                texture,
                x.toInt(),
                y.toInt(),
                8f,
                8f,
                8,
                8,
                size.toInt(),
                size.toInt(),
                64f,
                64f,
                Color.WHITE
            )

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            glPopAttrib()
            glPopMatrix()
        }
    }

    private fun getTarget(): EntityLivingBase? {
        val killAuraTarget = KillAura.target.takeIf { it is EntityPlayer }

        // 当打开聊天栏时强制显示
        val shouldRender = KillAura.handleEvents() && killAuraTarget != null || mc.currentScreen is GuiChat

        val target = killAuraTarget ?: if (delayCounter >= vanishDelay) {
            // 重置动画状态
            animatedHealthPercent = 0f
            null
        } else {
            lastTarget
        }

        if (shouldRender) {
            delayCounter = 0
            lastTarget = killAuraTarget
        } else {
            delayCounter++
        }

        return target
    }
}