/*
 * Eclipse Hacked Client
 * A mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura.target
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.findItem
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.skid.eclipse.EasyUtils.eyePoint3
import net.ccbluex.liquidbounce.utils.skid.eclipse.EasyUtils.getPitch
import net.ccbluex.liquidbounce.utils.skid.eclipse.EasyUtils.getYaw
import net.ccbluex.liquidbounce.utils.skid.eclipse.EasyUtils.rangeFrom
import net.ccbluex.liquidbounce.utils.skid.eclipse.EasyUtils.swapItem
import net.ccbluex.liquidbounce.utils.skid.eclipse.Point
import net.minecraft.client.settings.KeyBinding.setKeyBindState
import net.minecraft.init.Items.fishing_rod
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt

object AutoRod2 : Module("AutoRod2", Category.COMBAT) {

    /**
     * options
     */
    // Basic Options
    private val rodMode by choices("RodMode", arrayOf("Legit", "Packet", "NewPacket"), "Legit")
    val maxRange by float("MaxRange", 6f, 0f..16f)
    private val maxDelayValue : Value<Int> = int("MaxDelay",200,0..1000).onChange { _, new ->
        new.coerceAtLeast(minDelay.get())
    }
    private val minDelay: Value<Int> = int("MinDelay", 100, 0..1000).onChange { _, new ->
        new.coerceAtMost(maxDelayValue.get())
    }
    // Smart Rod
    private val smartDelay by boolean("SmartDelay", false)
    private val smartRodTiming by boolean("SmartRodTiming", false) { smartDelay }

    private val perfectTiming by boolean("PerfectTiming", false)
    private val perfectHurtTime by int("PerfectHurtTime", 9, 0..10) { perfectTiming }

    private val predictMode by choices("PredictMode", arrayOf("Custom", "ExperimentalFitting"), "Custom")
    private val predictSize by float("PredictSize", 3.5f, 0f..10f) { predictMode == "Custom" }


    /**
     * variables
     */
    private var currentItem = -1
    private var itemStack: ItemStack? = null
    var resetting = false
    private var pauseTick = 0
    private var rodActionState = false
    private var itemState = false
    private var hasThrownRod = false


    /**
     * misc
     */
    override val tag
        get() = rodMode

    override fun onDisable() {
        reset()
    }


    /**
     * tick event
     */

    val onTick = handler<GameTickEvent> {
        val player = mc.thePlayer
        val rod = getRod()
        val lastCurrentItem = currentItem
        val lastItemStack = itemStack

        if (cancelRun()) {
            resetting = true
            when (rodMode) {
                "Legit" -> {
                    if (rodActionState) setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
                    if (itemState) swapItem(lastCurrentItem)
                }
                "Packet" -> {
                    if (rodActionState) sendPacket(C08PacketPlayerBlockPlacement(lastItemStack))
                    if (itemState) sendPacket(C09PacketHeldItemChange(lastCurrentItem))
                }
                "NewPacket" -> {
                    if (itemState) sendPacket(C09PacketHeldItemChange(lastCurrentItem))
                }
            }
            resetting = false

            reset()

            return@handler
        }

        currentItem = player.inventory.currentItem
        itemStack = player.inventoryContainer.getSlot(rod).stack

        val shouldPullRod = perfectTiming && target != null && target!!.hurtTime == perfectHurtTime

        when (rodMode) {
            "Legit" -> {
                if (perfectTiming) {
                    if (!hasThrownRod && shouldThrowRod()) {
                        swapItem(rod - 36)
                        itemState = true
                        setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, true)
                        rodActionState = true
                        hasThrownRod = true
                    }

                    if (hasThrownRod && (shouldPullRod || cancelRun())) {
                        setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
                        rodActionState = false
                        swapItem(currentItem)
                        itemState = true
                        hasThrownRod = false
                        pauseTick = 0
                    }
                } else {
                    when (++pauseTick) {
                        1 -> {
                            swapItem(rod - 36)
                            itemState = true
                            setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, true)
                            rodActionState = true
                        }
                    }
                    if (pauseTick >= tickDelay) {
                        setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
                        rodActionState = false
                        swapItem(currentItem)
                        itemState = true
                        pauseTick = 0
                    }
                }
            }
            "Packet" -> {
                if (perfectTiming) {
                    if (!hasThrownRod && shouldThrowRod()) {
                        sendPacket(C09PacketHeldItemChange(rod - 36))
                        itemState = true
                        sendPacket(C08PacketPlayerBlockPlacement(itemStack))
                        rodActionState = true
                        hasThrownRod = true
                    }

                    if (hasThrownRod && (shouldPullRod || cancelRun())) {
                        sendPacket(C08PacketPlayerBlockPlacement(itemStack))
                        rodActionState = false
                        sendPacket(C09PacketHeldItemChange(currentItem))
                        itemState = false
                        hasThrownRod = false
                        pauseTick = 0
                    }
                } else {
                    when (++pauseTick) {
                        1 -> {
                            sendPacket(C09PacketHeldItemChange(rod - 36))
                            itemState = true
                            sendPacket(C08PacketPlayerBlockPlacement(itemStack))
                            rodActionState = true
                        }
                    }
                    if (pauseTick >= tickDelay) {
                        sendPacket(C08PacketPlayerBlockPlacement(itemStack))
                        rodActionState = false
                        sendPacket(C09PacketHeldItemChange(currentItem))
                        itemState = false
                        pauseTick = 0
                    }
                }
            }
            "NewPacket" -> {
                if (perfectTiming) {
                    if (!hasThrownRod && shouldThrowRod()) {
                        sendPacket(C09PacketHeldItemChange(rod - 36))
                        itemState = true
                        sendPacket(C08PacketPlayerBlockPlacement(itemStack))
                        hasThrownRod = true
                    }

                    if (hasThrownRod && (shouldPullRod || cancelRun())) {
                        sendPacket(C09PacketHeldItemChange(currentItem))
                        itemState = false
                        hasThrownRod = false
                        pauseTick = 0
                    }
                } else {
                    when (++pauseTick) {
                        1 -> {
                            sendPacket(C09PacketHeldItemChange(rod - 36))
                            itemState = true
                            sendPacket(C08PacketPlayerBlockPlacement(itemStack))
                        }
                    }
                    if (pauseTick >= tickDelay) {
                        sendPacket(C09PacketHeldItemChange(currentItem))
                        itemState = false
                        pauseTick = 0
                    }
                }
            }
        }
    }


    /**
     * others
     */
    private fun predictedPoint(): Point {
        when (predictMode) {
            "Custom" -> {
                val motionX = target?.posX!! - target?.prevPosX!!
                val motionZ = target?.posZ!! - target?.prevPosZ!!
                return Point(
                    x = motionX * predictSize,
                    z = motionZ * predictSize
                )
            }
            "ExperimentalFitting" -> {
                val motionX = target?.posX!! - target?.prevPosX!!
                val motionZ = target?.posZ!! - target?.prevPosZ!!
                val bpsX = (target?.posX!! - target?.prevPosX!!) * 20
                val bpsZ = (target?.posZ!! - target?.prevPosZ!!) * 20

                fun f(x: Double) = (
                        0.00428696 * x.pow(5) +
                                -0.1235 * x.pow(4) +
                                1.32092 * x.pow(3) +
                                -6.35726 * x.pow(2) +
                                12.732 * x
                        )

                val fittedX = motionX * f(bpsX).rangeFrom(f(1.0), f(9.8))
                val fittedZ = motionZ * f(bpsZ).rangeFrom(f(1.0), f(9.8))

                return Point(
                    x = fittedX,
                    z = fittedZ
                )
            }
            else -> return Point(0.0, 0.0)
        }
    }

    val rotation
        get() = Rotation(
            yaw = Point(
                x = target?.posX!! + predictedPoint().x,
                z = target?.posZ!! + predictedPoint().z
            ).getYaw(),
            pitch = target?.eyePoint3?.getPitch()!!
        )

    private val distance
        get() = (
                if (mc.thePlayer == null || target == null) 0.0
                else mc.thePlayer.getDistanceToEntityBox(target!!)
                )

    private val delay
        get() = (
                if (smartDelay) {
                    val calculatedValue = (1880f / (1f + (18.71f * 2.7182818285.pow(-0.2076f * distance))) / 100f).roundToInt() * 100f
                    calculatedValue.coerceIn(200f, 650f).roundToInt()
                } else {
                    (minDelay.get()..maxDelayValue.get()).random()
                }
                )
    private val tickDelay
        get() = ceil(delay / 50f).toInt()

    private val rodTiming
        get() = target?.hurtTime!! <= 3 + tickDelay

    private val isInRodRange
        get() = distance > KillAura.range && distance <= maxRange

    private fun reset() {
        pauseTick = 0
        rodActionState = false
        itemState = false
        hasThrownRod = false
    }

    private fun getRod(): Int {
        return findItem(36, 45, fishing_rod)?: -1
    }

    private fun cancelRun(): Boolean {
        if (!KillAura.handleEvents() || target == null) {
            return true
        }

        if (getRod() == -1) {
            return true
        }

        if (!isInRodRange) {
            return true
        }

        if (smartDelay && smartRodTiming && !rodTiming) {
            return true
        }

        return false
    }

    private fun shouldThrowRod(): Boolean {
        if (cancelRun()) {
            return false
        }

        if (perfectTiming && target != null) {
            return target!!.hurtTime == 9
        }

        return true
    }

}
fun Float.pow(exponent: Float): Float = this.toDouble().pow(exponent.toDouble()).toFloat()
fun Double.pow(exponent: Float): Double = this.pow(exponent.toDouble())
fun Float.pow(exponent: Double): Float = this.toDouble().pow(exponent).toFloat()