package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.font.Fonts.minecraftFont
import net.ccbluex.liquidbounce.utils.extensions.safeDiv
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.render.ShootTask
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.MovingObjectPosition
import java.awt.Color
import javax.vecmath.Vector2f
import kotlin.math.min

object AutoLuGuan : Module("AutoLuGuan", Category.FUN) {

    val particleType by choices("ParticleType", arrayOf("Water", "Flame", "Slime", "Snowball"), "Flame")
    val motionXZ by float("MotionXZ", 1.0F, 0.1F..4F)
    val motionY by float("MotionY", 1.0F, 0.1F..4F)
    val motionDecay by float("MotionDecay", 0.98F, 0.1F..1F)
    val motionAccel by float("MotionAccel", 0.08F, 0.01F..0.5F)
    val maxDelta by float("ParticleDelta", 0.1F, 0.01F..1F)
    val delay by float("Delay", 1000F, 0F..10000F)
    val swingNeeded by intRange("SwingToTrigger", 8..12, 0..40)
    val startPointOffset by float("StartPointOffset", 1.25F, 0F..2F)
    val verticalAngleOffset by float("VerticalAngleOffset", 22F, 0F..45F)
    val reverseAngleDirection by boolean("ReverseAngleDirection", true)
    val withGravity by boolean("WithGravity", true)
    val shakeCamera by boolean("ShakeCamera", true)
    val shakeFrequency by int("ShakeFrequency", 4, 1..10)
    val progress by boolean("Progress", true)

    var swingCount = 0
    var nextSwing = 0
    val activeTasks = mutableListOf<ShootTask>()
    private val cooldownTimer = MSTimer()
    private val sinceShoot = MSTimer()

    val onPacket = handler<PacketEvent> { event ->
        if (shouldTriggerShoot(event)) {
            triggerShoot()
        }
    }

    val onUpdate = handler<UpdateEvent> { _ ->
        if(activeTasks.isNotEmpty()) cleanupCompletedTasks()
        if(mc.thePlayer.ticksExisted%10==0&&swingCount>0) swingCount -= 1
    }

    val onRender2D = handler<Render2DEvent> {
        if(mc.thePlayer==null)return@handler
        val sc = ScaledResolution(mc)
        val since = !sinceShoot.hasTimePassed(min(delay.toLong(), 3000L))
        if (progress&&(swingCount>0||since)) {
            var textToRender = ""
            textToRender = if(since){
                if(shakeCamera&&mc.thePlayer.ticksExisted%shakeFrequency==0){
                    mc.thePlayer.rotationYaw+=nextFloat(-5F, 5F)
                    mc.thePlayer.rotationPitch+=nextFloat(-5F, 5F)
                }
                (when(mc.thePlayer.ticksExisted%7){
                    0->EnumChatFormatting.RED
                    1->EnumChatFormatting.DARK_RED
                    2->EnumChatFormatting.GOLD
                    3->EnumChatFormatting.YELLOW
                    4->EnumChatFormatting.GREEN
                    5->EnumChatFormatting.DARK_GREEN
                    6->EnumChatFormatting.AQUA
                    else->EnumChatFormatting.BLUE
                }).toString() + "You ejaculated!!!"
            }else{
                "Ejaculation progress: ${((swingCount.toFloat() safeDiv nextSwing.toFloat())*100F).toInt()}%"
            }
            minecraftFont.drawString(
                textToRender,
                sc.scaledWidth / 2F - minecraftFont.getStringWidth(textToRender) / 2F,
                sc.scaledHeight / 2F + 10F,
                Color(255, 255, 255).rgb,
                true
            )
        }
    }
    private fun shouldTriggerShoot(event: PacketEvent): Boolean {
        if(mc.thePlayer.isSneaking && event.packet is C0APacketAnimation && mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK){
            swingCount+=1
            if(swingCount <= nextSwing) return false
            else{
                nextSwing = swingNeeded.random()
                return cooldownTimer.hasTimePassed(delay)
            }
        }
        return false
    }

    private fun triggerShoot() {
        swingCount = 0
        sinceShoot.reset()
        cooldownTimer.reset()
        val player = mc.thePlayer?:return
        val spawnPosition = player.positionVector.addVector(0.0, startPointOffset.toDouble(), 0.0)

        val shootTask = ShootTask(
            particleType = particleType,
            motionX = motionXZ,
            motionZ = motionXZ,
            motionY = motionY,
            yaw = player.rotationYaw + nextFloat(-1.5F, 1.6F),
            pitch = player.rotationPitch + nextFloat(-1.5F, 1.6F),
            position = spawnPosition,
            maxDelta = maxDelta,
            verticalAngleOffset = verticalAngleOffset * (if (reverseAngleDirection) -1 else 1),
            withGravity = withGravity,
            motionData = Vector2f(motionAccel, motionDecay)
        ).redirect()

        activeTasks.add(shootTask)
    }

    private fun cleanupCompletedTasks() {
        activeTasks.removeIf { !it.tick() }
    }
}