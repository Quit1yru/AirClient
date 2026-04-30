/*
 * Eclipse Hacked Client
 * A mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.utils.skid.eclipse

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.utils.attack.CPSCounter
import net.ccbluex.liquidbounce.utils.attack.CPSCounter.registerClick
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.attackEntityWithModifiedSprint
import net.ccbluex.liquidbounce.utils.extensions.rotation
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.mc
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextDouble
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.getRotationDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.serverRotation
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.potion.Potion
import net.minecraft.util.IChatComponent
import net.minecraft.util.MathHelper
import kotlin.math.*

object EasyUtils { // foolish, yet simple utils for sluggard

    var pi = 3.1415927f

    fun Double.toRad() = this * 0.017453292
    fun Float.toRad() = this * 0.017453292f

    fun Double.toDeg() = this * 57.295779513
    fun Float.toDeg() = this * 57.295779513

    fun Double.wrapTo180() = MathHelper.wrapAngleTo180_double(this)
    fun Float.wrapTo180() = MathHelper.wrapAngleTo180_float(this)

    val Double.isNeg
        get() = this < 0.0
    val Float.isNeg
        get() = this < 0f
    val Int.isNeg
        get() = this < 0

    val gameTicks
        get() = mc.thePlayer.ticksExisted

    val Entity.currPoint3
        get() = Point3(this.posX, this.posY, this.posZ)
    val Entity.nextPoint3
        get() = Point3(this.prevPosX, this.prevPosY, this.prevPosZ)
    val Entity.prevPoint3
        get() = this.nextPoint3.minus(this.currPoint3)

    val Entity.currPoint
        get() = Point(this.posX, this.posZ)
    val Entity.nextPoint
        get() = Point(this.prevPosX, this.prevPosZ)
    val Entity.prevPoint
        get() = this.nextPoint.minus(this.currPoint)

    val Entity.eyePoint3
        get() = Point3(this.posX, this.posY + this.eyeHeight, this.posZ)

    val Entity.motionSpeed
        get() = Point3(this.motionX, this.motionY, this.motionZ).toHypot()

    fun Double.rangeBy(num: Double) = if (num < 0.0) this.coerceIn(num, -num) else this.coerceIn(-num, num)
    fun Float.rangeBy(num: Float) = if (num < 0f) this.coerceIn(num, -num) else this.coerceIn(-num, num)
    fun Int.rangeBy(num: Int) = if (num < 0) this.coerceIn(num, -num) else this.coerceIn(-num, num)

    fun Double.rangeFrom(min: Double, max: Double) = if (min > max) this.coerceIn(max, min) else this.coerceIn(min, max)
    fun Float.rangeFrom(min: Float, max: Float) = if (min > max) this.coerceIn(max, min) else this.coerceIn(min, max)
    fun Int.rangeFrom(min: Int, max: Int) = if (min > max) this.coerceIn(max, min) else this.coerceIn(min, max)

    fun Double.randomBy(num: Double) = nextDouble(this - num, this + num)
    fun Float.randomBy(num: Float) = nextFloat(this - num, this + num)
    fun Int.randomBy(num: Int) = nextInt(this - num, this + num)

    fun Double.randomFrom(min: Double, max: Double) = nextDouble(this + min, this + max)
    fun Float.randomFrom(min: Float, max: Float) = nextFloat(this + min, this + max)
    fun Int.randomFrom(min: Int, max: Int) = nextInt(this + min, this + max)

    val Double.abs
        get() = abs(this)
    val Float.abs
        get() = abs(this)
    val Int.abs
        get() = abs(this)

    val Double.negTo0
        get() = this.coerceAtLeast(0.0)
    val Float.negTo0
        get() = this.coerceAtLeast(0f)
    val Int.negTo0
        get() = this.coerceAtLeast(0)

    val Double.toSign
        get() = if (this == 0.0) 0.0 else (if (this > 0.0) 1.0 else -1.0)
    val Float.toSign
        get() = if (this == 0f) 0f else (if (this > 0f) 1f else -1f)
    val Int.toSign
        get() = if (this == 0) 0 else (if (this > 0) 1 else -1)

    val signRandom
        get() = if (50.rate) 1 else -1

    fun Double.format(digits: Int) = "%.${digits}f".format(this).toDouble()
    fun Float.format(digits: Int) = "%.${digits}f".format(this).toFloat()

    fun onceFunc(pos1: DoubleArray, pos2: DoubleArray): DoubleArray {
        val k = (pos2[1] - pos2[0]) / (pos1[1] - pos1[0])
        val b = pos2[0] - (pos1[0] * k)
        return doubleArrayOf(k, b)
    }
    fun onceFunc(pos1: FloatArray, pos2: FloatArray): FloatArray {
        val k = (pos2[1] - pos2[0]) / (pos1[1] - pos1[0])
        val b = pos2[0] - (pos1[0] * k)
        return floatArrayOf(k, b)
    }

    val Int.rate
        get() = this >= nextInt(0, 100)

    fun getDistance(point: Point): Float = point.minus(mc.thePlayer.currPoint).toHypot().toFloat()
    fun getDistance3(point: Point3): Float = point.minus(mc.thePlayer.currPoint3).toHypot().toFloat()

    fun Point.getYaw(): Float {
        val diff = this.minus(mc.thePlayer.currPoint)
        return (atan2(diff.z, diff.x).toDeg() - 90).wrapTo180().toFloat()
    }

    fun Point3.getYaw(): Float {
        val diff = this.minus(mc.thePlayer.eyePoint3)
        return (atan2(diff.z, diff.x).toDeg() - 90).wrapTo180().toFloat()
    }

    fun Point3.getPitch(): Float {
        val diff = this.minus(mc.thePlayer.eyePoint3)
        return -atan2(diff.y, hypot(diff.x, diff.z)).toDeg().toFloat()
    }

    fun Point3.getRotation(): Rotation {
        val diff = this.minus(mc.thePlayer.eyePoint3)
        return Rotation(
            (atan2(diff.z, diff.x).toDeg() - 90).wrapTo180().toFloat(),
            -atan2(diff.y, hypot(diff.x, diff.z)).toDeg().toFloat()
        )
    }

    fun Entity.getRotsDiff(silent: Boolean): Float {
        val rotation = Rotation(this.currPoint.getYaw(), this.eyePoint3.getPitch())
        val currRotation = if (silent) (currentRotation ?: serverRotation) else mc.thePlayer.rotation
        return getRotationDifference(rotation, currRotation).toFloat()
    }

    fun EntityLivingBase.moveTo(yaw: Float, speed: Float) {
        yaw.toRad().let {
            this.motionX = speed * -sin(it.toDouble())
            this.motionZ = speed * cos(it.toDouble())
        }
    }
    fun EntityLivingBase.moveTo(point: Point, speed: Float) {
        this.moveTo(point.getYaw(), speed)
    }

    fun EntityLivingBase.moveAround(point: Point, radius: Float, speed: Float, ticks: Int = gameTicks, reverse: Boolean = false) {
        (speed * ticks / radius).let {
            val xDir = if (reverse) sin(it) else -cos(it)
            val zDir = if (reverse) -cos(it) else sin(it)
            this.moveTo(Point(
                point.x + radius * xDir,
                point.z + radius * zDir
            ), speed)
        }
    }

    fun getMoveTo(yaw: Float, speed: Float): Point {
        yaw.toRad().let {
            return Point(
                speed * -sin(it.toDouble()),
                speed * cos(it.toDouble())
            )
        }
    }
    fun EntityLivingBase.getMoveTo(point: Point, speed: Float): Point {
        return getMoveTo(point.getYaw(), speed)
    }

    fun EntityLivingBase.getMoveAround(point: Point, radius: Float, speed: Float, ticks: Int = gameTicks, reverse: Boolean = false): Point {
        (speed * ticks / radius).let {
            val xDir = if (reverse) sin(it) else -cos(it)
            val zDir = if (reverse) -cos(it) else sin(it)
            return this.getMoveTo(Point(
                point.x + radius * xDir,
                point.z + radius * zDir
            ), speed)
        }
    }

    fun Point3.isInsidePoints(minPoint3: Point3, maxPoint3: Point3): Boolean {
        return (
                this.x in minPoint3.x..maxPoint3.x &&
                        this.y in minPoint3.y..maxPoint3.y &&
                        this.z in minPoint3.z..maxPoint3.z
                )
    }

    fun Point3.isOverlapping(entity: EntityLivingBase): Boolean {
        entity.collisionBorderSize.toDouble().let { size ->
            entity.entityBoundingBox.expand(size, size, size).let {
                return this.isInsidePoints(
                    minPoint3 = Point3(it.minX, it.minY, it.minZ),
                    maxPoint3 = Point3(it.maxX, it.maxY, it.maxZ)
                )
            }
        }
    }

    val EntityPlayer.canCrit
        get() = (
                !this.onGround && this.fallDistance > 0f &&
                        !this.isOnLadder && !this.isInWater && !this.isInLava && !this.isInWeb &&
                        !this.isPotionActive(Potion.blindness) &&
                        this.ridingEntity == null
                )

    val EntityLivingBase.eyeY
        get() = this.posY + this.eyeHeight

    fun attackAction(
        target: Entity,
        packet: Boolean = true,
        packetType: String = "Normal",
        packetAmount: Int = 1,
        event: Boolean = true,
        eventAmount: Int = 1,
        swing: Boolean = true,
        swingType: String = "Normal",
        swingAmount: Int = 1,
        renderECrit: Boolean = true,
        renderCCrit: Boolean = false,
        registerClick: Boolean = true
    ) {
        // Event
        if (event) repeat(eventAmount) {
            mc.thePlayer.attackEntityWithModifiedSprint(
                target, false
            ) {mc.thePlayer.swingItem()}
        }

        // Swing
        if (swing) repeat(swingAmount) {
            when (swingType) {
                "Normal" -> mc.thePlayer.swingItem()
                "Packet" -> sendPacket(C0APacketAnimation())
            }
        }

        // Packet
        if (packet) repeat(packetAmount) {
            when (packetType) {
                "Normal" -> sendPacket(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))
                "Simulate" -> {
                    sendPacket(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))
                    mc.thePlayer.attackTargetEntityWithCurrentItem(target)
                }
            }

            // CPSCounter
            if (registerClick) registerClick(CPSCounter.MouseButton.LEFT)

            // Particle
            if (renderECrit) mc.thePlayer.onEnchantmentCritical(target)
            if (renderCCrit) mc.thePlayer.onCriticalHit(target)
        }
    }
    fun alert(str: String) {
        chat(str)
    }
    val String.alert
        get() = alert(this)

    fun debug(str: String) {
        (mc.thePlayer ?: return).let { player ->
            val prefixMessage = "§7[Debug]§r $str"
            val jsonObject = JsonObject()
            jsonObject.addProperty("text", prefixMessage)
            player.addChatMessage(IChatComponent.Serializer.jsonToComponent(jsonObject.toString()))
        }
    }
    val String.debug
        get() = debug(this)

    fun log(str: String) {
        (mc.thePlayer ?: return).let { player ->
            val jsonObject = JsonObject()
            jsonObject.addProperty("text", str)
            player.addChatMessage(IChatComponent.Serializer.jsonToComponent(jsonObject.toString()))
        }
    }
    val String.log
        get() = log(this)

    fun String.debugWith(prefix: String = "") {
        (mc.thePlayer ?: return).let { player ->
            val prefixMessage = if (prefix == "") this else "§7[$prefix]§r $this"
            val jsonObject = JsonObject()
            jsonObject.addProperty("text", prefixMessage)
            player.addChatMessage(IChatComponent.Serializer.jsonToComponent(jsonObject.toString()))
        }
    }

    fun msg(str: String) {
        mc.thePlayer.sendChatMessage(str)
    }
    val String.msg
        get() = mc.thePlayer.sendChatMessage(this)

    fun swapItem(slot: Int) {
        mc.thePlayer.inventory.currentItem = slot.rangeFrom(0, 8)
    }

    fun isKeyMoving(): Boolean {
        return mc.gameSettings.let {
            it.keyBindForward.isKeyDown ||
                    it.keyBindBack.isKeyDown
            it.keyBindLeft.isKeyDown ||
                    it.keyBindRight.isKeyDown ||
                    it.keyBindJump.isKeyDown
        }
    }
}