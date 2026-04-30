/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render.shader.shaders

import net.ccbluex.liquidbounce.utils.render.shader.FramebufferShader
import org.lwjgl.opengl.GL20.*

object BlurShader2 : FramebufferShader("blur2.frag") {

    // 可调节参数
    var blurRadius = 8
    var blurStrength = 3.0f
    var blurQuality = 2.0f

    override fun setupUniforms() {
        setupUniform("texture")
        setupUniform("texelSize")
        setupUniform("radius")
        setupUniform("strength")
        setupUniform("direction") // 0: horizontal, 1: vertical
        setupUniform("quality")
    }

    override fun updateUniforms() {
        glUniform1i(getUniform("texture"), 0)
        glUniform2f(getUniform("texelSize"),
            1f / mc.displayWidth * renderScale,
            1f / mc.displayHeight * renderScale
        )
        glUniform1i(getUniform("radius"), blurRadius)
        glUniform1f(getUniform("strength"), blurStrength)
        glUniform1f(getUniform("quality"), blurQuality)
        // 方向在渲染时动态设置
    }

    fun renderHorizontal() {
        glUniform1i(getUniform("direction"), 0)
    }

    fun renderVertical() {
        glUniform1i(getUniform("direction"), 1)
    }
}