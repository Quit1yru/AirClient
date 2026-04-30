package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.RawInputMod
import net.minecraft.client.renderer.GlStateManager

object Fixes : Module("Fixes", Category.COMBAT) {
    private val NoClickDelay by boolean("NoClickDelay", true)
    private val NoRightClickDelay by boolean("NoRightClickDelay",true)
    private val NoBlockHitDelay by boolean("NoBlockHitDelay", false)
    private val RawMouseInput by boolean("RawMouseInput(EnableAgainToApply)", true)

    private val Booster by boolean("Booster", false)
    private val FPSBoost by boolean("FPSBoost", true)
    private val RenderOptimization by boolean("RenderOptimization", true)
    private val EntityOptimization by boolean("EntityOptimization", true)

    val NoJumpDelay by boolean("NoJumpDelay",true)
    @JvmField
    var noJumpDelay = NoJumpDelay

    val onUpdate = handler<UpdateEvent> {
        if (NoClickDelay) mc.leftClickCounter = 0
        if (NoBlockHitDelay) mc.playerController.blockHitDelay = 0
        if (NoRightClickDelay) mc.rightClickDelayTimer = 0

        if (Booster && EntityOptimization) {
            optimizeEntities()
        }
    }

    val onJump = handler<JumpEvent> {
        if (NoJumpDelay) mc.thePlayer.jumpTicks = 0
    }

    val onRender3D = handler<Render3DEvent> {
        if (Booster && RenderOptimization) {
            optimizeRendering()
        }
    }

    val onWorld = handler<WorldEvent> {
        if (Booster && it.worldClient != null && EntityOptimization) {
            clearEntityCache()
        }
    }

    override fun onEnable() {
        if(RawMouseInput) RawInputMod.start()
    }

    override fun onDisable() {
        if(RawMouseInput) RawInputMod.stop()
    }

    private fun optimizeEntities() {
        val world = mc.theWorld ?: return
        val player = mc.thePlayer ?: return

        world.loadedEntityList.forEach { entity ->
            if (entity !== player) {
                val distance = player.getDistanceToEntity(entity)

                if (distance > 64) {
                    entity.isInvisible = true
                } else if (distance > 32) {
                    entity.isInvisible = false
                }
            }
        }
    }

    private fun optimizeRendering() {
        GlStateManager.disableAlpha()
        GlStateManager.enableAlpha()

        if (mc.renderEngine != null) {
            GlStateManager.bindTexture(0)
        }

        if (FPSBoost) {
            mc.entityRenderer.disableLightmap()
            mc.entityRenderer.enableLightmap()
        }
    }

    private fun clearEntityCache() {
        try {
            val world = mc.theWorld ?: return
            val entityList = world.loadedEntityList

            if (entityList.size > 100) {
                world.loadedEntityList.removeAll { entity ->
                    entity.isDead || entity.riddenByEntity == null && entity.ridingEntity == null && entity.ticksExisted > 600
                }
            }
        } catch (_: Exception) {
        }
    }
}