/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object NoBlockParticles : Module("NoBlockParticles", Category.RENDER) {
    val blockParticles by boolean("BlockParticles", true)
    val breakBlockParticles by boolean("BreakBlockParticles", true)

}
