/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.getHealth
import net.ccbluex.liquidbounce.utils.extensions.lerpWith
import net.ccbluex.liquidbounce.utils.extensions.safeDiv
import net.ccbluex.liquidbounce.utils.extra.ColorUtils
import net.ccbluex.liquidbounce.utils.extra.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawGradientRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawHead
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.withClipping
import net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

@ElementInfo(name = "Target")
class Target : Element("Target") {

    // 基本样式设置
    private val roundedRectRadius by float("Rounded-Radius", 3F, 0F..5F)
    private val borderStrength by float("Border-Strength", 2F, 1F..5F)

    // 血条显示模式设置
    private val healthBarDisplayMode by choices("HealthBar-DisplayMode", arrayOf("Mode1", "Mode2"), "Mode1")

    // 背景颜色设置
    private val backgroundMode by choices("Background-ColorMode", arrayOf("Custom", "Rainbow"), "Custom")
    private val backgroundColor by color("Background-Color", Color.BLACK.withAlpha(150)) { backgroundMode == "Custom" }
    private val shadowcheck by boolean("Shadowcheck", false)
    private val shadowStrength by float("ShadowStrength", 0.5f, 1.0f..5.0f) {shadowcheck}
    // 生命值条设置
    private val healthBarColor1 by color("HealthBar-Gradient1", Color(3, 65, 252))
    private val healthBarColor2 by color("HealthBar-Gradient2", Color(3, 252, 236))
    private val roundHealthBarShape by boolean("RoundHealthBarShape", true)

    // 边框设置
    private val borderColor by color("Border-Color", Color.BLACK)

    // 文本设置
    private val textColor by color("TextColor", Color.WHITE)
    private val titleFont by font("TitleFont", Fonts.fontSemibold40)
    private val healthFont by font("HealthFont", Fonts.fontRegular30)
    private val textShadow by boolean("TextShadow", false)

    // 动画设置
    private val fadeSpeed by float("FadeSpeed", 2F, 1F..9F)
    private val animation by choices("Animation", arrayOf("Smooth", "Fade"), "Fade")
    private val animationSpeed by float("AnimationSpeed", 0.2F, 0.05F..1F)
    private val vanishDelay by int("VanishDelay", 300, 0..500)

    // 目标信息设置
    private val absorption by boolean("Absorption", true)
    private val healthFromScoreboard by boolean("HealthFromScoreboard", true)

    // 新增功能设置
    private val showExactHealth by boolean("ShowExactHealth", true) // 显示精确生命值
    private val showEnchantments by boolean("ShowEnchantments", true) // 显示附魔
    private val showEquipment by boolean("ShowEquipment", true)
    private val equipmentScale by float("EquipmentScale", 0.8f, 0.5f..1.5f)
    private val compactMode by boolean("CompactMode", false)

    // 彩虹效果设置
    private val rainbowX by float("Rainbow-X", -1000F, -2000F..2000F) { backgroundMode == "Rainbow" }
    private val rainbowY by float("Rainbow-Y", -1000F, -2000F..2000F) { backgroundMode == "Rainbow" }

    // 状态变量
    private var easingHealth = 0F
    private var lastTarget: EntityLivingBase? = null
    private var width = 0f
    private var height = 0f
    private var alphaText = 0
    private var alphaBackground = 0
    private var alphaBorder = 0
    private var delayCounter = 0
    private var easingHurtTime = 0F

    private val isRendered get() = width > 0f || height > 0f
    private val isAlpha get() = alphaBorder > 0 || alphaBackground > 0 || alphaText > 0
    private fun showShadow(startX: Float,startY: Float,width: Float,height:Float){
        if (shadowcheck) {
            GlowUtils.drawGlow(
                startX, startY,
                width, height,
                (shadowStrength * 13F).toInt(),
                Color(0, 0, 0, 120)
            )
        }
    }
    // 获取物品的附魔信息（简化版）
    private fun getEnchantments(itemStack: ItemStack): List<String> {
        if (!showEnchantments || !itemStack.isItemEnchanted) return emptyList()

        val enchantments = mutableListOf<String>()
        val enchantmentList = itemStack.enchantmentTagList

        for (i in 0 until enchantmentList.tagCount()) {
            val tag = enchantmentList.getCompoundTagAt(i)
            val enchantment = Enchantment.getEnchantmentById(tag.getShort("id").toInt())
            val level = tag.getShort("lvl").toInt()

            if (enchantment != null) {
                val abbreviation = when (enchantment.getName()) {
                    "enchantment.protect.all" -> "P"  // 保护
                    "enchantment.damage.all" -> "S"   // 锋利
                    "enchantment.fire.aspect" -> "F"   // 火焰附加
                    "enchantment.knockback" -> "K"     // 击退
                    "enchantment.thorns" -> "T"        // 荆棘
                    "enchantment.arrowDamage" -> "P"   // 力量
                    "enchantment.arrowFire" -> "F"     // 火矢
                    "enchantment.arrowInfinite" -> "Inf" // 无限
                    "enchantment.arrowKnockback" -> "P" // 冲击
                    else -> null
                }
                abbreviation?.let {
                    enchantments.add("$it$level")
                }
            }
        }

        return enchantments
    }

    // 绘制物品堆（优化显示）
    private fun drawItemStack(itemStack: ItemStack, x: Float, y: Float, alpha: Int) {
        glPushMatrix()
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        // 设置物品渲染环境
        RenderHelper.enableGUIStandardItemLighting()
        GlStateManager.enableRescaleNormal()
        GlStateManager.enableDepth()
        GlStateManager.disableLighting()

        // 渲染物品
        mc.renderItem.renderItemAndEffectIntoGUI(itemStack, x.toInt(), y.toInt())
        mc.renderItem.renderItemOverlays(mc.fontRendererObj, itemStack, x.toInt(), y.toInt())

        // 清理渲染状态
        GlStateManager.enableLighting()
        GlStateManager.disableDepth()
        RenderHelper.disableStandardItemLighting()
        GlStateManager.disableRescaleNormal()

        // 绘制附魔信息（在物品右侧垂直排列）
        if (showEnchantments) {
            val enchantTexts = getEnchantments(itemStack)
            if (enchantTexts.isNotEmpty()) {
                glPushMatrix()
                glTranslatef(0f, 0f, 200f) // 确保文字显示在物品上方
                var offsetY = 0f
                for (text in enchantTexts) {
                    Fonts.fontRegular30.drawString(
                        text,
                        x + 6,  // 物品右侧
                        y + offsetY,
                        Color.WHITE.withAlpha(alpha).rgb,
                        textShadow
                    )
                    offsetY += 8f // 每行间隔8像素
                }
                glPopMatrix()
            }
        }

        glDisable(GL_BLEND)
        glPopMatrix()
    }

    // 绘制装备
    private fun drawEquipment(target: EntityLivingBase, x: Float, y: Float, alpha: Int) {
        if (target !is EntityPlayer) return

        glPushMatrix()
        glScalef(equipmentScale, equipmentScale, equipmentScale)

        val scaledX = x / equipmentScale
        val scaledY = y / equipmentScale
        val itemSpacing = if (compactMode) 2f else 4f

        // 只渲染可见的非空装备
        val itemsToRender = mutableListOf<ItemStack>()

        // 添加防具(头盔到靴子)
        for (i in 0..3) {
            target.inventory.armorItemInSlot(3 - i)?.let {
                itemsToRender.add(it)
            }
        }

        // 添加主手物品
        target.heldItem?.let {
            itemsToRender.add(it)
        }

        // 计算总宽度并居中显示
        val totalWidth = itemsToRender.size * 16 + (itemsToRender.size - 1) * itemSpacing
        var currentX = scaledX + (if (compactMode) 0f else (80f / equipmentScale - totalWidth) / 2)

        // 渲染物品
        for (item in itemsToRender) {
            drawItemStack(item, currentX, scaledY, alpha)
            currentX += 16 + itemSpacing
        }

        glPopMatrix()
    }

    override fun drawElement(): Border {
        val smoothMode = animation == "Smooth"
        val fadeMode = animation == "Fade"
        val isMode2 = healthBarDisplayMode == "Mode2"

        val killAuraTarget = KillAura.target.takeIf { it is EntityPlayer }
        val shouldRender = KillAura.handleEvents() && killAuraTarget != null || mc.currentScreen is GuiChat
        val target = killAuraTarget ?: if (delayCounter >= vanishDelay && !isRendered) {
            mc.thePlayer
        } else {
            lastTarget ?: mc.thePlayer
        }


        // 计算基础宽度
        val name = target.name ?: "Unknown"
        val displayName = if (compactMode && name.length > 12) "${name.substring(0, 10)}.." else name
        val nameWidth = titleFont.getStringWidth(displayName)

        // 动态计算所需宽度
        var requiredWidth = 40f + nameWidth // 基础宽度(头像+名称)

        // 考虑精确生命值显示的宽度
        if (showExactHealth) {
            val sampleHealth = "100.0/100.0" // 最大预计生命值文本
            requiredWidth += healthFont.getStringWidth(sampleHealth) + 4f
        }

        // 考虑装备显示的宽度
        if (showEquipment && target is EntityPlayer) {
            requiredWidth = max(requiredWidth, if (compactMode) 120f else 140f)
        }

        // Mode2模式下增加宽度以适应窄长血条
        if (isMode2) {
            requiredWidth = max(requiredWidth, if (compactMode) 140f else 160f)
        }

        // 应用最小宽度限制
        val finalWidth = max(requiredWidth, if (compactMode) 100f else 120f)

        // 计算基础高度 - Mode2模式下高度更小
        var baseHeight = if (isMode2) {
            if (compactMode) 28f else 32f
        } else {
            if (compactMode) 32f else 36f
        }
        if (showEquipment && target is EntityPlayer && !isMode2) {
            baseHeight += if (compactMode) 12f else 16f
        }

        assumeNonVolatile {
            if (shouldRender) {
                delayCounter = 0
            } else if (isRendered || isAlpha) {
                delayCounter++
            }
            if (shouldRender || isRendered || isAlpha) {
                val targetHealth = getHealth(target!!, healthFromScoreboard, absorption)
                val maxHealth = target.maxHealth + if (absorption) target.absorptionAmount else 0F

                easingHealth += (targetHealth - easingHealth) / 2f.pow(10f - fadeSpeed) * deltaTime
                easingHealth = easingHealth.coerceIn(0f, maxHealth)
                val targetHurtTime = if (target.isEntityAlive) target.hurtTime.toFloat() else 0F
                easingHurtTime = (easingHurtTime..targetHurtTime).lerpWith(RenderUtils.deltaTimeNormalized())

                if (target != lastTarget || abs(easingHealth - targetHealth) < 0.01) {
                    easingHealth = targetHealth
                }

                if (smoothMode) {
                    val targetWidth = if (shouldRender) finalWidth else if (delayCounter >= vanishDelay) 0f else width
                    width = AnimationUtil.base(width.toDouble(), targetWidth.toDouble(), animationSpeed.toDouble())
                        .toFloat().coerceAtLeast(0f)

                    val targetHeight = if (shouldRender) baseHeight else if (delayCounter >= vanishDelay) 0f else height
                    height = AnimationUtil.base(height.toDouble(), targetHeight.toDouble(), animationSpeed.toDouble())
                        .toFloat().coerceAtLeast(0f)
                } else {
                    width = finalWidth
                    height = baseHeight

                    val targetText = if (shouldRender) textColor.alpha else if (delayCounter >= vanishDelay) 0f else alphaText
                    alphaText = AnimationUtil.base(alphaText.toDouble(), targetText.toDouble(), animationSpeed.toDouble()).toInt()

                    val targetBackground = if (shouldRender) backgroundColor.alpha else if (delayCounter >= vanishDelay) 0f else alphaBackground
                    alphaBackground = AnimationUtil.base(alphaBackground.toDouble(), targetBackground.toDouble(), animationSpeed.toDouble()).toInt()

                    val targetBorder = if (shouldRender) borderColor.alpha else if (delayCounter >= vanishDelay) 0f else alphaBorder
                    alphaBorder = AnimationUtil.base(alphaBorder.toDouble(), targetBorder.toDouble(), animationSpeed.toDouble()).toInt()
                }

                val backgroundCustomColor = backgroundColor.withAlpha(if (fadeMode) alphaBackground else backgroundColor.alpha).rgb
                val borderCustomColor = borderColor.withAlpha(if (fadeMode) alphaBorder else borderColor.alpha).rgb
                val textCustomColor = textColor.withAlpha(if (fadeMode) alphaText else textColor.alpha).rgb

                val rainbowOffset = System.currentTimeMillis() % 10000 / 10000F
                val rainbowX = 1f safeDiv rainbowX
                val rainbowY = 1f safeDiv rainbowY

                glPushMatrix()
                glEnable(GL_BLEND)
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

                if (fadeMode && isAlpha || smoothMode && isRendered || delayCounter < vanishDelay) {
                    val renderWidth = width.coerceAtLeast(0F)
                    val renderHeight = height.coerceAtLeast(0F)
                    RainbowShader.begin(backgroundMode == "Rainbow", rainbowX, rainbowY, rainbowOffset).use {
                        drawRoundedBorderRect(
                            0F, 0F, renderWidth, renderHeight, borderStrength,
                            if (backgroundMode == "Rainbow") 0 else backgroundCustomColor,
                            borderCustomColor, roundedRectRadius
                        )
                    }
                    if (shadowcheck) showShadow(0F,0F,width,height)
                    if (isMode2) {
                        // Mode2: 窄长血条在底部
                        val healthBarTop = renderHeight - (if (compactMode) 8F else 10F)
                        val healthBarHeight = if (compactMode) 4F else 6F
                        val healthBarStart = 4F
                        val healthBarTotal = renderWidth - 8F
                        val currentWidth = (easingHealth / maxHealth).coerceIn(0F, 1F) * healthBarTotal

                        // 背景条
                        if (roundHealthBarShape) {
                            drawRoundedRect(
                                healthBarStart, healthBarTop,
                                healthBarStart + healthBarTotal, healthBarTop + healthBarHeight,
                                Color.BLACK.rgb, 3F
                            )
                        }

                        // 主条
                        withClipping(main = {
                            if (roundHealthBarShape) {
                                drawRoundedRect(
                                    healthBarStart, healthBarTop,
                                    healthBarStart + currentWidth, healthBarTop + healthBarHeight,
                                    0, 3F
                                )
                            }
                        }, toClip = {
                            drawGradientRect(
                                healthBarStart.toInt(), healthBarTop.toInt(),
                                healthBarStart.toInt() + currentWidth.toInt(), healthBarTop.toInt() + healthBarHeight.toInt(),
                                healthBarColor1.rgb, healthBarColor2.rgb, 0f
                            )
                        })

                        // 百分比显示在血条上方
                        val healthPercentage = (easingHealth / maxHealth * 100).toInt()
                        val percentageText = "$healthPercentage%"
                        val textWidth = healthFont.getStringWidth(percentageText)
                        val textX = healthBarStart + (healthBarTotal - textWidth) / 2
                        val textY = healthBarTop - healthFont.FONT_HEIGHT - 2
                        healthFont.drawString(percentageText, textX, textY, textCustomColor, textShadow)

                    } else {
                        // Mode1: 原始血条位置
                        val healthBarTop = if (compactMode) 22F else 24F
                        val healthBarHeight = if (compactMode) 6F else 8F
                        val healthBarStart = 36F
                        val healthBarTotal = (renderWidth - 39F).coerceAtLeast(0F)
                        val currentWidth = (easingHealth / maxHealth).coerceIn(0F, 1F) * healthBarTotal

                        // 背景条
                        if (roundHealthBarShape) {
                            drawRoundedRect(
                                healthBarStart, healthBarTop,
                                healthBarStart + healthBarTotal, healthBarTop + healthBarHeight,
                                Color.BLACK.rgb, 6F
                            )
                        }

                        // 主条
                        withClipping(main = {
                            if (roundHealthBarShape) {
                                drawRoundedRect(
                                    healthBarStart, healthBarTop,
                                    healthBarStart + currentWidth, healthBarTop + healthBarHeight,
                                    0, 6F
                                )
                            }
                        }, toClip = {
                            drawGradientRect(
                                healthBarStart.toInt(), healthBarTop.toInt(),
                                healthBarStart.toInt() + currentWidth.toInt(), healthBarTop.toInt() + healthBarHeight.toInt(),
                                healthBarColor1.rgb, healthBarColor2.rgb, 0f
                            )
                        })

                        val healthPercentage = (easingHealth / maxHealth * 100).toInt()
                        val percentageText = "$healthPercentage%"
                        val textWidth = healthFont.getStringWidth(percentageText)
                        val calcX = healthBarStart + currentWidth - textWidth
                        val textX = max(healthBarStart, calcX)
                        val textY = healthBarTop - healthFont.FONT_HEIGHT / 2 - (if (compactMode) 1F else 2F)
                        healthFont.drawString(percentageText, textX, textY, textCustomColor, textShadow)
                    }

                    val shouldRenderBody = (fadeMode && alphaText + alphaBackground + alphaBorder > 100) || (smoothMode && width + height > 100)

                    if (shouldRenderBody) {
                        val renderer = mc.renderManager.getEntityRenderObject<Entity>(target)

                        if (renderer != null) {
                            val entityTexture = renderer.getEntityTexture(target)

                            glPushMatrix()
                            val scale = 1 - easingHurtTime / 10f
                            val f1 = (0.7F..1F).lerpWith(scale) * this.scale
                            val color = ColorUtils.interpolateColor(Color.RED, Color.WHITE, scale)
                            val centerX1 = (4..32).lerpWith(0.5F)
                            val midY = (4f..28f).lerpWith(0.5F)

                            glTranslatef(centerX1, midY, 0f)
                            glScalef(f1, f1, f1)
                            glTranslatef(-centerX1, -midY, 0f)

                            if (entityTexture != null) {
                                withClipping(main = {
                                    drawRoundedRect(4f, 4f, 32f, 32f, 0, roundedRectRadius)
                                }, toClip = {
                                    drawHead(
                                        entityTexture, 4, 4, 8f, 8f, 8, 8, 28, 28, 64F, 64F, color
                                    )
                                })
                            }
                            glPopMatrix()
                        }

                        // 绘制名称和生命值
                        val nameX = if (isMode2) 36F else 36F
                        val nameY = if (isMode2) 6F else (if (compactMode) 5F else 6F)

                        // 绘制名称
                        titleFont.drawString(displayName, nameX, nameY, textCustomColor, textShadow)

                        // 绘制精确生命值
                        if (showExactHealth) {
                            val healthText = "%.1f/%.1f".format(easingHealth, maxHealth)
                            val healthX = nameX + nameWidth + 6f
                            healthFont.drawString(healthText, healthX, nameY + (if (compactMode) 1F else 2F), textCustomColor, textShadow)
                        }

                        // Mode2下不显示装备
                        if (showEquipment && target is EntityPlayer && height > 36f && !isMode2) {
                            val equipmentY = if (compactMode) 30f else 34f
                            drawEquipment(target, nameX, equipmentY, if (fadeMode) alphaText else textColor.alpha)
                        }
                    }
                }

                glPopMatrix()
            }
        }

        lastTarget = target
        return Border(0F, 0F, width, height)

    }

}