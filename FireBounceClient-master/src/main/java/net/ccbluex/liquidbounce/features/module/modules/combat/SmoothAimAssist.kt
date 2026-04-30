/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.rotation.AimAssistRotationUtil
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.EntityLivingBase

object SmoothAimAssist : Module("SmoothAimAssist", Category.COMBAT) {

    private val range by float("Range", 4.4F, 1F..8F)
    private val horizontalAim by boolean("HorizontalAim", true)
    private val verticalAim by boolean("VerticalAim", true)
    private val horizontalSpeed by int("HorizontalSpeed", 180, 1..180)
    private val verticalSpeed by int("VerticalSpeed", 180, 1..180)
    private val entropyMax by float("EntropyDisturbMax", 1F, 0F..10F)
    private val entropyMin by float("EntropyDisturbMin", 0.5F, 0F..10F)
    private val entropyFactor by float("EntropyFactor", 0.5F, 0F..10F)
    private val randomize by float("Randomize", 0.5F, 0F..5F)
    private val heuristic by boolean("Heuristic", true)


    private val fov by float("FOV", 180F, 1F..180F)
    private val onClick by boolean("OnClick", false) { horizontalAim || verticalAim }
    private val breakBlocks by boolean("BreakBlocks", true)

    private val clickTimer = MSTimer()

    val onMotion = handler<MotionEvent> { event ->
        if (event.eventState != EventState.POST) return@handler
        val player = mc.thePlayer ?: return@handler
        val world = mc.theWorld ?: return@handler
        if (mc.gameSettings.keyBindAttack.isKeyDown) clickTimer.reset()
        if (onClick && (clickTimer.hasTimePassed(150) || !mc.gameSettings.keyBindAttack.isKeyDown && AutoClicker.handleEvents())) return@handler
        val entity = world.loadedEntityList.filter {
            Backtrack.runWithNearestTrackedDistance(it) {
                isSelected(
                    it,
                    true
                ) && player.canEntityBeSeen(it) && player.getDistanceToEntityBox(it) <= range && rotationDifference(it) <= fov
            }
        }.minByOrNull { player.getDistanceToEntityBox(it) } ?: return@handler

        if (mc.playerController.isHittingBlock && breakBlocks) {
            return@handler
        }
        val rotation = AimAssistRotationUtil.face(entity as EntityLivingBase, horizontalSpeed.toFloat()+Math.random().toFloat(), verticalSpeed.toFloat()+Math.random().toFloat(), mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, heuristic, true, entropyMax, entropyMin, entropyFactor, randomize)
        if (rotation != null) {
            mc.thePlayer.rotationYaw = rotation[0]
            mc.thePlayer.rotationPitch = rotation[1]
        }
    }
}