package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.render.shader.shaders.BlurShaderNew
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.shader.Framebuffer
import org.lwjgl.opengl.GL11
import java.awt.Color

object BlurUtils : MinecraftInstance {

    private var framebuffer: Framebuffer? = null
    private var lastScaleFactor = 0
    private var lastWidth = 0
    private var lastHeight = 0

    /**
     * 在指定矩形区域应用模糊
     */
    fun blur(x: Float, y: Float, width: Float, height: Float, strength: Float) {
        if (strength <= 0) return

        val scaledResolution = ScaledResolution(mc)
        val scaleFactor = scaledResolution.scaleFactor
        val scaleWidth = scaledResolution.scaledWidth
        val scaleHeight = scaledResolution.scaledHeight

        // 确保 framebuffer 尺寸正确
        if (framebuffer == null || lastScaleFactor != scaleFactor || lastWidth != scaleWidth || lastHeight != scaleHeight) {
            framebuffer?.deleteFramebuffer()
            framebuffer = Framebuffer(scaleWidth, scaleHeight, true)
            framebuffer?.setFramebufferColor(0f, 0f, 0f, 0f)
            lastScaleFactor = scaleFactor
            lastWidth = scaleWidth
            lastHeight = scaleHeight
        }

        framebuffer?.let { fb ->
            // --- 模板缓冲，限制区域 ---
            GL11.glEnable(GL11.GL_STENCIL_TEST)
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT)
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF)
            GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE)
            GL11.glStencilMask(0xFF)
            GL11.glColorMask(false, false, false, false)

            // 绘制模糊区域到 stencil
            RenderUtils.drawRect(x, y, x + width, y + height, Color.WHITE.rgb)

            // 启用 stencil 测试
            GL11.glColorMask(true, true, true, true)
            GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF)
            GL11.glStencilMask(0x00)

// pass 1：水平
            fb.bindFramebuffer(true)
            BlurShaderNew.start(true, strength)
            mc.framebuffer.bindFramebufferTexture()
            RenderUtils.drawTexturedModalRect(0, 0, 0, 0, scaleWidth, scaleHeight, scaleWidth.toFloat())
            BlurShaderNew.stopShader()

// pass 2：垂直
            mc.framebuffer.bindFramebuffer(true)
            BlurShaderNew.start(false, strength)
            fb.bindFramebufferTexture()
            RenderUtils.drawTexturedModalRect(0, 0, 0, 0, scaleWidth, scaleHeight, scaleWidth.toFloat())
            BlurShaderNew.stopShader()


            // 关闭 stencil
            GL11.glDisable(GL11.GL_STENCIL_TEST)
            GlStateManager.resetColor()
        }
    }

    fun cleanup() {
        framebuffer?.deleteFramebuffer()
        framebuffer = null
    }
    fun blurArea(x: Float, y: Float, width: Float, height: Float, radius: Float) {
        InternalBlurShader.blurArea(x, y, width, height, radius)
    }

}
