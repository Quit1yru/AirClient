/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions
import java.awt.Color

fun String.toLowerCamelCase() = String(toCharArray().apply {
    this[0] = this[0].lowercaseChar()
})

fun Color.darker(factor: Float) = Color(this.red / 255F * factor.coerceIn(0F, 1F), this.green / 255F * factor.coerceIn(0F, 1F), this.blue / 255F * factor.coerceIn(0F, 1F), this.alpha / 255F)
fun Color.setAlpha(factor: Int) = Color(this.red, this.green, this.blue, factor.coerceIn(0, 255))


fun Float.animLinear(speed: Float, min: Float, max: Float): Float {
    return (this + speed).coerceIn(min, max)
}