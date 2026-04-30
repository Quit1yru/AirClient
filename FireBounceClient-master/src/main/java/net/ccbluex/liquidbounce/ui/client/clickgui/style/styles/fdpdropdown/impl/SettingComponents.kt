/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.impl

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.features.module.Module

import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.BlackStyle.chosenText
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.BlackStyle.sliderValueHeld
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.animations.Animation
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.animations.Direction
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.animations.impl.DecelerateAnimation
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.animations.impl.EaseInOutQuad
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.normal.Main
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.render.DrRenderUtils
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extensions.round
import net.ccbluex.liquidbounce.utils.extensions.roundX
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawCustomShapeWithRadius
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.ui.EditableText
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.stream.Collectors
import kotlin.math.*

class SettingComponents(private val module: Module) : Component() {
    var settingHeightScissor: Animation? = null
    private val keySettingAnimMap = HashMap<Module, Array<Animation>>()
    private val sliderintMap = HashMap<IntValue, Float>()
    private val sliderintAnimMap = HashMap<IntValue, Array<Animation>>()
    private val sliderfloatMap = HashMap<FloatValue, Float>()
    private val sliderfloatAnimMap = HashMap<FloatValue, Array<Animation>>()
    private val toggleAnimation = HashMap<BoolValue, Array<Animation>>()
    private val modeSettingAnimMap = HashMap<ListValue, Array<Animation>>()
    private val modeSettingClick = HashMap<ListValue, Boolean>()
    private val modesHoverAnimation = HashMap<ListValue, HashMap<String, Animation>>()
    private val multiModeSettingAnimMap = HashMap<MultiChoiceValue, Array<Animation>>()
    private val multiModeSettingClick = HashMap<MultiChoiceValue, Boolean>()
    private val multiModesHoverAnimation = HashMap<MultiChoiceValue, HashMap<String, Animation>>()
    private val sliderFloatRangeMap = HashMap<FloatRangeValue, Float>()
    private val sliderFloatRangeAnimMap = HashMap<FloatRangeValue, Array<Animation>>()
    private val sliderIntegerRangeMap = HashMap<IntRangeValue, Float>()
    private val sliderIntegerRangeAnimMap = HashMap<IntRangeValue, Array<Animation>>()
    private val fontValueMap = HashMap<FontValue, Float>()
    private val fontValueAnimMap = HashMap<FontValue, Array<Animation>>()
    private var binding: Module? = null
    private var draggingNumber: Value<*>? = null
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 0f
    var rectHeight: Float = 0f
    var panelLimitY: Float = 0f
    var alphaAnimation: Int = 0
    var settingSize: Double = 0.0

    private val colorSettingAnimMap = HashMap<ColorValue, Array<Animation>>()
    private val colorPickerAnimationMap = HashMap<ColorValue, Animation>()

    init {
        keySettingAnimMap[module] = arrayOf(
            EaseInOutQuad(250, 1.0, Direction.BACKWARDS),
            DecelerateAnimation(225, 1.0, Direction.BACKWARDS)
        )

        for (setting in module.values) {
            if (setting is FloatValue) {
                sliderfloatMap[setting] = 0f
                sliderfloatAnimMap[setting] = arrayOf(
                    DecelerateAnimation(250, 1.0, Direction.BACKWARDS),
                    DecelerateAnimation(200, 1.0, Direction.BACKWARDS)
                )
            }
            if (setting is IntValue) {
                sliderintMap[setting] = 0f
                sliderintAnimMap[setting] = arrayOf(
                    DecelerateAnimation(250, 1.0, Direction.BACKWARDS),
                    DecelerateAnimation(200, 1.0, Direction.BACKWARDS)
                )
            }
            if (setting is FloatRangeValue) {
                sliderFloatRangeMap[setting] = 0f
                sliderFloatRangeAnimMap[setting] = arrayOf(
                    DecelerateAnimation(250, 1.0, Direction.BACKWARDS),
                    DecelerateAnimation(200, 1.0, Direction.BACKWARDS)
                )
            }
            if (setting is IntRangeValue) {
                sliderIntegerRangeMap[setting] = 0f
                sliderIntegerRangeAnimMap[setting] = arrayOf(
                    DecelerateAnimation(250, 1.0, Direction.BACKWARDS),
                    DecelerateAnimation(200, 1.0, Direction.BACKWARDS)
                )
            }
            if (setting is BoolValue) {
                toggleAnimation[setting] = arrayOf(
                    DecelerateAnimation(250, 1.0, Direction.BACKWARDS),
                    DecelerateAnimation(200, 1.0, Direction.BACKWARDS)
                )
            }
            if (setting is ListValue) {
                modeSettingClick[setting] = false
                modeSettingAnimMap[setting] = arrayOf(
                    DecelerateAnimation(250, 1.0, Direction.BACKWARDS),
                    EaseInOutQuad(250, 1.0, Direction.BACKWARDS)
                )

                val modeMap = HashMap<String, Animation>()
                for (mode in setting.values) {
                    modeMap[mode] = DecelerateAnimation(250, 1.0, Direction.BACKWARDS)
                }
                modesHoverAnimation[setting] = modeMap
            }
            if (setting is MultiChoiceValue) {
                multiModeSettingClick[setting] = false
                multiModeSettingAnimMap[setting] = arrayOf(
                    DecelerateAnimation(250, 1.0, Direction.BACKWARDS),
                    EaseInOutQuad(250, 1.0, Direction.BACKWARDS)
                )

                val modeMap = HashMap<String, Animation>()
                for (mode in setting.choices) {
                    modeMap[mode] = DecelerateAnimation(250, 1.0, Direction.BACKWARDS)
                }
                multiModesHoverAnimation[setting] = modeMap
            }
            if (setting is ColorValue) {
                colorSettingAnimMap[setting] = arrayOf(
                    DecelerateAnimation(250, 1.0, Direction.BACKWARDS),
                    DecelerateAnimation(200, 1.0, Direction.BACKWARDS)
                )
                colorPickerAnimationMap[setting] = DecelerateAnimation(300, 1.0, Direction.FORWARDS)

                setting.showPicker = false
            }
            if (setting is FontValue) {
                fontValueMap[setting] = 0f
                fontValueAnimMap[setting] = arrayOf(
                    DecelerateAnimation(250, 1.0, Direction.BACKWARDS),
                    DecelerateAnimation(200, 1.0, Direction.BACKWARDS)
                )
            }
        }
    }

    override fun initGui() {
        chosenText = null
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (binding != null) {
            if (keyCode == Keyboard.KEY_SPACE || keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE) {
                binding!!.keyBind = Keyboard.KEY_NONE
            } else {
                binding!!.keyBind = keyCode
            }
            binding = null
            return
        }

        if (keyCode == Keyboard.KEY_RETURN) {
            chosenText = null
            return
        }

        if (chosenText != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                chosenText = null
                return
            }
            when (keyCode) {
                Keyboard.KEY_LEFT -> {
                    chosenText!!.cursorIndex = max(chosenText!!.cursorIndex - 1, 0)
                }
                Keyboard.KEY_RIGHT -> {
                    chosenText!!.cursorIndex = min(chosenText!!.cursorIndex + 1, chosenText!!.string.length)
                }
                Keyboard.KEY_BACK -> {
                    if (chosenText!!.string.isNotEmpty()) {
                        chosenText!!.cursorIndex = chosenText!!.cursorIndex.coerceIn(0, chosenText!!.string.length)
                        if (chosenText!!.cursorIndex > 0) {
                            val removalStart = chosenText!!.cursorIndex - 1
                            val removalEnd = chosenText!!.cursorIndex
                            if (removalStart < removalEnd && removalEnd <= chosenText!!.string.length) {
                                chosenText!!.string = chosenText!!.string.removeRange(removalStart, removalEnd)
                                chosenText!!.cursorIndex = removalStart
                            }
                        }
                    }
                }
                Keyboard.KEY_DELETE -> {
                    if (chosenText!!.string.isNotEmpty()) {
                        chosenText!!.cursorIndex = chosenText!!.cursorIndex.coerceIn(0, chosenText!!.string.length - 1)
                        if (chosenText!!.cursorIndex < chosenText!!.string.length) {
                            val removalStart = chosenText!!.cursorIndex
                            val removalEnd = chosenText!!.cursorIndex + 1
                            if (removalStart < removalEnd && removalEnd <= chosenText!!.string.length) {
                                chosenText!!.string = chosenText!!.string.removeRange(removalStart, removalEnd)
                            }
                        }
                    }
                }
                else -> {
                    if (!typedChar.isISOControl()) {
                        chosenText!!.cursorIndex = chosenText!!.cursorIndex.coerceIn(0, chosenText!!.string.length)
                        val insertionIndex = chosenText!!.cursorIndex
                        chosenText!!.string =
                            chosenText!!.string.substring(0, insertionIndex) +
                                    typedChar +
                                    chosenText!!.string.substring(insertionIndex)
                        chosenText!!.cursorIndex = insertionIndex + 1
                    }
                }
            }

            val value = chosenText!!.value
            if (value is TextValue || value is ColorValue) {
                when (value) {
                    is TextValue -> value.set(chosenText!!.string, true)
                    is ColorValue -> {
                        try {
                            val numericString = chosenText!!.string.filter { it.isDigit() }
                            if (numericString.isNotEmpty()) {
                                val intValue = numericString.toInt().coerceIn(0, 255)
                                chosenText!!.string = intValue.toString()
                                when (value.rgbaIndex) {
                                    0 -> value.set(Color(intValue, value.get().green, value.get().blue, value.get().alpha), true)
                                    1 -> value.set(Color(value.get().red, value.get().green, intValue, value.get().alpha), true)
                                    2 -> value.set(Color(value.get().red, value.get().green, value.get().blue, intValue), true)
                                }
                            } else {
                                chosenText!!.string = "0"
                            }
                        } catch (_: NumberFormatException) {
                            chosenText!!.string = "0"
                        }
                    }
                    else -> { }
                }
            }

            return
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int) {
        val baseTextColor = Color(255, 255, 255, alphaAnimation) 
        val darkRectHover = Color(20, 20, 20, alphaAnimation)

        val accent = ClickGUI.fdpcm.equals("Color", ignoreCase = true) // 使用ClickGUI中的colormode设置
        val index = 0
        val generatedColor = ClickGUI.generateColor(index) // 使用ClickGUI中的generateColor方法
        val accentedColor2 = DrRenderUtils.applyOpacity(generatedColor, alphaAnimation / 255f) // Returns Color

        var count = 0.0

        for (setting in module.values.stream().filter { obj: Value<*> -> obj.shouldRender() }
            .collect(Collectors.toList())) {
            val settingY = roundToHalf(y + (count * rectHeight)).toFloat()

            // ----- FloatValue -----
            if (setting is FloatValue) {
                val valueText = round(setting.value.toDouble(), 0.01).toFloat().toString()
                val regularFontWidth = Fonts.InterMedium_18.stringWidth(setting.name + ": ").toFloat()
                val valueFontWidth = Fonts.InterMedium_18.stringWidth(valueText).toFloat()

                val titleX = x + width / 2f - (regularFontWidth + valueFontWidth) / 2f
                val titleY = (settingY + (rectHeight - Fonts.InterMedium_18.height) / 2f)

                DrRenderUtils.resetColor()
                Fonts.InterMedium_18.drawString(setting.name + ": ", titleX, titleY, baseTextColor.rgb, false)
                Fonts.InterBold_18.drawString(valueText, titleX + regularFontWidth, titleY, baseTextColor.rgb, false)

                val hoverAnimation = sliderfloatAnimMap[setting]!![0]
                val totalSliderWidth = width - 10
                val hoveringSlider = isClickable(settingY + 17f)
                        && DrRenderUtils.isHovering(x + 5, settingY + 17, totalSliderWidth, 6f, mouseX, mouseY)

                hoverAnimation.direction = if (hoveringSlider || draggingNumber === setting) Direction.FORWARDS else Direction.BACKWARDS

                if (draggingNumber === setting && Mouse.isButtonDown(0)) {
                    val percent = min(1.0, max(0.0, ((mouseX - (x + 5)) / totalSliderWidth).toDouble())).toFloat()
                    val newValue = ((percent * (setting.maximum - setting.minimum)) + setting.minimum).toDouble()
                    setting.set(newValue)
                }

                val currentValue = setting.value.toDouble()
                val sliderMath = ((currentValue - setting.minimum) / (setting.maximum - setting.minimum)).toFloat()

                val oldSlider = sliderfloatMap[setting]!!
                val targetSlider = totalSliderWidth * sliderMath
                sliderfloatMap[setting] = DrRenderUtils.animate(targetSlider.toDouble(), oldSlider.toDouble(), .1).toFloat()

                val sliderY = (settingY + 18)
                drawCustomShapeWithRadius(x + 5, sliderY, totalSliderWidth, 3f, 1.5f,
                    DrRenderUtils.applyOpacity(Color(60, 60, 60, alphaAnimation), (.6f + (.2 * hoverAnimation.output)).toFloat())) // 更亮的背景
                drawCustomShapeWithRadius(x + 5, sliderY, max(4.0, sliderfloatMap[setting]!!.toDouble()).toFloat(), 3f, 1.5f,
                    if (accent) accentedColor2 else baseTextColor) // Both Color

                DrRenderUtils.setAlphaLimit(0f)
                DrRenderUtils.fakeCircleGlow((x + 4 + max(4.0, sliderfloatMap[setting]!!.toDouble())).toFloat(), sliderY + 1.5f, 6f, Color.BLACK, .3f)
                DrRenderUtils.drawGoodCircle((x + 4 + max(4.0, sliderfloatMap[setting]!!.toDouble())), (sliderY + 1.5f).toDouble(), 3.75f,
                    if (accent) accentedColor2.rgb else baseTextColor.rgb)

                count += 2.0 
            }

            // ----- FloatRangeValue -----
            else if (setting is FloatRangeValue) {
                val slider1 = setting.get().start
                val slider2 = setting.get().endInclusive
                val text = "${setting.name}: ${round(slider1)} - ${round(slider2)}"
                val regularFontWidth = Fonts.InterMedium_18.stringWidth(text).toFloat()
                val titleX = x + width / 2f - regularFontWidth / 2f
                val titleY = settingY + (rectHeight - Fonts.InterMedium_18.height) / 2f

                Fonts.InterMedium_18.drawString(text, titleX, titleY, baseTextColor.rgb)

                val totalSliderWidth = width - 10
                val sliderPosY = settingY + 18
                val colorDraw = if (accent) accentedColor2 else baseTextColor 

                if (draggingNumber == setting && Mouse.isButtonDown(0)) {
                    val mousePercent = min(1.0, max(0.0, ((mouseX - (x + 5)) / totalSliderWidth).toDouble())).toFloat()
                    val newVal = (setting.minimum + (setting.maximum - setting.minimum) * mousePercent)
                    val distStart = abs(newVal - slider1)
                    val distEnd = abs(newVal - slider2)
                    if (distStart <= distEnd) {
                        setting.setFirst(newVal.coerceIn(setting.minimum, slider2), false)
                    } else {
                        setting.setLast(newVal.coerceIn(slider1, setting.maximum), false)
                    }
                }

                val rangeMin = setting.minimum
                val rangeMax = setting.maximum
                val pixelPos1 = totalSliderWidth * ((setting.get().start - rangeMin) / (rangeMax - rangeMin))
                val pixelPos2 = totalSliderWidth * ((setting.get().endInclusive - rangeMin) / (rangeMax - rangeMin))

                drawCustomShapeWithRadius(x + 5, sliderPosY, totalSliderWidth, 3f, 1.5f, DrRenderUtils.applyOpacity(Color(60, 60, 60, alphaAnimation), (.6f)))
                drawCustomShapeWithRadius(x + 5 + min(pixelPos1, pixelPos2), sliderPosY, abs(pixelPos2 - pixelPos1), 3f, 1.5f, colorDraw)

                fun drawSliderCircle(px: Float) {
                    DrRenderUtils.fakeCircleGlow(x + 4 + px, sliderPosY + 1.5f, 6f, Color.BLACK, .3f)
                    DrRenderUtils.drawGoodCircle((x + 4 + px).toDouble(), sliderPosY + 1.5, 3.75f, colorDraw.rgb)
                }
                drawSliderCircle(pixelPos1)
                drawSliderCircle(pixelPos2)

                count += 2.0 
            }

            // ----- IntValue -----
            else if (setting is IntValue) {
                val valueText = roundX(setting.value.toDouble(), 1.0).toFloat().toString()
                val regularFontWidth = Fonts.InterMedium_18.stringWidth(setting.name + ": ").toFloat()
                val valueFontWidth = Fonts.InterMedium_18.stringWidth(valueText).toFloat()
                val titleX = x + width / 2f - (regularFontWidth + valueFontWidth) / 2f
                val titleY = (settingY + (rectHeight - Fonts.InterMedium_18.height) / 2f)

                DrRenderUtils.resetColor()
                Fonts.InterMedium_18.drawString(setting.name + ": ", titleX, titleY, baseTextColor.rgb, false)
                Fonts.InterBold_18.drawString(valueText, titleX + regularFontWidth, titleY, baseTextColor.rgb, false)

                val hoverAnimation = sliderintAnimMap[setting]!![0]
                val totalSliderWidth = width - 10
                val hoveringSlider = isClickable(settingY + 17f) && DrRenderUtils.isHovering(x + 5, settingY + 17, totalSliderWidth, 6f, mouseX, mouseY)

                hoverAnimation.direction = if (hoveringSlider || draggingNumber === setting) Direction.FORWARDS else Direction.BACKWARDS

                if (draggingNumber === setting && Mouse.isButtonDown(0)) {
                    val percent = min(1.0, max(0.0, ((mouseX - (x + 5)) / totalSliderWidth).toDouble())).toFloat()
                    val newValue = ((percent * (setting.maximum - setting.minimum)) + setting.minimum).roundToInt()
                    setting.set(newValue)
                }

                val sliderMath = ((setting.value.toDouble() - setting.minimum) / (setting.maximum - setting.minimum)).toFloat()
                val oldSlider = sliderintMap[setting]!!
                val targetSlider = totalSliderWidth * sliderMath
                sliderintMap[setting] = DrRenderUtils.animate(targetSlider.toDouble(), oldSlider.toDouble(), .1).toFloat()

                val sliderY = (settingY + 18)
                drawCustomShapeWithRadius(x + 5, sliderY, totalSliderWidth, 3f, 1.5f, DrRenderUtils.applyOpacity(Color(60, 60, 60, alphaAnimation), (.6f + (.2 * hoverAnimation.output)).toFloat()))
                drawCustomShapeWithRadius(x + 5, sliderY, max(4.0, sliderintMap[setting]!!.toDouble()).toFloat(), 3f, 1.5f, if (accent) accentedColor2 else baseTextColor)

                DrRenderUtils.setAlphaLimit(0f)
                DrRenderUtils.fakeCircleGlow((x + 4 + max(4.0, sliderintMap[setting]!!.toDouble())).toFloat(), sliderY + 1.5f, 6f, Color.BLACK, .3f)
                DrRenderUtils.drawGoodCircle((x + 4 + max(4.0, sliderintMap[setting]!!.toDouble())), (sliderY + 1.5f).toDouble(), 3.75f, if (accent) accentedColor2.rgb else baseTextColor.rgb)

                count += 2.0 
            }

            // ----- IntegerRangeValue -----
            else if (setting is IntRangeValue) {
                val slider1 = setting.get().first
                val slider2 = setting.get().last
                val text = "${setting.name}: $slider1 - $slider2"
                val regularFontWidth = Fonts.InterMedium_18.stringWidth(text).toFloat()
                val colorDraw = if (accent) accentedColor2 else baseTextColor 
                val titleX = x + width / 2f - regularFontWidth / 2f
                val titleY = settingY + (rectHeight - Fonts.InterMedium_18.height) / 2f

                Fonts.InterMedium_18.drawString(text, titleX, titleY, baseTextColor.rgb)

                val totalSliderWidth = width - 10
                val sliderPosY = settingY + 18

                if (draggingNumber == setting && Mouse.isButtonDown(0)) {
                    val mousePercent = min(1.0, max(0.0, ((mouseX - (x + 5)) / totalSliderWidth).toDouble())).toFloat()
                    val newVal = (setting.minimum + (setting.maximum - setting.minimum) * mousePercent).toInt()
                    val distStart = abs(newVal - slider1)
                    val distEnd = abs(newVal - slider2)
                    if (distStart <= distEnd) {
                        setting.setFirst(newVal.coerceIn(setting.minimum, slider2), false)
                    } else {
                        setting.setLast(newVal.coerceIn(slider1, setting.maximum), false)
                    }
                }

                val rangeMin = setting.minimum.toFloat()
                val rangeMax = setting.maximum.toFloat()
                val pixelPos1 = totalSliderWidth * ((setting.get().first.toFloat() - rangeMin) / (rangeMax - rangeMin))
                val pixelPos2 = totalSliderWidth * ((setting.get().last.toFloat() - rangeMin) / (rangeMax - rangeMin))

                drawCustomShapeWithRadius(x + 5, sliderPosY, totalSliderWidth, 3f, 1.5f, DrRenderUtils.applyOpacity(Color(60, 60, 60, alphaAnimation), (.6f)))
                drawCustomShapeWithRadius(x + 5 + min(pixelPos1, pixelPos2), sliderPosY, abs(pixelPos2 - pixelPos1), 3f, 1.5f, colorDraw)

                fun drawSliderCircle(px: Float) {
                    DrRenderUtils.fakeCircleGlow(x + 4 + px, sliderPosY + 1.5f, 6f, Color.BLACK, .3f)
                    DrRenderUtils.drawGoodCircle((x + 4 + px).toDouble(), sliderPosY + 1.5, 3.75f, colorDraw.rgb)
                }
                drawSliderCircle(pixelPos1)
                drawSliderCircle(pixelPos2)

                count += 2.0 
            }

            // ----- BoolValue -----
            else if (setting is BoolValue) {
                val toggleAnim = toggleAnimation[setting]!![0]
                val hoverAnim = toggleAnimation[setting]!![1]

                DrRenderUtils.resetColor()
                GlStateManager.enableBlend()

                Fonts.InterMedium_18.drawString(setting.name, roundToHalf((x + 4).toDouble()).toInt().toFloat(), settingY + 5, baseTextColor.rgb, false)

                val switchWidth = 16f
                val hoveringSwitch = isClickable(settingY + 5f)
                        && DrRenderUtils.isHovering(x + width - (switchWidth + 6), settingY + 5, switchWidth, 8f, mouseX, mouseY)

                hoverAnim.direction = if (hoveringSwitch) Direction.FORWARDS else Direction.BACKWARDS
                toggleAnim.direction = if (setting.get()) Direction.FORWARDS else Direction.BACKWARDS
                DrRenderUtils.resetColor()

                val accentCircle = if (accent) DrRenderUtils.applyOpacity(generatedColor, .8f) else DrRenderUtils.darker(baseTextColor, .8f)

                drawCustomShapeWithRadius(x + width - (switchWidth + 5.5f), settingY + 7, switchWidth, 4.5f, 2f,
                    DrRenderUtils.interpolateColorC(DrRenderUtils.applyOpacity(darkRectHover, .5f), accentCircle, toggleAnim.output.toFloat()))

                DrRenderUtils.fakeCircleGlow(((x + width - (switchWidth + 3)) + ((switchWidth - 5) * toggleAnim.output)).toFloat(), settingY + 9, 6f, Color.BLACK, .3f)

                DrRenderUtils.resetColor()
                drawCustomShapeWithRadius((x + width - (switchWidth + 6) + ((switchWidth - 5) * toggleAnim.output)).toFloat(), settingY + 6, 6.5f, 6.5f, 3f, baseTextColor)
                
                count += 1.0 
            }

            // ----- ListValue -----
            else if (setting is ListValue) {
                val hoverAnim = modeSettingAnimMap[setting]!![0]
                val openAnim = modeSettingAnimMap[setting]!![1]
                val hoveringModeRect = isClickable(settingY + 5f) && DrRenderUtils.isHovering(x + 5, settingY + 5, width - 10, rectHeight + 7, mouseX, mouseY)

                hoverAnim.direction = if (hoveringModeRect) Direction.FORWARDS else Direction.BACKWARDS
                openAnim.direction = if (modeSettingClick[setting] ?: false) Direction.FORWARDS else Direction.BACKWARDS

                val mathSize = (setting.values.size - 1) * rectHeight
                drawCustomShapeWithRadius(x + 5, (settingY + rectHeight + 2 + (12 * openAnim.output)).toFloat(), width - 10, (mathSize * openAnim.output).toFloat(), 3f, DrRenderUtils.applyOpacity(darkRectHover, (.35f * openAnim.output).toFloat()))

                if (!openAnim.isDone) {
                    GL11.glEnable(GL11.GL_SCISSOR_TEST)
                    DrRenderUtils.scissor((x + 5).toDouble(), (settingY + 7 + rectHeight + (3 * openAnim.output)).toFloat().toDouble(), (width - 10).toDouble(), (mathSize * openAnim.output).toFloat().toDouble())
                }

                var modeCount = 0f
                for (mode in setting.values) {
                    if (mode.equals(setting.get(), ignoreCase = true)) continue
                    val modeY = ((settingY + rectHeight + 11 + ((8 + (modeCount * rectHeight)) * openAnim.output))).toFloat()
                    DrRenderUtils.resetColor()
                    val hoveringMode = isClickable(modeY - 5f) && openAnim.direction == Direction.FORWARDS && DrRenderUtils.isHovering(x + 5, modeY - 5, width - 10, rectHeight, mouseX, mouseY)
                    val modeHover = modesHoverAnimation[setting]!![mode]!!
                    modeHover.direction = if (hoveringMode) Direction.FORWARDS else Direction.BACKWARDS

                    if (modeHover.finished(Direction.FORWARDS) || !modeHover.isDone) {
                        drawCustomShapeWithRadius(x + 5, modeY - 5, width - 10, rectHeight, 3f, DrRenderUtils.applyOpacity(baseTextColor, (.2f * modeHover.output).toFloat()))
                    }
                    if (openAnim.isDone && openAnim.direction == Direction.FORWARDS || !openAnim.isDone) {
                        Fonts.InterMedium_18.drawString(mode, x + 13, modeY, DrRenderUtils.applyOpacity(baseTextColor.rgb, openAnim.output.toFloat()), false)
                    }
                    modeCount++
                }

                if (!openAnim.isDone) GL11.glDisable(GL11.GL_SCISSOR_TEST)
                drawCustomShapeWithRadius(x + 5, settingY + 5, width - 10, rectHeight + 7, 3f, DrRenderUtils.applyOpacity(darkRectHover, .45f))

                if (!hoverAnim.isDone || hoverAnim.finished(Direction.FORWARDS)) {
                    drawCustomShapeWithRadius(x + 5, settingY + 5, width - 10, rectHeight + 7, 3f, DrRenderUtils.applyOpacity(baseTextColor, (.2f * hoverAnim.output).toFloat()))
                }

                Fonts.InterMedium_14.drawString(setting.name, x + 13, settingY + 9, baseTextColor.rgb, false)
                DrRenderUtils.resetColor()
                Fonts.InterBold_18.drawString(setting.get(), x + 13, (settingY + 17.5).toFloat(), baseTextColor.rgb, false)
                DrRenderUtils.resetColor()
                DrRenderUtils.drawClickGuiArrow(x + width - 15, settingY + 17, 5f, openAnim, baseTextColor.rgb)

                count += 1.5 + ((mathSize / rectHeight) * openAnim.output)
            }

            // ----- MultiChoiceValue -----
            else if (setting is MultiChoiceValue) {
                val hoverAnim = multiModeSettingAnimMap[setting]!![0]
                val openAnim = multiModeSettingAnimMap[setting]!![1]
                val hoveringModeRect = isClickable(settingY + 5f) && DrRenderUtils.isHovering(x + 5, settingY + 5, width - 10, rectHeight + 7, mouseX, mouseY)

                hoverAnim.direction = if (hoveringModeRect) Direction.FORWARDS else Direction.BACKWARDS
                openAnim.direction = if (multiModeSettingClick[setting] ?: false) Direction.FORWARDS else Direction.BACKWARDS

                val selectableChoices = setting.getSelectableChoices()
                val mathSize = (selectableChoices.size - 1) * rectHeight
                drawCustomShapeWithRadius(x + 5, (settingY + rectHeight + 2 + (12 * openAnim.output)).toFloat(), width - 10, (mathSize * openAnim.output).toFloat(), 3f, DrRenderUtils.applyOpacity(darkRectHover, (.35f * openAnim.output).toFloat()))

                if (!openAnim.isDone) {
                    GL11.glEnable(GL11.GL_SCISSOR_TEST)
                    DrRenderUtils.scissor((x + 5).toDouble(), (settingY + 7 + rectHeight + (3 * openAnim.output)).toFloat().toDouble(), (width - 10).toDouble(), (mathSize * openAnim.output).toFloat().toDouble())
                }

                var modeCount = 0f
                for (mode in selectableChoices) {
                    val modeY = ((settingY + rectHeight + 11 + ((8 + (modeCount * rectHeight)) * openAnim.output))).toFloat()
                    DrRenderUtils.resetColor()
                    val hoveringMode = isClickable(modeY - 5f) && openAnim.direction == Direction.FORWARDS && DrRenderUtils.isHovering(x + 5, modeY - 5, width - 10, rectHeight, mouseX, mouseY)
                    val modeHover = multiModesHoverAnimation[setting]!![mode]!!
                    modeHover.direction = if (hoveringMode) Direction.FORWARDS else Direction.BACKWARDS

                    if (modeHover.finished(Direction.FORWARDS) || !modeHover.isDone) {
                        val choiceSelected = setting.isSelected(mode)
                        val choiceColor = if (choiceSelected) {
                            if (accent) accentedColor2 else baseTextColor
                        } else {
                            DrRenderUtils.applyOpacity(baseTextColor, 0.4f)
                        }
                        drawCustomShapeWithRadius(x + 5, modeY - 5, width - 10, rectHeight, 3f, DrRenderUtils.applyOpacity(choiceColor, (.4f * modeHover.output).toFloat()))
                    }
                    if (openAnim.isDone && openAnim.direction == Direction.FORWARDS || !openAnim.isDone) {
                        val choiceSelected = setting.isSelected(mode)
                        val textColor = if (choiceSelected) {
                            if (accent) accentedColor2 else baseTextColor
                        } else {
                            DrRenderUtils.applyOpacity(baseTextColor, 0.6f)
                        }
                        Fonts.InterMedium_18.drawString("• $mode", x + 13, modeY, textColor.rgb, false)
                    }
                    modeCount++
                }

                if (!openAnim.isDone) GL11.glDisable(GL11.GL_SCISSOR_TEST)
                drawCustomShapeWithRadius(x + 5, settingY + 5, width - 10, rectHeight + 7, 3f, DrRenderUtils.applyOpacity(darkRectHover, .45f))

                if (!hoverAnim.isDone || hoverAnim.finished(Direction.FORWARDS)) {
                    drawCustomShapeWithRadius(x + 5, settingY + 5, width - 10, rectHeight + 7, 3f, DrRenderUtils.applyOpacity(baseTextColor, (.2f * hoverAnim.output).toFloat()))
                }

                Fonts.InterMedium_14.drawString(setting.name, x + 13, settingY + 9, baseTextColor.rgb, false)
                DrRenderUtils.resetColor()
                val displayText = if (setting.get().isEmpty()) "None" else setting.get().joinToString(", ")
                Fonts.InterBold_18.drawString(displayText, x + 13, (settingY + 17.5).toFloat(), baseTextColor.rgb, false)
                DrRenderUtils.resetColor()
                DrRenderUtils.drawClickGuiArrow(x + width - 15, settingY + 17, 5f, openAnim, baseTextColor.rgb)

                count += 1.5 + ((mathSize / rectHeight) * openAnim.output)
            }

            // ----- TextValue -----
            else if (setting is TextValue) {
                val startText = setting.name + ": "
                val titleX = x + 5f
                val textY = settingY + (rectHeight - Fonts.InterMedium_18.height) / 2f
                val textX = titleX + Fonts.InterMedium_18.stringWidth(startText).toFloat()
                var highlightCursor: (Float) -> Unit = {}
                chosenText?.let {
                    if (it.value == setting) {
                        val input = it.string
                        if (it.selectionActive()) {
                            val start = textX - 1 + Fonts.InterMedium_18.stringWidth(input.take(it.selectionStart!!)).toFloat()
                            val end = textX - 1 + Fonts.InterMedium_18.stringWidth(input.take(it.selectionEnd!!)).toFloat()
                            drawRect(start, textY - 3f, end, textY + Fonts.InterMedium_18.height - 2f, Color(7, 152, 252).rgb)
                        }
                        highlightCursor = { cursorLocalX ->
                            val cursorX = cursorLocalX + Fonts.InterMedium_18.stringWidth(input.take(it.cursorIndex)).toFloat()
                            drawRect(cursorX, textY - 3f, cursorX + 1f, textY + Fonts.InterMedium_18.height - 2f, Color.WHITE.rgb)
                        }
                    }
                }
                Fonts.InterMedium_18.drawString(startText, titleX, textY, baseTextColor.rgb, false)
                Fonts.InterMedium_18.drawString(setting.get(), textX, textY, baseTextColor.rgb, false)
                highlightCursor(textX)
                count += 1.0
            }

            // ----- ColorValue -----
            else if (setting is ColorValue) {
                val currentColor = setting.selectedColor()
                Fonts.InterMedium_18.drawString(setting.name + ":", x + 5, settingY + 3, baseTextColor.rgb, false)
                val colorCodeText = "#%08X".format(currentColor.rgb)
                Fonts.InterMedium_18.drawString(colorCodeText, x + 5, settingY + 3 + Fonts.InterMedium_18.height + 2, baseTextColor.rgb, false)
                val previewSize = 9
                val previewX1 = x + width - 10 - previewSize
                val previewY1 = settingY + 2
                drawRect(previewX1, previewY1, x + width - 10, previewY1 + previewSize, currentColor.rgb)
                val rainbowPreviewX1 = previewX1 - previewSize - previewSize
                if (rainbowPreviewX1 > x + 4) {
                    drawRect(rainbowPreviewX1, previewY1, rainbowPreviewX1 + previewSize, previewY1 + previewSize, ColorUtils.rainbow(alpha = setting.opacitySliderY).rgb)
                }

                val extraOptionsHeight: Float = if (setting.showOptions && !setting.showPicker) {
                    val rgbaLabels = listOf("R", "G", "B", "A")
                    val labelWidth = rgbaLabels.maxOf { Fonts.InterMedium_18.stringWidth(it).toFloat() }
                    var optionY = settingY + 3 + Fonts.InterMedium_18.height * 2 + 4
                    rgbaLabels.forEachIndexed { index, label ->
                        val valueText = when (index) {
                            0 -> currentColor.red.toString()
                            1 -> currentColor.green.toString()
                            2 -> currentColor.blue.toString()
                            else -> currentColor.alpha.toString()
                        }
                        Fonts.InterMedium_18.drawString("$label:", x + 5, optionY, baseTextColor.rgb, false)
                        val valueTextColor = if (chosenText != null && chosenText!!.value == setting && setting.rgbaIndex == index) Color.WHITE else Color.LIGHT_GRAY
                        Fonts.InterMedium_18.drawString(valueText, x + 5 + labelWidth + 10, optionY, valueTextColor.rgb, false)
                        optionY += Fonts.InterMedium_18.height + 4
                    }
                    optionY - (settingY + 3 + Fonts.InterMedium_18.height * 2 + 4)
                } else 0f

                if (setting.showPicker) {
                    val colorPickerHeight = 50
                    val colorPickerStartY = (settingY + 15 + extraOptionsHeight).toInt()
                    val darkRectColor = Color(20, 20, 20, alphaAnimation)
                    drawRect(x + 5, colorPickerStartY.toFloat(), x + width - 5, (colorPickerStartY + colorPickerHeight).toFloat(), DrRenderUtils.applyOpacity(darkRectColor, 0.8f))
                    count += 1.0 + ((colorPickerHeight + extraOptionsHeight) / rectHeight)
                } else {
                    count += 1.0 + (extraOptionsHeight / rectHeight)
                }
            }

            // ----- FontValue -----
            else if (setting is FontValue) {
                val displayText = setting.displayName
                val regularFontWidth = Fonts.InterMedium_18.stringWidth(displayText).toFloat()
                val titleX = x + width / 2f - regularFontWidth / 2f
                val titleY = settingY + (rectHeight - Fonts.InterMedium_18.height) / 2f
                Fonts.InterMedium_18.drawString(displayText, titleX, titleY, baseTextColor.rgb, false)
                count += 1.0
            }

            // Render Key Bind
            else {
                val bind = Keyboard.getKeyName(module.keyBind)
                val hoveringBindRect = isClickable(y + (rectHeight - Fonts.InterBold_18.height) / 2f) && DrRenderUtils.isHovering(x + width - (Fonts.InterBold_18.stringWidth(bind) + 10), y + (rectHeight - Fonts.InterBold_18.height) / 2f, (Fonts.InterBold_18.stringWidth(bind) + 8).toFloat(), (Fonts.InterBold_18.height + 6).toFloat(), mouseX, mouseY)
                val animations = keySettingAnimMap[module]!!
                animations[1].direction = if (binding === module) Direction.FORWARDS else Direction.BACKWARDS
                animations[0].direction = if (hoveringBindRect) Direction.FORWARDS else Direction.BACKWARDS
                count += 1.0
            }
        }
        settingSize = count
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int) {
        var count = 0.0
        for (setting in module.values.stream().filter { obj: Value<*> -> obj.shouldRender() }
            .collect(Collectors.toList())) {
            val settingY = roundToHalf(y + (count * rectHeight)).toFloat()

            if (setting is FloatValue) {
                val totalSliderWidth = width - 10
                if (isClickable(settingY + 17f) && DrRenderUtils.isHovering(x + 5, settingY + 17, totalSliderWidth, 6f, mouseX, mouseY) && button == 0) {
                    draggingNumber = setting
                }
                count += 2.0
            } else if (setting is FloatRangeValue) {
                val totalSliderWidth = width - 10
                if (isClickable(settingY + 17f) && DrRenderUtils.isHovering(x + 5, settingY + 17, totalSliderWidth, 6f, mouseX, mouseY) && button == 0) {
                    draggingNumber = setting
                }
                count += 2.0
            } else if (setting is IntValue) {
                val totalSliderWidth = width - 10
                if (isClickable(settingY + 17f) && DrRenderUtils.isHovering(x + 5, settingY + 17, totalSliderWidth, 6f, mouseX, mouseY) && button == 0) {
                    draggingNumber = setting
                }
                count += 2.0
            } else if (setting is IntRangeValue) {
                val totalSliderWidth = width - 10
                if (isClickable(settingY + 17f) && DrRenderUtils.isHovering(x + 5, settingY + 17, totalSliderWidth, 6f, mouseX, mouseY) && button == 0) {
                    draggingNumber = setting
                }
                count += 2.0
            } else if (setting is BoolValue) {
                val switchWidth = 16f
                if (isClickable(settingY + 5f) && DrRenderUtils.isHovering(x + width - (switchWidth + 6), settingY + 5, switchWidth, 12f, mouseX, mouseY) && button == 0) {
                    setting.toggle()
                }
                count += 1.0
            } else if (setting is ListValue) {
                if (isClickable(settingY + 5f) && DrRenderUtils.isHovering(x + 5, settingY + 5, width - 10, rectHeight + 7, mouseX, mouseY)) {
                    if (button == 1) modeSettingClick[setting] = !(modeSettingClick[setting] ?: false)
                }
                var modeCount = 0f
                if (modeSettingClick[setting] ?: false) {
                    for (mode in setting.values) {
                        if (mode.equals(setting.get(), ignoreCase = true)) continue
                        val modeY = ((settingY + rectHeight + 11 + ((8 + (modeCount * rectHeight))))).toFloat()
                        if (isClickable(modeY - 5f) && DrRenderUtils.isHovering(x + 5, modeY - 5, width - 10, rectHeight, mouseX, mouseY) && button == 0) {
                            setting.set(mode, true)
                            modeSettingClick[setting] = false
                        }
                        modeCount++
                    }
                }
                count += 1.5 + (if (modeSettingClick[setting] ?: false) (setting.values.size - 1) else 0)
            } else if (setting is MultiChoiceValue) {
                if (isClickable(settingY + 5f) && DrRenderUtils.isHovering(x + 5, settingY + 5, width - 10, rectHeight + 7, mouseX, mouseY)) {
                    if (button == 1) multiModeSettingClick[setting] = !(multiModeSettingClick[setting] ?: false)
                }
                var modeCount = 0f
                if (multiModeSettingClick[setting] ?: false) {
                    for (mode in setting.getSelectableChoices()) {
                        val modeY = ((settingY + rectHeight + 11 + ((8 + (modeCount * rectHeight))))).toFloat()
                        if (isClickable(modeY - 5f) && DrRenderUtils.isHovering(x + 5, modeY - 5, width - 10, rectHeight, mouseX, mouseY) && button == 0) {
                            setting.toggle(mode)
                        }
                        modeCount++
                    }
                }
                count += 1.5 + (if (multiModeSettingClick[setting] ?: false) (setting.getSelectableChoices().size - 1) else 0)
            }
            else if (setting is TextValue) {
                val startText = setting.name + ": "
                val textX = x + 5f + Fonts.InterMedium_18.stringWidth(startText).toFloat()
                if (isClickable(settingY + 4f) && mouseX.toFloat() in textX..(x + width) && mouseY.toFloat() in (settingY + 2f)..(settingY + 4f + Fonts.InterMedium_18.height) && button == 0) {
                    chosenText = EditableText.forTextValue(setting)
                }
                count += 1.0
            } else if (setting is ColorValue) {
                val previewSize = 9
                val previewX1 = x + width - 10 - previewSize
                if (isClickable(settingY + 2f) && DrRenderUtils.isHovering(previewX1, settingY + 2, previewSize.toFloat(), previewSize.toFloat(), mouseX, mouseY)) {
                    if (button == 0) setting.rainbow = false
                    if (button == 1) setting.showPicker = !setting.showPicker
                }
                if (button == 1 && isClickable(settingY + 3f) && DrRenderUtils.isHovering(x + 5, settingY + 3, 50f, 20f, mouseX, mouseY)) {
                    setting.showOptions = !setting.showOptions
                }
                val extraOptionsHeight: Float = if (setting.showOptions && !setting.showPicker) {
                    val rgbaLabels = listOf("R", "G", "B", "A")
                    val labelWidth = rgbaLabels.maxOf { Fonts.InterMedium_18.stringWidth(it).toFloat() }
                    var optionY = settingY + 3 + Fonts.InterMedium_18.height * 2 + 4
                    rgbaLabels.forEachIndexed { index, _ ->
                        val rgbaTextX = x + 5 + labelWidth + 10
                        val valueText = when (index) {
                            0 -> setting.selectedColor().red.toString()
                            1 -> setting.selectedColor().green.toString()
                            2 -> setting.selectedColor().blue.toString()
                            else -> setting.selectedColor().alpha.toString()
                        }
                        val rgbaTextWidth = Fonts.InterMedium_18.stringWidth(valueText).toFloat()
                        if (isClickable(optionY) && mouseX.toFloat() in rgbaTextX..(rgbaTextX + rgbaTextWidth) && mouseY.toFloat() in (optionY - 2)..(optionY + Fonts.InterMedium_18.height + 2) && button == 0) {
                            chosenText = EditableText.forRGBA(setting, index)
                            setting.rgbaIndex = index
                        }
                        optionY += Fonts.InterMedium_18.height + 4
                    }
                    optionY - (settingY + 3 + Fonts.InterMedium_18.height * 2 + 4)
                } else 0f
                count += if (setting.showPicker) {
                    1.0 + (50f / rectHeight)
                } else {
                    1.0 + (extraOptionsHeight / rectHeight)
                }
            } else if (setting is FontValue) {
                val displayText = setting.displayName
                val regularFontWidth = Fonts.InterMedium_18.stringWidth(displayText).toFloat()
                val titleX = x + width / 2f - regularFontWidth / 2f
                if (isClickable(settingY) && DrRenderUtils.isHovering(titleX - 5f, settingY, regularFontWidth + 10f, rectHeight, mouseX, mouseY)) {
                    if (button == 0) setting.next() else if (button == 1) setting.previous()
                }
                count += 1.0
            } else {
                val bind = Keyboard.getKeyName(module.keyBind)
                if (isClickable(y + (rectHeight - Fonts.InterBold_18.height) / 2f) && DrRenderUtils.isHovering(x + width - (Fonts.InterBold_18.stringWidth(bind) + 10), y + (rectHeight - Fonts.InterBold_18.height) / 2f, (Fonts.InterBold_18.stringWidth(bind) + 8).toFloat(), (Fonts.InterBold_18.height + 6).toFloat(), mouseX, mouseY) && button == 0) {
                    binding = module
                }
                count += 1.0
            }
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        draggingNumber = null
        sliderValueHeld = null
    }

    private fun isClickable(currentY: Float): Boolean {
        // Threshold check: the element must be within the scrollable area and below the header
        return currentY > panelLimitY + 20f && currentY < panelLimitY + Main.allowedClickGuiHeight + 20f
    }

    private fun roundToHalf(d: Double): Double = (d * 2.0).roundToInt() / 2.0

    companion object {
        var scale: Float = 1.0f
    }
}