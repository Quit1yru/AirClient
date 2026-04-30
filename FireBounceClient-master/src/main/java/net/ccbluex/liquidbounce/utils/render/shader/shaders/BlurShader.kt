package net.ccbluex.liquidbounce.utils.render.shader.shaders

import net.ccbluex.liquidbounce.utils.render.shader.Shader
import org.lwjgl.opengl.GL20.glUniform1f
import org.lwjgl.opengl.GL20.glUniform2f

object BlurShader : Shader("blur.frag") {

    private var texelSizeUniform = -1
    private var radiusUniform = -1

    // 每帧需要更新的参数
    private var horizontalFlag = true
    private var radiusValue = 1f

    override fun setupUniforms() {
        setupUniform("texelSize")
        setupUniform("radius")
    }

    override fun updateUniforms() {
        if (texelSizeUniform == -1) texelSizeUniform = getUniform("texelSize")
        if (radiusUniform == -1) radiusUniform = getUniform("radius")

        val texelX = if (horizontalFlag) 1.0f / mc.displayWidth else 0f
        val texelY = if (horizontalFlag) 0f else 1.0f / mc.displayHeight

        glUniform2f(texelSizeUniform, texelX, texelY)
        glUniform1f(radiusUniform, radiusValue)
    }

    /**
     * 开始使用 shader
     * @param horizontal 是否水平方向
     * @param radius 模糊半径
     */
    fun start(horizontal: Boolean, radius: Float) {
        horizontalFlag = horizontal
        radiusValue = radius

        startShader()       // 激活 shader
        updateUniforms()    // 每次开始都更新 uniforms
    }

    fun stop() {
        stopShader()
    }
}
