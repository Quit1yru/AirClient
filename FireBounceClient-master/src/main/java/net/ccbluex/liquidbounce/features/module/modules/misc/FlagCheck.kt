/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.font.Fonts.minecraftFont
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extra.MovementUtils
import net.ccbluex.liquidbounce.utils.math.AimAnalysisEngine
import net.ccbluex.liquidbounce.utils.math.ComplexMath
import net.ccbluex.liquidbounce.utils.math.FastMathUtil
import net.ccbluex.liquidbounce.utils.math.Vec2f
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.Vec3
import java.awt.Color
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object FlagCheck : Module("FlagCheck", Category.MISC, gameDetecting = true) {
    val checkType by multiChoices("CheckType", arrayOf("Packet", "Health", "Hunger", "GhostBlock", "Scaffold","AutoClick","Window", "LongTermAimAnalysis", "ShortTermAimAnalysis"), arrayOf("Packet"))
    fun sphericalDistance(angle1: Vec2f, angle2: Vec2f): Float {
        val theta1 = FastMathUtil.toAngleRadian(angle1.x)
        val phi1 = FastMathUtil.toAngleRadian(angle1.y)
        val theta2 = FastMathUtil.toAngleRadian(angle2.x)
        val phi2 = FastMathUtil.toAngleRadian(angle2.y)

        val cosGamma = FastMathUtil.cos(theta1) * FastMathUtil.cos(theta2) + FastMathUtil.sin(theta1) * FastMathUtil.sin(theta2) * FastMathUtil.cos(phi1 - phi2)
        val gamma = FastMathUtil.arcCos(cosGamma)
        return 2f * FastMathUtil.sin(gamma / 2f)
    }
    fun distance3d(a: Vec3, b: Vec3): Float{
        return sqrt(
            (b.xCoord - a.xCoord).pow(2.0) + (b.yCoord - a.yCoord).pow(2.0) + (b.zCoord - a.zCoord).pow(2.0)
        ).toFloat()
    }
    val autoQuit by boolean("AutoQuit", false)
    val autoQuitVL by int("AutoQuitVL", 500, 50..5000)
    val VLReset by int("VLResetSecond", 300, 5..3600)

    val tip by boolean("ShowLagTimePassed", true)

    var VL = 0
    val FiveSecondTimer = MSTimer()
    val longTermData = ArrayList<FloatArray>()
    var shortTermData = Pair(Vec2f(0f, -1337f), Vec2f(0f,-1337f))
    val clickWindowHistory = ArrayList<Long>()
    val clickWindowTimer = MSTimer()
    val scaffoldHistory = ArrayList<Long>()
    val scaffoldTimer = MSTimer()
    var emptyClickCount = 0
    var lastRot = Vec2f(0f,0f)

    const val ON_GROUND_CONST = 1/64.0
    fun reset(){
        lastRot = Vec2f(0f, 0f)
        VL = 0
        longTermData.clear()
        shortTermData= Pair(Vec2f(0f, -1337f), Vec2f(0f,-1337f))
        clickWindowTimer.reset()
        clickWindowHistory.clear()
        emptyClickCount = 0
        scaffoldHistory.clear()
        scaffoldTimer.reset()
    }
    override fun onEnable() {
        reset()
    }
    val onWorld = handler<WorldEvent>{
        reset()
    }
    val onMotion = handler<MotionEvent>{
        mc.thePlayer?:return@handler
        if(mc.thePlayer.ticksExisted%(VLReset*20)==0) VL=0
        if(autoQuit){
            if(VL>=autoQuitVL) {
                VL=0
                mc.theWorld.sendQuittingDisconnectingPacket()
            }
        }
    }
    val onPacket = handler < PacketEvent>{ event ->
        mc.thePlayer?:return@handler
        when(val packet = event.packet){
            is C08PacketPlayerBlockPlacement -> {
                if(checkType.contains("Scaffold")){
                    if (MovementUtils.getSpeed(mc.thePlayer) > 0.1) scaffoldHistory.add(scaffoldTimer.getTimePassed())
                    scaffoldTimer.reset()
                    if (scaffoldHistory.size >= 10) {
                        val stdDev = ComplexMath.getStandardDeviation(scaffoldHistory)
                        scaffoldHistory.clear()
                        if(stdDev<20){
                            doFlag("Scaffold", "[StdDev=${stdDev}")
                        }
                    }
                }
            }
            is C0EPacketClickWindow -> {
                if(checkType.contains("Window")){
                    if(clickWindowTimer.hasTimePassed(3000L)){
                        clickWindowHistory.clear()
                        clickWindowTimer.reset()
                        emptyClickCount=0
                    }
                    if(packet.clickedItem==null) emptyClickCount++
                    clickWindowHistory.add(clickWindowTimer.getTimePassed())
                    clickWindowTimer.reset()
                    if(clickWindowHistory.size>=6){
                        val avg = (ComplexMath.getAverage(clickWindowHistory))
                        val stdev: Double = ComplexMath.getStandardDeviation(clickWindowHistory)
                        clickWindowHistory.clear()
                        val t = emptyClickCount
                        emptyClickCount = 0
                        if(stdev < 15.0 || avg <= 80 || (avg < 170 && t == 0)){
                            doFlag("Window", "[Average=${avg}, StdDev=${stdev}]")
                        }
                    }
                }
            }
            is S08PacketPlayerPosLook -> {
                if(checkType.contains("Packet")){
                    FiveSecondTimer.reset()
                    doFlag("LagBack", "[ΔPos=${String.format("%.3f", distance3d(Vec3(packet.x, packet.y, packet.z), mc.thePlayer.positionVector))}, ΔAngle=${String.format("%.3f", sphericalDistance(Vec2f(packet.yaw, packet.pitch), Vec2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)))}]")
                }
            }
            is C03PacketPlayer -> {
                //packet.yaw,packet.pitch: now
                //pair.first: 1tick before
                //pair.second: 2tick before
                if(checkType.contains("ShortTermAimAnalysis")){
                    if(shortTermData.first.y!=-1337f&&shortTermData.second.y!=-1337f){
                        if(AimAnalysisEngine.shortTermAnalysis(Vec2f(packet.yaw, packet.pitch), shortTermData.first, Vec2f(shortTermData.first.x-shortTermData.second.x, shortTermData.first.y-shortTermData.second.y))){
                            doFlag("SAim", "Distance=${String.format("%.3f", sphericalDistance(Vec2f(packet.yaw, packet.pitch), shortTermData.first))}||${String.format("%.3f", sphericalDistance(shortTermData.first, shortTermData.second))}")
                        }
                    }
                }
                if(checkType.contains("LongTermAimAnalysis")){
                    longTermData.add(FloatArray(2){packet.yaw; packet.pitch})
                    if(longTermData.size>=40) {
                        val result = AimAnalysisEngine.aimAnalysisEngineInstance.analyze(longTermData)
                        longTermData.clear()
                        result.forEach { if(it!=null){
                            doFlag("LAim-${it.result.name}", it.description)
                        } }
                    }
                }
                shortTermData = Pair(Vec2f(abs(packet.yaw-lastRot.x), abs(packet.pitch-lastRot.y)), shortTermData.first)
                lastRot = Vec2f(packet.yaw, packet.pitch)
            }
        }
    }
    val onRender2D = handler<Render2DEvent> {
        val sc = ScaledResolution(mc)
        if(tip&&!FiveSecondTimer.hasTimePassed(5000L)){
            val text = EnumChatFormatting.YELLOW.toString()+"Since last lagBack: ${String.format("%.1f", FiveSecondTimer.getTimePassed()/1000.0f)}"
            minecraftFont.drawString(
                text,
                sc.scaledWidth / 2F - minecraftFont.getStringWidth(text) / 2F,
                sc.scaledHeight / 2F + 140F,
                Color(255, 255, 255).rgb,
                true
            )
        }
    }
    val onUpdate = handler<UpdateEvent> {
        mc.thePlayer?:return@handler

        checkType.forEach {
            when(it){
                "GhostBlock" ->
                    if(mc.thePlayer.onGround != (mc.thePlayer.posY % ON_GROUND_CONST == 0.0)) {
                        doFlag("GhostBlock", "[Y=${mc.thePlayer.posY}, Client=${mc.thePlayer.onGround}, Server=${mc.thePlayer.posY % ON_GROUND_CONST == 0.0}]")
                    }
                "Health" ->
                    if(mc.thePlayer.health < 0f) {
                        doFlag("HealthExploit", "[Health=${mc.thePlayer.health}]")
                    }
                "Hunger" ->
                    if(mc.thePlayer.foodStats.foodLevel < 0f) {
                        doFlag("HungerExploit", "[Hunger=${mc.thePlayer.foodStats.foodLevel}]")
                    }
            }
        }
    }
    fun doFlag(reason: String, detail: String){
        VL++
        chat("Detected: ${VL}x"+ EnumChatFormatting.LIGHT_PURPLE + reason+ EnumChatFormatting.YELLOW + "[$detail]")
    }
}