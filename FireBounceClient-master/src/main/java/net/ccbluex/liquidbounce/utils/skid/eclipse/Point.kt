/*
 * Eclipse Hacked Client
 * A mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.utils.skid.eclipse

import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.skid.eclipse.EasyUtils.format
import net.ccbluex.liquidbounce.utils.skid.eclipse.EasyUtils.toDeg
import net.ccbluex.liquidbounce.utils.skid.eclipse.EasyUtils.toRad
import net.ccbluex.liquidbounce.utils.skid.eclipse.EasyUtils.wrapTo180
import net.minecraft.util.Vec3
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Two-Dimensional Spatial Point
 */
class Point(var x: Double, var z: Double) : MinecraftInstance {

    val toStr: String
        get() = "Point($x, $z)"

    operator fun plus(point: Point): Point = Point(x + point.x, z + point.z)
    operator fun plus(num: Double): Point = Point(x + num, z + num)
    operator fun plus(num: Float): Point = Point(x + num, z + num)
    operator fun plus(num: Int): Point = Point(x + num, z + num)

    operator fun minus(point: Point): Point = Point(x - point.x, z - point.z)
    operator fun minus(num: Double): Point = Point(x - num, z - num)
    operator fun minus(num: Float): Point = Point(x - num, z - num)
    operator fun minus(num: Int): Point = Point(x - num, z - num)

    operator fun times(point: Point): Point = Point(x * point.x, z * point.z)
    operator fun times(num: Double): Point = Point(x * num, z * num)
    operator fun times(num: Float): Point = Point(x * num, z * num)
    operator fun times(num: Int): Point = Point(x * num, z * num)

    operator fun div(point: Point): Point = Point(x / point.x, z / point.z)
    operator fun div(num: Double): Point = Point(x / num, z / num)
    operator fun div(num: Float): Point = Point(x / num, z / num)
    operator fun div(num: Int): Point = Point(x / num, z / num)

    fun toPoint3(y: Double) = Point3(x, y, z)
    fun toHypot(): Double = sqrt(x * x + z * z)

    fun toYaw(point: Point): Float = (point - this).run { (atan2(z, x).toDeg() - 90).wrapTo180().toFloat() }

    fun format(digits: Int): Point = Point(x.format(digits), z.format(digits))
    fun offset(ofsX: Double, ofsZ: Double) = Point(x + ofsX, z + ofsZ)

}

/**
 * Three-Dimensional Spatial Point
 */
class Point3(var x: Double, var y: Double, var z: Double) : MinecraftInstance {

    val toStr: String
        get() = "Point3($x, $y, $z)"

    operator fun plus(point: Point3): Point3 = Point3(x + point.x, y + point.y, z + point.z)
    operator fun plus(num: Double): Point3 = Point3(x + num, y + num, z + num)
    operator fun plus(num: Float): Point3 = Point3(x + num, y + num, z + num)
    operator fun plus(num: Int): Point3 = Point3(x + num, y + num, z + num)

    operator fun minus(point: Point3): Point3 = Point3(x - point.x, y - point.y, z - point.z)
    operator fun minus(num: Double): Point3 = Point3(x - num, y - num, z - num)
    operator fun minus(num: Float): Point3 = Point3(x - num, y - num, z - num)
    operator fun minus(num: Int): Point3 = Point3(x - num, y - num, z - num)

    operator fun times(point: Point3): Point3 = Point3(x * point.x, y * point.y, z * point.z)
    operator fun times(num: Double): Point3 = Point3(x * num, y * num, z * num)
    operator fun times(num: Float): Point3 = Point3(x * num, y * num, z * num)
    operator fun times(num: Int): Point3 = Point3(x * num, y * num, z * num)

    operator fun div(point: Point3): Point3 = Point3(x / point.x, y / point.y, z / point.z)
    operator fun div(num: Double): Point3 = Point3(x / num, y / num, z / num)
    operator fun div(num: Float): Point3 = Point3(x / num, y / num, z / num)
    operator fun div(num: Int): Point3 = Point3(x / num, y / num, z / num)

    fun toPoint(): Point = Point(x, z)
    fun toPoint3(point: Point, pitch: Float) = Point3(point.x, this.y + (-tan(pitch.toRad()) * (point - this.toPoint()).toHypot()), point.z)
    fun toVec3(): Vec3 = Vec3(x, y, z)
    fun toHypot(): Double = sqrt(x * x + y * y + z * z)

    fun toYaw(point3: Point3): Float = (point3 - this).run { (atan2(z, x).toDeg() - 90.0).wrapTo180().toFloat() }
    fun toPitch(point3: Point3): Float = (point3 - this).run { -atan2(y, hypot(x, z)).toDeg().toFloat() }
    fun toRotation(point3: Point3): Rotation = Rotation(toYaw(point3), toPitch(point3))

    fun format(digits: Int): Point3 = Point3(x.format(digits), y.format(digits), z.format(digits))
    fun offset(ofsX: Double, ofsY: Double, ofsZ: Double) = Point3(x + ofsX, y + ofsY, z + ofsZ)

}

/**
 * Two-Dimensional Spatial Points
 */
class Points(var first: Point, var last: Point) {
    val toStr: String
        get() = "first: ${first.toStr}, last: ${last.toStr}"

    operator fun component1() = first
    operator fun component2() = last
}

/**
 * Three-Dimensional Spatial Points
 */
class Point3s(var first: Point3, var last: Point3) {
    val toStr: String
        get() = "first: ${first.toStr}, last: ${last.toStr}"

    operator fun component1() = first
    operator fun component2() = last
}

/**
 * Zero Point
 */
val zeroPoint = Point(0.0, 0.0)

/**
 * Zero Point3
 */
val zeroPoint3 = Point3(0.0, 0.0, 0.0)