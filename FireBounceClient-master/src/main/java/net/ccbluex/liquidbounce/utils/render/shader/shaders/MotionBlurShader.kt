// MotionBlurShader.kt
package net.ccbluex.liquidbounce.utils.render.shader.shaders

import net.ccbluex.liquidbounce.utils.render.shader.FramebufferShader
import org.lwjgl.opengl.GL20.glUniform1f
import org.lwjgl.opengl.GL20.glUniform2f
import java.awt.Color

object MotionBlurShader : FramebufferShader("motionblur.frag") {

    // Uniform 变量
    private var blurAmountUniform = -1
    private var texelSizeUniform = -1

    // 当前参数
    private var blurAmount = 0.5f
    private var horizontal = true

    override fun setupUniforms() {
        super.setupUniforms()
        setupUniform("blurAmount")
        setupUniform("texelSize")
    }

    override fun updateUniforms() {
        super.updateUniforms()

        if (blurAmountUniform == -1) blurAmountUniform = getUniform("blurAmount")
        if (texelSizeUniform == -1) texelSizeUniform = getUniform("texelSize")

        // 计算纹理像素大小
        val texelX = if (horizontal) 1.0f / mc.displayWidth else 0f
        val texelY = if (horizontal) 0f else 1.0f / mc.displayHeight

        glUniform1f(blurAmountUniform, blurAmount)
        glUniform2f(texelSizeUniform, texelX, texelY)
    }

    /**
     * 开始应用运动模糊
     * @param partialTicks 部分刻
     * @param intensity 模糊强度 (0.0-1.0)
     * @param quality 模糊质量 (1-5)
     */
    fun renderMotionBlur(partialTicks: Float, intensity: Float, quality: Int = 3) {
        if (intensity <= 0.001f) return

        // 保存当前状态
        blurAmount = intensity

        // 多次迭代以获得更好的模糊效果
        for (i in 0 until quality) {
            // 水平模糊
            horizontal = true
            renderPass(partialTicks, Color.WHITE)

            // 垂直模糊
            horizontal = false
            renderPass(partialTicks, Color.WHITE)
        }
    }

    /**
     * 渲染单次模糊
     */
    private fun renderPass(partialTicks: Float, color: Color) {
        // 开始绘制到Framebuffer
        startDraw(partialTicks, 1.0f)

        // 设置颜色和参数
        this.red = color.red / 255f
        this.green = color.green / 255f
        this.blue = color.blue / 255f
        this.alpha = color.alpha / 255f

        // 停止绘制并应用着色器
        stopDraw(color, 0, 0, 0f)
    }

    /**
     * 快速应用模糊（性能优化版）
     */
    fun applyQuick(partialTicks: Float, intensity: Float) {
        blurAmount = intensity.coerceIn(0.0f, 1.0f)

        // 单次水平+垂直模糊
        horizontal = true
        renderPass(partialTicks, Color.WHITE)

        horizontal = false
        renderPass(partialTicks, Color.WHITE)
    }
}