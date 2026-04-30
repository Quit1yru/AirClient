/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.pathfinder.CustomPathHelper
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.rotation.AimAssistRotationUtil
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomClickDelay
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11
import java.awt.Color

object FightBot : Module("FightBot", Category.COMBAT) {
    val searchDistance by int("SearchDistance", 128, 64..512)
    val reach by float("Range", 3F, 3F..4.5F)
    val minDist by float("MinDist", 2.8F, 1F..3F)
    val horizontalSpeed by intRange("HorizontalSpeed", 80..120, 4..180)
    val verticalSpeed by intRange("VerticalSpeed", 80..120, 4..180)
    val randomize by float("Randomize", 0.5F, 0F..5F)
    val CPSMode by choices("CPSMode", arrayOf("RNG", "Record1", "Record2", "Record3", "Butterfly", "Jitter", "ConstantMiddle", "ConstGenerate", "ConstGenerate2", "Extra"), "RNG")
    val cps by intRange("CPS", 9..12, 1..20)
    val strafeTick by intRange("StrafeTick(First:LeftTick/Negative|Last:RightTick/Positive)", -2..2, -10..10).onChanged {
        it.first.coerceAtMost(0)
        it.last.coerceAtLeast(0)
    }
    private val pathColorValue by color("PathColor", Color(255, 0, 0, 150))

    val clickTimer = MSTimer()
    var path: ArrayList<Vec3>? = null
    var directionRight = false
    var directionTick = 0
    var next = 0
    var next2 = 9
    var entity: Entity? = null

    override fun onEnable() {
        path=null
    }
    override fun onDisable() {
        mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
        mc.gameSettings.keyBindForward.pressed= GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
        mc.gameSettings.keyBindLeft.pressed= GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)
        mc.gameSettings.keyBindRight.pressed= GameSettings.isKeyDown(mc.gameSettings.keyBindRight)
    }
    private fun renderPath(path: List<Vec3>) {
        val color = pathColorValue

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        GL11.glLineWidth(2.0f)

        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

        for (vec in path) {
            GL11.glVertex3d(
                vec.xCoord - mc.renderManager.viewerPosX,
                vec.yCoord - mc.renderManager.viewerPosY,
                vec.zCoord - mc.renderManager.viewerPosZ
            )
        }
        GL11.glEnd()

        GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f * 0.3f)

        for (vec in path) {
            val x = vec.xCoord - mc.renderManager.viewerPosX
            val y = vec.yCoord - mc.renderManager.viewerPosY
            val z = vec.zCoord - mc.renderManager.viewerPosZ
            val width = 0.3
            val height = 1.8
            RenderUtils.drawBoundingBox(
                x - width, y, z - width,
                x + width, y + height, z + width
            )
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
    }
    val onRender3D = handler<Render3DEvent> {
        if(path!=null&&path!!.isNotEmpty()) {
            renderPath(path!!)
        }
    }
    val onMotion = handler<MotionEvent> {
        mc.thePlayer ?: return@handler
        mc.theWorld ?: return@handler

        if(mc.currentScreen!=null)return@handler

        entity = mc.theWorld.loadedEntityList.filter {
            Backtrack.runWithNearestTrackedDistance(it) {
                isSelected(
                    it,
                    true
                ) && mc.thePlayer.getDistanceToEntityBox(it) <= searchDistance && it is EntityLivingBase
            }
        }.minByOrNull { mc.thePlayer.getDistanceToEntityBox(it) }

        if (entity != null) {
            path = CustomPathHelper.findTeleportPathEntityToEntity(mc.thePlayer, entity, 1.0, true)
            if(path!=null&&path!!.size>1){
                path!!.removeAt(0)

                val rot = AimAssistRotationUtil.face(path!!.first(), horizontalSpeed.random()+nextFloat(-4F, 4F), verticalSpeed.random()+nextFloat(-4F, 4F), mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, randomize)
                mc.thePlayer.rotationYaw = rot[0]

                mc.gameSettings.keyBindLeft.pressed= GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)
                mc.gameSettings.keyBindRight.pressed= GameSettings.isKeyDown(mc.gameSettings.keyBindRight)
                mc.gameSettings.keyBindForward.pressed=true
                mc.gameSettings.keyBindJump.pressed=mc.thePlayer.isCollidedHorizontally||mc.thePlayer.isInWater||mc.thePlayer.isInLava||path!!.first().yCoord-mc.thePlayer.posY>0.62
                if(mc.thePlayer.getDistanceToEntityBox(entity!!)<reach||path!!.size<=2){
                    mc.gameSettings.keyBindForward.pressed=mc.thePlayer.getDistanceToEntity(entity)>minDist
                    mc.gameSettings.keyBindJump.pressed=mc.thePlayer.hurtTime==next2||mc.thePlayer.isCollidedHorizontally||mc.thePlayer.isInWater||mc.thePlayer.isInLava
                    if(mc.thePlayer.hurtTime==next2-1) next2=nextInt(7, 10)
                    directionTick++
                    if(strafeTick.first<0&&strafeTick.last>0) {
                        if(directionRight){
                            mc.gameSettings.keyBindRight.pressed=true
                            mc.gameSettings.keyBindLeft.pressed=false
                            if(directionTick>strafeTick.last) {
                                directionRight = false
                                directionTick=0
                            }
                        }else {
                            mc.gameSettings.keyBindRight.pressed=false
                            mc.gameSettings.keyBindLeft.pressed=true
                            if(directionTick>-strafeTick.first) {
                                directionRight = true
                                directionTick=0
                            }
                        }
                    }

                    val rot = AimAssistRotationUtil.face(entity as EntityLivingBase, horizontalSpeed.random()+nextFloat(-4F, 4F), verticalSpeed.random()+nextFloat(-4F, 4F), mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, false, true, 0F, 0F, 0F, randomize)
                    mc.thePlayer.rotationYaw = rot[0]
                    mc.thePlayer.rotationPitch = rot[1]

                    if(clickTimer.hasTimePassed(next)) {
                        clickTimer.reset()
                        next = randomClickDelay(cps.first, cps.last, CPSMode)
                        mc.clickMouse()
                    }
                }
            }
        }else {
            mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
            mc.gameSettings.keyBindForward.pressed= GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
            mc.gameSettings.keyBindLeft.pressed= GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)
            mc.gameSettings.keyBindRight.pressed= GameSettings.isKeyDown(mc.gameSettings.keyBindRight)
        }
    }
}