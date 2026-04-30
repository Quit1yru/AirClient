/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.utils.extensions

import java.awt.Color

fun Color.withAlpha(a: Int) = Color(red, green, blue, a)
