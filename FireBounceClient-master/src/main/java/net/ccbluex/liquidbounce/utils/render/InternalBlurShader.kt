/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.shader.Framebuffer
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20
import net.minecraft.client.renderer.GlStateManager

object InternalBlurShader {
    private val mc = Minecraft.getMinecraft()
    private var blurOutputFramebuffer: Framebuffer? = null
    private var shaderProgramID: Int = -1
    private var uniformTextureLocation = -1
    private var uniformTexelSizeLocation = -1
    private var uniformDirectionLocation = -1
    private var uniformRadiusLocation = -1

    fun blurArea(x: Float, y: Float, width: Float, height: Float, radius: Float) {
        if (radius <= 0) return
        
        ensureShaderInitialized()
        ensureFramebuffer(mc.displayWidth, mc.displayHeight)

        val buffer = blurOutputFramebuffer ?: return
        val mainBuffer = mc.framebuffer

        // Pass 1: Horizontal Blur
        buffer.framebufferClear()
        buffer.bindFramebuffer(true)
        mainBuffer.bindFramebufferTexture()
        
        GL20.glUseProgram(shaderProgramID)
        GL20.glUniform2f(uniformTexelSizeLocation, 1.0f / mc.displayWidth, 1.0f / mc.displayHeight)
        GL20.glUniform1i(uniformTextureLocation, 0)
        GL20.glUniform1f(uniformRadiusLocation, radius)
        GL20.glUniform2f(uniformDirectionLocation, 1.0f, 0.0f)
        drawFullScreenQuad()

        // Pass 2: Vertical Blur
        mainBuffer.bindFramebuffer(true)
        buffer.bindFramebufferTexture()
        
        GL20.glUniform2f(uniformDirectionLocation, 0.0f, 1.0f)
        drawFullScreenQuad()

        GL20.glUseProgram(0)
    }

    private fun ensureShaderInitialized() {
        if (shaderProgramID != -1) return
        val vertexShaderSrc = """
            #version 120
            void main() {
                gl_TexCoord[0] = gl_MultiTexCoord0;
                gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
            }
        """.trimIndent()
        
        val fragmentShaderSrc = """
            #version 120
            uniform sampler2D textureIn;
            uniform vec2 texelSize;
            uniform vec2 direction;
            uniform float radius;
            
            float gaussian(float x, float sigma) {
                return exp(-(x*x) / (2.0 * sigma * sigma));
            }
            
            void main() {
                vec2 coord = gl_TexCoord[0].xy;
                vec4 sum = vec4(0.0);
                float totalWeight = 0.0;
                int range = int(min(radius, 50.0));
                float sigma = radius / 2.0;
                
                for (int i = -range; i <= range; i++) {
                    float weight = gaussian(float(i), sigma);
                    vec2 offset = float(i) * texelSize * direction;
                    sum += texture2D(textureIn, coord + offset) * weight;
                    totalWeight += weight;
                }
                gl_FragColor = sum / totalWeight;
            }
        """.trimIndent()

        val vID = createShader(vertexShaderSrc, GL20.GL_VERTEX_SHADER)
        val fID = createShader(fragmentShaderSrc, GL20.GL_FRAGMENT_SHADER)
        shaderProgramID = GL20.glCreateProgram()
        GL20.glAttachShader(shaderProgramID, vID)
        GL20.glAttachShader(shaderProgramID, fID)
        GL20.glLinkProgram(shaderProgramID)
        
        uniformTextureLocation = GL20.glGetUniformLocation(shaderProgramID, "textureIn")
        uniformTexelSizeLocation = GL20.glGetUniformLocation(shaderProgramID, "texelSize")
        uniformDirectionLocation = GL20.glGetUniformLocation(shaderProgramID, "direction")
        uniformRadiusLocation = GL20.glGetUniformLocation(shaderProgramID, "radius")
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
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL_FALSE) {
            println("Shader Error: " + GL20.glGetShaderInfoLog(id, 1024))
        }
        return id
    }

    private fun drawFullScreenQuad() {
        val sr = ScaledResolution(mc)
        val w = sr.scaledWidth_double
        val h = sr.scaledHeight_double

        glPushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.disableAlpha()
        GlStateManager.disableDepth()

        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        GlStateManager.color(1f, 1f, 1f, 1f) // Reset color to white

        // Set up Ortho Projection for pixel-perfect drawing
        glMatrixMode(GL_PROJECTION)
        glPushMatrix()
        glLoadIdentity()
        glOrtho(0.0, w, h, 0.0, 1000.0, 3000.0)
        glMatrixMode(GL_MODELVIEW)
        glPushMatrix()
        glLoadIdentity()

        // Draw quad
        glBegin(GL_QUADS)
        glTexCoord2f(0f, 1f); glVertex2f(0.0f, 0.0f)
        glTexCoord2f(0f, 0f); glVertex2f(0.0f, h.toFloat())
        glTexCoord2f(1f, 0f); glVertex2f(w.toFloat(), h.toFloat())
        glTexCoord2f(1f, 1f); glVertex2f(w.toFloat(), 0.0f)
        glEnd()

        // Restore GL states
        glMatrixMode(GL_PROJECTION)
        glPopMatrix()
        glMatrixMode(GL_MODELVIEW)
        glPopMatrix()
        glPopMatrix()

        GlStateManager.enableDepth()
        GlStateManager.enableAlpha()
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }
}