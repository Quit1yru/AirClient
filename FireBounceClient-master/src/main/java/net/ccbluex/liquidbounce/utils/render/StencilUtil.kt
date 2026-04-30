package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import org.lwjgl.opengl.GL11

object StencilUtil : MinecraftInstance {

    private var stencilEnabled = false

    fun initStencilToWrite() {
        // 保存当前状态
        GL11.glPushAttrib(GL11.GL_STENCIL_BUFFER_BIT)

        GL11.glEnable(GL11.GL_STENCIL_TEST)
        GL11.glStencilMask(0xFF)  // 启用stencil写入
        GL11.glClearStencil(0)    // 设置清除值为0
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT)  // 清除stencil缓冲区

        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF)
        GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE)
        GL11.glColorMask(false, false, false, false)  // 禁用颜色写入
        GL11.glDepthMask(false)  // 禁用深度写入

        stencilEnabled = true
    }

    fun readStencilBuffer(ref: Int) {
        if (!stencilEnabled) return

        GL11.glColorMask(true, true, true, true)  // 启用颜色写入
        GL11.glDepthMask(true)  // 启用深度写入
        GL11.glStencilMask(0x00)  // 禁用stencil写入
        GL11.glStencilFunc(GL11.GL_EQUAL, ref, 0xFF)
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
    }

    fun uninitStencilBuffer() {
        if (!stencilEnabled) return

        GL11.glDisable(GL11.GL_STENCIL_TEST)
        GL11.glStencilMask(0xFF)  // 恢复stencil写入
        GL11.glPopAttrib()  // 恢复之前保存的状态

        stencilEnabled = false
    }
}