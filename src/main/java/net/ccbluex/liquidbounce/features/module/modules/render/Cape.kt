// skid neko bounce 
// https://github.com/RouQingNeko1024/NekoBounce
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.util.ResourceLocation
import java.util.*

object Cape : Module("Cape", Category.RENDER) {

    private val capeMode by choices(
        "Cape",
        arrayOf("cape1", "cape2", "cape3", "cape4", "astolfo", "ravenanime", "ravenxd", "Augustus", "Astolfotrap"),
        "cape1"
    )

    private val capes = mapOf(
        "cape1" to ResourceLocation("airclient/cape/cape1.png"),
        "cape2" to ResourceLocation("airclient/cape/cape2.png"),
        "cape3" to ResourceLocation("airclient/cape/cape3.png"),
        "cape4" to ResourceLocation("airclient/cape/cape4.png"),
        "astolfo" to ResourceLocation("airclient/cape/astolfo.png"),
        "ravenanime" to ResourceLocation("airclient/cape/ravenanime.png"),
        "ravenxd" to ResourceLocation("airclient/cape/ravenxd.png"),
        "Augustus" to ResourceLocation("airclient/cape/Augustus.png"),
        "Astolfotrap" to ResourceLocation("airclient/cape/Astolfotrap.png")
    )

    fun getCapeForPlayer(uuid: UUID): ResourceLocation? {
        if (!state) return null
        
        val currentPlayer = mc.thePlayer ?: return null
        
        if (uuid != currentPlayer.uniqueID) return null
        
        return capes[capeMode]
    }

    override val tag
        get() = capeMode
}
