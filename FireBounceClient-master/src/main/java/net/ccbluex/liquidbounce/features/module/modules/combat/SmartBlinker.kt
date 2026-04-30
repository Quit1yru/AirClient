package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.isHurting
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.S14PacketEntity
import net.minecraft.util.Vec3

object SmartBlinker : Module("SmartBlinker", Category.COMBAT) {
    private val tagMode by choices("TagMode", arrayOf("Normal", "MaxTime", "Custom", "PacketCount"), "Normal")
    private val customTag by text("CustomTag", "")
    private val range by floatRange("Range", 2f..4f, 0f..6f)
    private val limitMode by multiChoices("LimitMode", arrayOf("BlinkTime", "MoveRange"), arrayOf("BlinkTime"))
    private val maxBlinkTime by int("MaxBlinkTime", 500, 0..5000, "ms") { "BlinkTime" in limitMode }
    private val maxMoveRangePerBlink by float("MaxMoveRangePerBlink", 5f, 0f..50f, "blocks") { "MoveRange" in limitMode }
    private val minDelayBetweenCancelBlink by intRange("MinDelayBetweenPerCancelBlink", 0..0, 0..5000, "ms")
    private val delay by intRange("Delay", 500..1000, 0..5000, "ms")
    private val stopOnAttack by boolean("StopOnAttack", true)
    private val stopOnPlaceBlock by boolean("StopOnPlaceBlock", false)
    private val stopOnHurt by boolean("StopOnHurt", false)
    private val stopOnLag by boolean("StopOnServerTP", true)
    private val blockAllPackets by boolean("BlockAllPackets", false)
    private val tips by boolean("Tips", true)
    private val debugger by boolean("Debugger", false)

    private val delayTimer = MSTimer()
    private val delayTimer2 = MSTimer()
    private var bufferTarget: EntityLivingBase? = null
    private var isBlinking = false
    private var lastBlinkState = false
    private var blinkStartTime = 0L
    private var lastPlayerPos: Vec3? = null
    private var totalMoveDistance = 0f

    private var actualDelay = 0

    val onGameLoop = handler<GameLoopEvent> {
        if (!isBlinking) actualDelay = delay.random()

        if (KillAura.target != null) {
            bufferTarget = KillAura.target as EntityLivingBase
        }
        if (isBlinking && Blink.handleEvents()) {
            Blink.toggle()
            chat("Don't enable your Blink Module When This Module Is Working!")
        }

        if (isBlinking && "MoveRange" in limitMode) {
            val currentPos = Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)

            if (lastPlayerPos != null) {
                val segmentDistance = lastPlayerPos!!.distanceTo(currentPos).toFloat()
                totalMoveDistance += segmentDistance

                if (totalMoveDistance >= maxMoveRangePerBlink) {
                    debugMessage("MaxMoveRange reached (${"%.3f".format(totalMoveDistance)} blocks), stopping...")
                    reset()
                    return@handler
                }
            }
            lastPlayerPos = currentPos
        }

        if (isBlinking && blinkStartTime > 0 &&
            "BlinkTime" in limitMode &&
            System.currentTimeMillis() - blinkStartTime >= maxBlinkTime
        ) {
            debugMessage("MaxBlinkTime reached, stopping... Duration: ${System.currentTimeMillis() - blinkStartTime}ms")
            reset()
        }
    }

    private val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (
            (
                    (stopOnAttack && packet.isAttackPacket) ||
                            (stopOnPlaceBlock && packet.isPlaceBlockPacket) ||
                            (stopOnLag && packet.isServerLagPacket)
                    )
            && isBlinking
        ) {
            debugMessage("StopWorking, DuringTime:${System.currentTimeMillis() - blinkStartTime}ms, MoveDistance:${"%.3f".format(totalMoveDistance)} blocks")
            reset()
            return@handler
        }

        if (shouldBlink()) {
            if (!isBlinking) {
                val startPos = Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
                lastPlayerPos = startPos
                totalMoveDistance = 0f
                debugMessage("Blink started at position: (${"%.2f".format(startPos.xCoord)}, ${"%.2f".format(startPos.yCoord)}, ${"%.2f".format(startPos.zCoord)})")
            }

            BlinkUtils.blink(packet, event, receive = blockAllPackets, receiveFilter = { p -> p is S14PacketEntity })
            if (!lastBlinkState) {
                debugMessage("StartWorking")
                blinkStartTime = System.currentTimeMillis()
            }
            isBlinking = true
            lastBlinkState = true
        } else if (!shouldBlink() && isBlinking) {
            val duration = System.currentTimeMillis() - blinkStartTime
            debugMessage("StopWorking, DuringTime:${duration}ms, TotalMoveDistance:${"%.3f".format(totalMoveDistance)} blocks")
            reset()
        }
    }

    private fun shouldBlink(): Boolean {
        val killAuraIsWorking = KillAura.state && KillAura.handleEvents() && KillAura.target != null
        if (stopOnHurt && mc.thePlayer.isHurting()) return false
        if (!killAuraIsWorking) return false
        if (bufferTarget == null) return false
        if (!delayTimer.hasTimePassed(actualDelay)) return false

        val distance = mc.thePlayer.getDistanceToEntityBox(bufferTarget as EntityLivingBase)
        val withinRange = distance in range

        if (isBlinking) {
            var continueBlink = withinRange

            if ("BlinkTime" in limitMode) {
                continueBlink = continueBlink && blinkStartTime > 0 &&
                        System.currentTimeMillis() - blinkStartTime < maxBlinkTime
            }

            if ("MoveRange" in limitMode) {
                continueBlink = continueBlink && totalMoveDistance < maxMoveRangePerBlink
            }

            return continueBlink
        }

        return withinRange
    }

    private fun debugMessage(msg: Any) {
        if (debugger) chat(msg.toString())
    }

    private fun reset() {
        if (!delayTimer2.hasTimePassed(minDelayBetweenCancelBlink.random())) return
        delayTimer.reset()
        delayTimer2.reset()
        BlinkUtils.unblink()
        isBlinking = false
        blinkStartTime = 0L
        lastBlinkState = false
        lastPlayerPos = null
        totalMoveDistance = 0f
    }

    override fun onEnable() {
        if (tips) chat("If you open this module, when module is working, the blink module will be automatically disabled")
    }

    override val tag: String?
        get() = when (tagMode) {
            "Normal" -> "${range.start} - ${range.endInclusive}"
            "MaxTime" -> "${maxBlinkTime}ms"
            "Custom" -> customTag
            "PacketCount" -> if (isBlinking) BlinkUtils.packets.size else "0"
            else -> null
        } as String?
}