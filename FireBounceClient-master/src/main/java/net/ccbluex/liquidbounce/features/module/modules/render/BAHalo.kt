package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import kotlin.math.sin

object BAHalo : Module("BAHalo", Category.RENDER) {

    private val characterChoices = arrayOf(
        "alice", "shiroko", "reisa", "hoshino", "azusa", "izuna", "kayoko", "shengya","TianYang","A", "A1", "A2", "A3", "A4",
        "B", "B1", "B2", "B3", "B4",
        "C", "C1", "C2", "C3", "C4",
        "D", "D1", "D2", "D3", "D4",
        "E", "E1", "E2", "E3", "E4",
        "F", "F1", "F2", "F3", "F4",
        "G", "G1", "G2", "G3", "G4",
        "H", "H1", "H2", "H3", "H4",
        "I", "I1", "I2", "I3", "I4",
        "J", "J1", "J2", "J3", "J4",
        "K", "K1", "K2", "K3",
        "L", "L1", "L2", "L3",
        "M", "M1", "M2", "M3",
        "N", "N1", "N2", "N3",
        "O", "O1", "O2", "O3",
        "P", "P1", "P2", "P3",
        "Q", "Q1", "Q2", "Q3",
        "R", "R1", "R2", "R3",
        "S", "S1", "S2", "S3",
        "T", "T1", "T2", "T3",
        "U", "U1", "U2", "U3",
        "V", "V1", "V2", "V3",
        "W", "W1", "W2", "W3",
        "X", "X1", "X2", "X3",
        "Y", "Y1", "Y2", "Y3",
        "Z", "Z1", "Z2", "Z3",
        "DLA","DLB","DLC","DLD","DLE","DLF",
    )
    private val character by choices("Character", characterChoices, "alice")
    private val onlySelf by boolean("OnlySelf", true)
    private val trackCamera by boolean("TrackCamera", true)
    private val size by float("Scale", 0.5f, 0.1f..2f)
    private val offsetX by float("OffsetX", 0f, -2f..2f)
    private val offsetY by float("OffsetY", 0.4f, -2f..2f)
    private val offsetZ by float("OffsetZ", 0f, -2f..2f)
    private val floatRange by float("FloatRange", 0.05f, 0f..1f)
    private val floatSpeed by float("FloatSpeed", 0.5f, 0.01f..5f)
    private val flipEnabled by boolean("FlipEnabled", true)
    private val flipX by float("FlipX", 0f, -180f..180f)
    private val flipY by float("FlipY", 5f, -180f..180f)
    private val flipZ by float("FlipZ", 0f, -180f..180f)

    private val texture: ResourceLocation
        get() {
            val name = character.toString()
            return ResourceLocation("liquidbounce/halo/$name.png")
        }

    override val tag: String
        get() = character

    @Suppress("unused")
    val onRender3D = handler<Render3DEvent> { event ->
        val mc = mc
        val world = mc.theWorld ?: return@handler
        val partialTicks = event.partialTicks.toDouble()

        val players = if (!onlySelf) world.playerEntities else listOfNotNull(mc.thePlayer)

        val time = world.totalWorldTime + partialTicks
        val floatY = sin(time * floatSpeed) * floatRange

        try {
            mc.textureManager.bindTexture(texture)
        } catch (t: Throwable) {
            chat("Halo: texture ${texture.resourcePath} not found")
            return@handler
        }

        // 保存 GL 状态并设置渲染状态
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
        GlStateManager.pushMatrix()

        try {
            GlStateManager.disableLighting()
            GlStateManager.disableCull()
            GlStateManager.enableBlend()
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            GlStateManager.depthMask(false)
            GlStateManager.disableDepth()
            GlStateManager.color(1f, 1f, 1f, 1f)

            val renderManager = mc.renderManager

            for (entityPlayer in players) {
                try {
                    if (entityPlayer.isSpectator || !entityPlayer.isEntityAlive) continue

                    val interpX = entityPlayer.lastTickPosX + (entityPlayer.posX - entityPlayer.lastTickPosX) * partialTicks
                    val interpY = entityPlayer.lastTickPosY + (entityPlayer.posY - entityPlayer.lastTickPosY) * partialTicks
                    val interpZ = entityPlayer.lastTickPosZ + (entityPlayer.posZ - entityPlayer.lastTickPosZ) * partialTicks

                    val baseY = entityPlayer.getEyeHeight().toDouble() + offsetY + floatY
                    val hx = interpX + offsetX
                    val hy = interpY + baseY
                    val hz = interpZ + offsetZ

                    GlStateManager.pushMatrix()
                    GlStateManager.translate(
                        (hx - renderManager.viewerPosX).toFloat(),
                        (hy - renderManager.viewerPosY).toFloat(),
                        (hz - renderManager.viewerPosZ).toFloat()
                    )

                    val scale = size.toDouble()
                    GlStateManager.scale(scale.toFloat(), scale.toFloat(), scale.toFloat())

                    val viewY = try { renderManager.playerViewY } catch (_: Throwable) { mc.thePlayer.rotationYawHead }
                    val viewX = try { renderManager.playerViewX } catch (_: Throwable) {
                        mc.thePlayer.rotationPitch
                    }

                    GlStateManager.rotate(-viewY, 0f, 1f, 0f)
                    if (trackCamera) {
                        GlStateManager.rotate(viewX, 1f, 0f, 0f)
                    }

                    if (flipEnabled) {
                        GlStateManager.rotate(flipX, 1f, 0f, 0f)
                        GlStateManager.rotate(flipY, 0f, 1f, 0f)
                        GlStateManager.rotate(flipZ, 0f, 0f, 1f)
                    }

                    val half = 0.5f

                    val tess = Tessellator.getInstance()
                    val wr = try {
                        tess.worldRenderer
                    } catch (t: Throwable) {
                        try {
                            tess.worldRenderer
                        } catch (t2: Throwable) {
                            null
                        }
                    }

                    if (wr != null) {
                        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR)
                        // v1
                        wr.pos((-half).toDouble(), (-half).toDouble(), 0.0).tex(0.0, 1.0).color(1f, 1f, 1f, 1f).endVertex()
                        // v2
                        wr.pos(half.toDouble(), (-half).toDouble(), 0.0).tex(1.0, 1.0).color(1f, 1f, 1f, 1f).endVertex()
                        // v3
                        wr.pos(half.toDouble(), half.toDouble(), 0.0).tex(1.0, 0.0).color(1f, 1f, 1f, 1f).endVertex()
                        // v4
                        wr.pos((-half).toDouble(), half.toDouble(), 0.0).tex(0.0, 0.0).color(1f, 1f, 1f, 1f).endVertex()
                        tess.draw()
                    } else {
                        try {
                            val beginM = Tessellator.getInstance().javaClass.getMethod("getWorldRenderer")
                            val worldRenderer = beginM.invoke(Tessellator.getInstance())
                            val cls = worldRenderer.javaClass
                            val begin = cls.getMethod("begin", Int::class.javaPrimitiveType, DefaultVertexFormats::class.java)
                            begin.invoke(worldRenderer, GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR)
                            val pos = cls.getMethod("pos", Double::class.javaPrimitiveType, Double::class.javaPrimitiveType, Double::class.javaPrimitiveType)
                            val tex = cls.getMethod("tex", Double::class.javaPrimitiveType, Double::class.javaPrimitiveType)
                            val colorM = cls.getMethod("color", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
                            val endV = cls.getMethod("endVertex")
                            pos.invoke(worldRenderer, (-half).toDouble(), (-half).toDouble(), 0.0)
                            tex.invoke(worldRenderer, 0.0, 1.0)
                            colorM.invoke(worldRenderer, 1f, 1f, 1f, 1f)
                            endV.invoke(worldRenderer)
                            pos.invoke(worldRenderer, half.toDouble(), (-half).toDouble(), 0.0)
                            tex.invoke(worldRenderer, 1.0, 1.0)
                            colorM.invoke(worldRenderer, 1f, 1f, 1f, 1f)
                            endV.invoke(worldRenderer)
                            pos.invoke(worldRenderer, half.toDouble(), half.toDouble(), 0.0)
                            tex.invoke(worldRenderer, 1.0, 0.0)
                            colorM.invoke(worldRenderer, 1f, 1f, 1f, 1f)
                            endV.invoke(worldRenderer)
                            pos.invoke(worldRenderer, (-half).toDouble(), half.toDouble(), 0.0)
                            tex.invoke(worldRenderer, 0.0, 0.0)
                            colorM.invoke(worldRenderer, 1f, 1f, 1f, 1f)
                            endV.invoke(worldRenderer)
                            val drawM = Tessellator.getInstance().javaClass.getMethod("draw")
                            drawM.invoke(Tessellator.getInstance())
                        } catch (_: Throwable) {

                        }
                    }

                    GlStateManager.popMatrix()
                } catch (_: Throwable) {

                }
            }
        } finally {
            try { GlStateManager.color(1f, 1f, 1f, 1f) } catch (_: Throwable) {}
            try {
                GlStateManager.enableDepth()
                GlStateManager.depthMask(true)
                GlStateManager.disableBlend()
                GlStateManager.enableCull()
                GlStateManager.enableLighting()
            } catch (_: Throwable) {}
            GlStateManager.popMatrix()
            GL11.glPopAttrib()
        }
    }
}
