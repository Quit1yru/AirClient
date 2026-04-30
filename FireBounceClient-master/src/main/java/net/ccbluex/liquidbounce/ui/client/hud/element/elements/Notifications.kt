/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
@file:Suppress("SameParameterValue")

package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.HUD.addNotification
import net.ccbluex.liquidbounce.ui.client.hud.HUD.notifications
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification.Companion.maxTextLength
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.client.ClientUtils
import net.ccbluex.liquidbounce.utils.extensions.lerpWith
import net.ccbluex.liquidbounce.utils.extra.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorder
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.shader.Framebuffer
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * CustomHUD Notification element
 */
@ElementInfo(name = "Notifications", single = true, priority = -1)
class Notifications(
    x: Double = 0.0, y: Double = 30.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.DOWN)
) : Element("Notifications", x, y, scale, side) {

    val styles by choices("Styles", arrayOf("Liquidbounce", "Classic", "WinUI"), "WinUI")
    val shadowCheck by boolean("ShadowCheck", true) { styles == "Classic" }
    val shadowStrength by int("ShadowStrength", 1, 1..2) { styles == "Classic" }
    val horizontalFade by choices("HorizontalFade", arrayOf("InOnly", "OutOnly", "Both", "None"), "OutOnly")
    val padding by int("Padding", 5, -1..20)
    val roundRadius by float("RoundRadius", 3f, 0f..10f)
    val color by color("BackgroundColor", Color.BLACK.withAlpha(128))
    val renderBorder by boolean("RenderBorder", false)
    val borderColor by color("BorderColor", Color.BLUE.withAlpha(255)) { renderBorder }
    val borderWidth by float("BorderWidth", 2f, 0.5F..5F) { renderBorder }

    private val exampleNotification = Notification("Example Title", "Example Description")

    private var index = 0

    override fun updateElement() {
        if (mc.currentScreen is GuiHudDesigner && ClientUtils.runTimeTicks % 60 == 0) {
            exampleNotification.severityType = SeverityType.entries[++index % SeverityType.entries.size]
        }
    }

    override fun drawElement(): Border? {
        var verticalOffset = 0f

        maxTextLength = maxOf(100, notifications.maxOfOrNull { it.textLength } ?: 0)

        notifications.removeIf { notification ->
            if (notification != exampleNotification) {
                notification.y = (notification.y..verticalOffset).lerpWith(RenderUtils.deltaTimeNormalized())
            }

            notification.drawNotification(this).also { if (!it) verticalOffset += Notification.MAX_HEIGHT + padding }
        }

        if (mc.currentScreen is GuiHudDesigner) {
            if (exampleNotification !in notifications) {
                index = 0
                addNotification(exampleNotification)
            }

            exampleNotification.fadeState = Notification.FadeState.STAY
            exampleNotification.textLength = Fonts.fontSemibold40.getStringWidth(exampleNotification.longestString)

            val notificationHeight = Notification.MAX_HEIGHT

            exampleNotification.y = 0F

            return Border(
                -(maxTextLength.toFloat() + 24 + 20), -notificationHeight.toFloat(), 0F, 0F
            )
        }

        return null
    }

    enum class SeverityType(val path: ResourceLocation) {
        SUCCESS(ResourceLocation("liquidbounce/notifications/success.png")),
        RED_SUCCESS(ResourceLocation("liquidbounce/notifications/redsuccess.png")),
        INFO(ResourceLocation("liquidbounce/notifications/info.png")),
        WARNING(ResourceLocation("liquidbounce/notifications/warning.png")),
        ERROR(ResourceLocation("liquidbounce/notifications/error.png"))
    }
}

class Notification(
    var title: String,
    var description: String,
    private val delay: Long = 2000L,
    var severityType: Notifications.SeverityType = Notifications.SeverityType.INFO
) {
    var x = 0F

    // Spawn the notification 32 pixels above the last one - if exists.
    var y: Float = (notifications.lastOrNull()?.y ?: 0F) + MAX_HEIGHT * 2
    var textLength = 0

    val longestString
        get() = arrayOf(title, description).maxBy { Fonts.fontSemibold40.getStringWidth(it) }

    private var stay = delay
    private var fadeStep = 0F
    var fadeState = FadeState.IN

    fun replaceModuleNotification(title: String, description: String, severityType: Notifications.SeverityType) {
        if (fadeState.ordinal > 1) {
            return
        }
        stay = delay
        this.severityType = severityType
        this.title = title
        this.description = description

        textLength = Fonts.fontSemibold40.getStringWidth(longestString)
        maxTextLength = maxOf(textLength, maxTextLength)

        notifications.sortBy { it.stay }
    }

    companion object {
        fun informative(title: String, message: String, delay: Long = 2000L) =
            Notification(title, message, delay, Notifications.SeverityType.INFO)

        fun informative(title: Module, message: String, delay: Long = 2000L) =
            Notification(title.spacedName, message, delay, Notifications.SeverityType.INFO)

        fun error(title: Module, message: String, delay: Long = 2000L) =
            Notification(title.spacedName, message, delay, Notifications.SeverityType.ERROR)

        fun warning(title: Module, message: String, delay: Long = 2000L) =
            Notification(title.spacedName, message, delay, Notifications.SeverityType.WARNING)

        var maxTextLength = 0
        const val MAX_HEIGHT = 36
        const val ICON_SIZE = 24
    }

    enum class FadeState {
        IN, STAY, OUT, END
    }

    init {
        textLength = Fonts.fontSemibold40.getStringWidth(longestString)
        maxTextLength = maxOf(maxTextLength, textLength)
    }

    fun drawNotification(element: Notifications): Boolean {
        val delta = deltaTime
        val notificationWidth = maxTextLength + ICON_SIZE + 24F

        val currentX = when (fadeState) {
            FadeState.IN -> if (element.horizontalFade in arrayOf("InOnly", "Both")) x else notificationWidth
            FadeState.OUT -> if (element.horizontalFade in arrayOf("OutOnly", "Both")) x else notificationWidth
            else -> x
        }

        when (fadeState) {
            FadeState.IN -> {
                if (x < notificationWidth) x += delta
                if (x >= notificationWidth) {
                    fadeState = FadeState.STAY
                    x = notificationWidth
                    fadeStep = notificationWidth
                }
                stay = delay
            }
            FadeState.STAY -> {
                if (textLength != maxTextLength) {
                    maxTextLength = maxOf(textLength, maxTextLength)
                    x = maxTextLength + ICON_SIZE + 24F
                    fadeStep = x
                }
                stay -= delta
                if (stay <= 0) fadeState = FadeState.OUT
            }
            FadeState.OUT -> if (x > 0) {
                x -= delta
                y -= delta / 4F
            } else {
                fadeState = FadeState.END
                return true
            }
            FadeState.END -> return true
        }

        when (element.styles) {
            "Liquidbounce" -> {
                val extraSpace = 4F
                drawRoundedRect(0F, -y - MAX_HEIGHT, -currentX - extraSpace, -y, element.color.rgb, element.roundRadius)
                if (element.renderBorder) {
                    drawRoundedBorder(0F, -y - MAX_HEIGHT, -currentX - extraSpace, -y, element.borderWidth, element.borderColor.rgb, element.roundRadius)
                }
                val nearTopSpot = -y - MAX_HEIGHT + 12
                Fonts.fontSemibold40.drawString(title, ICON_SIZE + 8F - currentX, nearTopSpot - 5, Color.WHITE.rgb)
                Fonts.fontSemibold35.drawString(description, ICON_SIZE + 8F - currentX, nearTopSpot + Fonts.fontSemibold40.fontHeight - 2, Int.MAX_VALUE)
                RenderUtils.drawImage(severityType.path, -currentX + 2, -y - MAX_HEIGHT + 6, ICON_SIZE, ICON_SIZE, radius = element.roundRadius)
            }
            "Classic" -> {
                val extraSpace = 4F
                val ofst = 145f
                val (backgroundColor, borderColor, textColor) = when (severityType) {
                    Notifications.SeverityType.SUCCESS -> Triple(Color(28, 148, 97).withAlpha(element.color.alpha.coerceAtLeast(180)), Color(46, 170, 80).withAlpha(230), Color.WHITE)
                    Notifications.SeverityType.RED_SUCCESS -> Triple(Color(137, 39, 39).withAlpha(element.color.alpha.coerceAtLeast(180)), Color(229, 57, 53).withAlpha(230), Color.WHITE)
                    Notifications.SeverityType.INFO -> Triple(Color(52, 152, 219).withAlpha(element.color.alpha.coerceAtLeast(180)), Color(41, 128, 185).withAlpha(230), Color.WHITE)
                    Notifications.SeverityType.WARNING -> Triple(Color(255, 193, 7).withAlpha(element.color.alpha.coerceAtLeast(180)), Color(245, 166, 35).withAlpha(230), Color(33, 33, 33))
                    Notifications.SeverityType.ERROR -> Triple(Color(239, 83, 80).withAlpha(element.color.alpha.coerceAtLeast(180)), Color(222, 50, 50).withAlpha(230), Color.WHITE)
                }
                val txWd = Fonts.fontGoogleSans45.getStringWidth("$title $description")
                if (element.shadowCheck) {
                    val glowX = -currentX - extraSpace - txWd - 15f + ofst
                    val glowY = -y - MAX_HEIGHT + 2f
                    val glowWidth = txWd + 17f
                    val glowHeight = MAX_HEIGHT - 9f
                    GlowUtils.drawGlow(glowX, glowY, glowWidth, glowHeight, (element.shadowStrength * 13F).toInt(), Color(0, 0, 0, 120))
                }
                drawRoundedRect(-currentX - extraSpace - txWd - 15f + ofst, -y - MAX_HEIGHT + 2f, -currentX - extraSpace - 2F + ofst, -y - 7f, backgroundColor.rgb, element.roundRadius)
                if (element.renderBorder) {
                    drawRoundedBorder(-currentX - extraSpace - txWd - 15f + ofst, -y - MAX_HEIGHT + 2f, -currentX - extraSpace - 2F + ofst, -y - 7f, element.borderWidth, borderColor.rgb, element.roundRadius)
                }
                val nearTopSpot = -y - MAX_HEIGHT + 10
                Fonts.fontGoogleSans45.drawString("$title $description!", -currentX - extraSpace - txWd - 8f + ofst, nearTopSpot + 5 - 6F, textColor.rgb)
            }
            "WinUI" -> {
                val extraSpace = 4F

                // WinUI Color Palette (Acrylic dark)
                val backgroundColor = Color(32, 32, 32, 100) // Low opacity to let blur show through
                val borderColor = Color(60, 60, 60, 255)
                val highlightColor = Color(255, 255, 255, 20)

                // Accent colors
                val accentColor = when (severityType) {
                    Notifications.SeverityType.SUCCESS -> Color(16, 124, 16)
                    Notifications.SeverityType.RED_SUCCESS -> Color(232, 17, 35) // Red
                    Notifications.SeverityType.INFO -> Color(0, 120, 215)
                    Notifications.SeverityType.WARNING -> Color(255, 140, 0)
                    Notifications.SeverityType.ERROR -> Color(232, 17, 35)
                }

                // Coordinates
                val rectX = -currentX - extraSpace
                val rectY = -y - MAX_HEIGHT
                val rectX2 = 0F
                val rectY2 = -y
                val radius = 5f

                // 1. Draw Gaussian Blur
                // Calculate absolute screen coordinates for the scissor blur
                val blurX = element.renderX.toFloat() + rectX
                val blurY = element.renderY.toFloat() + rectY
                val blurW = rectX2 - rectX
                val blurH = rectY2 - rectY

                InternalBlurShader.blurArea(blurX, blurY, blurW, blurH, 15f)

                // 2. Draw Soft Shadow
                GlowUtils.drawGlow(rectX, rectY, rectX2 - rectX, MAX_HEIGHT.toFloat(), 12, Color(0, 0, 0, 80))

                // 3. Draw Background
                drawRoundedRect(rectX, rectY, rectX2, rectY2, backgroundColor.rgb, radius)

                // 4. Draw Top Highlight
                RenderUtils.drawRect(rectX + radius, rectY, rectX2 - radius, rectY + 1f, highlightColor.rgb)

                // 5. Border
                if (element.renderBorder) {
                    drawRoundedBorder(rectX, rectY, rectX2, rectY2, 1f, borderColor.rgb, radius)
                } else {
                    drawRoundedBorder(rectX, rectY, rectX2, rectY2, 1f, Color(255, 255, 255, 10).rgb, radius)
                }

                // Layout Config
                val contentStartX = rectX + 8f // No more left bar, so less padding needed
                val contentCenterY = rectY + MAX_HEIGHT / 2f

                // 6. Draw Icon
                val iconSize = 20f
                val iconX = contentStartX
                val iconY = contentCenterY - iconSize / 2f

                // Ensure pure shapes rendering
                GlStateManager.disableTexture2D()
                GlStateManager.enableBlend()
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)

                // 6a. Circle Background
                drawFilledCircle(iconX + iconSize / 2f, iconY + iconSize / 2f, iconSize / 2f, accentColor)

                // 6b. Symbol
                GlStateManager.pushMatrix()
                GlStateManager.translate(iconX, iconY, 0f)

                GL11.glEnable(GL11.GL_LINE_SMOOTH)
                GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)

                RenderUtils.glColor(Color.WHITE)
                GL11.glLineWidth(2.5f)

                when (severityType) {
                    Notifications.SeverityType.SUCCESS -> drawCheckMark(iconSize)
                    // Fix: Disabled modules (Red Success or Error) should show Cross, not Check
                    Notifications.SeverityType.RED_SUCCESS -> drawCrossMark(iconSize)
                    Notifications.SeverityType.ERROR -> drawCrossMark(iconSize)
                    Notifications.SeverityType.INFO -> drawInfoMark(iconSize)
                    Notifications.SeverityType.WARNING -> drawExclamationMark(iconSize)
                }

                GL11.glDisable(GL11.GL_LINE_SMOOTH)
                GlStateManager.popMatrix()
                GlStateManager.enableTexture2D()

                // 7. Text Layout
                val textStartX = contentStartX + iconSize + 8f
                val titleY = rectY + 6f

                // Title
                Fonts.fontSemibold40.drawString(title, textStartX, titleY, Color.WHITE.rgb)

                // Description
                Fonts.fontSemibold35.drawString(
                    description, textStartX, titleY + 13f, Color(210, 210, 210).rgb
                )
            }
        }

        return false
    }

    // --- Custom Geometric Icon Drawers ---

    private fun drawCheckMark(size: Float) {
        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glVertex2f(size * 0.28f, size * 0.52f)
        GL11.glVertex2f(size * 0.45f, size * 0.70f)
        GL11.glVertex2f(size * 0.72f, size * 0.35f)
        GL11.glEnd()
    }

    private fun drawCrossMark(size: Float) {
        val margin = 0.35f
        GL11.glBegin(GL11.GL_LINES)
        GL11.glVertex2f(size * margin, size * margin)
        GL11.glVertex2f(size * (1 - margin), size * (1 - margin))
        GL11.glVertex2f(size * (1 - margin), size * margin)
        GL11.glVertex2f(size * margin, size * (1 - margin))
        GL11.glEnd()
    }

    private fun drawInfoMark(size: Float) {
        drawFilledCircle(size * 0.5f, size * 0.32f, size * 0.08f, Color.WHITE)
        drawFilledRect(size * 0.44f, size * 0.46f, size * 0.12f, size * 0.3f)
    }

    private fun drawExclamationMark(size: Float) {
        drawFilledRect(size * 0.44f, size * 0.25f, size * 0.12f, size * 0.38f)
        drawFilledCircle(size * 0.5f, size * 0.75f, size * 0.08f, Color.WHITE)
    }

    private fun drawFilledRect(x: Float, y: Float, w: Float, h: Float) {
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex2f(x, y)
        GL11.glVertex2f(x + w, y)
        GL11.glVertex2f(x + w, y + h)
        GL11.glVertex2f(x, y + h)
        GL11.glEnd()
    }

    private fun drawFilledCircle(cx: Float, cy: Float, r: Float, color: Color) {
        RenderUtils.glColor(color)
        GL11.glBegin(GL11.GL_TRIANGLE_FAN)
        GL11.glVertex2f(cx, cy)
        for (i in 0..360 step 10) {
            val angle = Math.toRadians(i.toDouble())
            GL11.glVertex2f((cx + cos(angle) * r).toFloat(), (cy + sin(angle) * r).toFloat())
        }
        GL11.glEnd()
    }
}

// --- Shader Helper ---
object InternalBlurShader {
    private val mc = Minecraft.getMinecraft()
    private var blurOutputFramebuffer: Framebuffer? = null
    private var shaderProgramID: Int = -1
    private var uniformTextureLocation = -1
    private var uniformTexelSizeLocation = -1
    private var uniformDirectionLocation = -1
    private var uniformRadiusLocation = -1

    fun blurArea(x: Float, y: Float, width: Float, height: Float, radius: Float) {
        val sr = ScaledResolution(mc)
        val factor = sr.scaleFactor
        ensureShaderInitialized()
        ensureFramebuffer(mc.displayWidth, mc.displayHeight)

        val sX = (x * factor).toInt()
        val sY = (mc.displayHeight - (y * factor).toInt() - (height * factor).toInt())
        val sW = (width * factor).toInt()
        val sH = (height * factor).toInt()

        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        val pad = (radius * factor).toInt()
        GL11.glScissor(sX - pad, sY - pad, sW + pad * 2, sH + pad * 2)

        val buffer = blurOutputFramebuffer ?: return
        val mainBuffer = mc.framebuffer

        buffer.framebufferClear()
        buffer.bindFramebuffer(true)
        mainBuffer.bindFramebufferTexture()
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 33071)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 33071)

        GL20.glUseProgram(shaderProgramID)
        GL20.glUniform2f(uniformTexelSizeLocation, 1.0f / mc.displayWidth, 1.0f / mc.displayHeight)
        GL20.glUniform1i(uniformTextureLocation, 0)
        GL20.glUniform1f(uniformRadiusLocation, radius)
        GL20.glUniform2f(uniformDirectionLocation, 1.0f, 0.0f)
        drawQuads()

        mainBuffer.bindFramebuffer(true)
        buffer.bindFramebufferTexture()
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 33071)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 33071)

        GL20.glUniform2f(uniformDirectionLocation, 0.0f, 1.0f)
        drawQuads()

        GL20.glUseProgram(0)
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }

    private fun ensureShaderInitialized() {
        if (shaderProgramID != -1) return
        val vertexShaderSrc = "#version 120\nvoid main() { gl_TexCoord[0] = gl_MultiTexCoord0; gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex; }"
        val fragmentShaderSrc = "#version 120\nuniform sampler2D textureIn; uniform vec2 texelSize; uniform vec2 direction; uniform float radius;\nfloat gaussian(float x, float sigma) { return exp(-(x*x) / (2.0 * sigma * sigma)); }\nvoid main() { vec2 coord = gl_TexCoord[0].xy; vec4 sum = vec4(0.0); float totalWeight = 0.0; int range = int(min(radius, 50.0)); float sigma = radius / 2.0; for (int i = -range; i <= range; i++) { float weight = gaussian(float(i), sigma); vec2 offset = float(i) * texelSize * direction; sum += texture2D(textureIn, coord + offset) * weight; totalWeight += weight; } gl_FragColor = sum / totalWeight; }"
        val vID = createShader(vertexShaderSrc, GL20.GL_VERTEX_SHADER)
        val fID = createShader(fragmentShaderSrc, GL20.GL_FRAGMENT_SHADER)
        shaderProgramID = GL20.glCreateProgram()
        GL20.glAttachShader(shaderProgramID, vID)
        GL20.glAttachShader(shaderProgramID, fID)
        GL20.glLinkProgram(shaderProgramID)
        GL20.glUseProgram(shaderProgramID)
        uniformTextureLocation = GL20.glGetUniformLocation(shaderProgramID, "textureIn")
        uniformTexelSizeLocation = GL20.glGetUniformLocation(shaderProgramID, "texelSize")
        uniformDirectionLocation = GL20.glGetUniformLocation(shaderProgramID, "direction")
        uniformRadiusLocation = GL20.glGetUniformLocation(shaderProgramID, "radius")
        GL20.glUseProgram(0)
    }
    private fun ensureFramebuffer(w: Int, h: Int) {
        if (blurOutputFramebuffer == null || blurOutputFramebuffer!!.framebufferWidth != w || blurOutputFramebuffer!!.framebufferHeight != h) {
            blurOutputFramebuffer?.deleteFramebuffer()
            blurOutputFramebuffer = Framebuffer(w, h, true)
            blurOutputFramebuffer!!.setFramebufferFilter(9729)
        }
    }
    private fun createShader(src: String, type: Int): Int {
        val id = GL20.glCreateShader(type)
        GL20.glShaderSource(id, src)
        GL20.glCompileShader(id)
        return id
    }
    private fun drawQuads() {
        val sr = ScaledResolution(mc)
        val w = sr.scaledWidth_double
        val h = sr.scaledHeight_double
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glTexCoord2f(0f, 1f); GL11.glVertex2d(0.0, 0.0)
        GL11.glTexCoord2f(0f, 0f); GL11.glVertex2d(0.0, h)
        GL11.glTexCoord2f(1f, 0f); GL11.glVertex2d(w, h)
        GL11.glTexCoord2f(1f, 1f); GL11.glVertex2d(w, 0.0)
        GL11.glEnd()
    }
}