//Ai Neko Code
// skid neko bounce 
// https://github.com/RouQingNeko1024/NekoBounce
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object ChatPrefix : Module("ChatPrefix", Category.CLIENT) {
    
    val mode by choices(
        "Mode",
        arrayOf(
            "Naven",
            "Opai",
            "FireBounce",
            "D1ckBounce",
            "Augustus",
            "Mayu",
            "SilenceFix",
            "Custom"
        ),
        "D1ckBounce"
    )
    
    val customPrefix by text("CustomText", "[Prefix]") { mode == "Custom" }
    
    fun getFormattedPrefix(): String {
        return when (mode.lowercase()) {
            "naven" -> "[§bN§r] "
            "opai" -> "§fOpai >>§r "
            "firebounce" -> "§8[§cF§6i§er§ae§bB§do§9u§5n§cc§6e§8]§r "
            "d1ckbounce" -> "§8[§b§lD1ckBounce§8]§r §f§l» §r"
            "augustus" -> "§6[§9Augustus§6]§r "
            "mayu" -> "§7[§cM§6y§ea§au§7]§r "
            "silencefix" -> "§b欣欣公益客户�?§7>§r "
            "custom" -> "$customPrefix "
            else -> "§8[§b§lD1ckBounce]§r §f§l» §r"
        }
    }
    
    override val tag: String
        get() = mode
}
