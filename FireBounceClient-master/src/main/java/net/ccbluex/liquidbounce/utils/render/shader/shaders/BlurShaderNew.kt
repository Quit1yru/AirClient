package net.ccbluex.liquidbounce.utils.render.shader.shaders

import com.mojang.realmsclient.util.RealmsTextureManager.GL_TEXTURE0
import com.mojang.realmsclient.util.RealmsTextureManager.glActiveTexture
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.render.shader.Shader
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.glBindTexture
import org.lwjgl.opengl.GL20.*
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sqrt

object BlurShaderNew : Shader("blur_new.frag"), MinecraftInstance {

    private var textureInUniform = -1
    private var texelSizeUniform = -1
    private var directionUniform = -1
    private var radiusUniform = -1
    private var weightsUniform = -1

    private var horizontalFlag = true
    private var radiusValue = 1f

    override fun setupUniforms() {
        textureInUniform = glGetUniformLocation(programId, "textureIn")
        texelSizeUniform = glGetUniformLocation(programId, "texelSize")
        directionUniform = glGetUniformLocation(programId, "direction")
        radiusUniform = glGetUniformLocation(programId, "radius")

        // 为权重数组设置多个独立uniform（因为无法直接传递完整数组）
        // 这里只设置前32个权重值，对于大多数情况足够了
        weightsUniform = glGetUniformLocation(programId, "weights[0]")
    }

    override fun updateUniforms() {
        // 设置纹理单元
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, 0)
        glUniform1i(textureInUniform, 0)

        // 设置纹理尺寸
        val texelX = 1.0f / mc.displayWidth
        val texelY = 1.0f / mc.displayHeight
        glUniform2f(texelSizeUniform, texelX, texelY)

        // 设置方向
        val dirX = if (horizontalFlag) 1.0f else 0.0f
        val dirY = if (horizontalFlag) 0.0f else 1.0f
        glUniform2f(directionUniform, dirX, dirY)

        // 设置半径
        glUniform1f(radiusUniform, radiusValue)

        // 设置权重数组（前32个值）
        val weights = FloatArray(32)
        for (i in 0 until 32) {
            weights[i] = gaussian(i.toFloat(), radiusValue / 2.0f)
        }

        // 逐个设置权重uniform值
        for (i in weights.indices) {
            glUniform1f(weightsUniform + i, weights[i])
        }
    }

    /**
     * 计算高斯权重
     */
    private fun gaussian(x: Float, sigma: Float): Float {
        return (if (sigma <= 0) {
            if (x == 0f) 1.0f else 0.0f
        } else {
            val sigmaSquared = sigma * sigma
            (1.0 / sqrt(2.0 * PI * sigmaSquared)) *
                    exp(-(x * x) / (2.0 * sigmaSquared))
        }) as Float
    }

    /**
     * 开始使用 shader
     * @param horizontal 是否水平方向
     * @param radius 模糊半径
     */
    fun start(horizontal: Boolean, radius: Float) {
        horizontalFlag = horizontal
        radiusValue = radius.coerceIn(1f, 20f) // 限制半径范围

        startShader()
        updateUniforms()
    }

    fun stop() {
        stopShader()
    }
}
