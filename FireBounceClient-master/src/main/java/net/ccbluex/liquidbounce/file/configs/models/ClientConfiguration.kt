package net.ccbluex.liquidbounce.file.configs.models

import net.ccbluex.liquidbounce.FireBounce
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.render.IconUtils
import net.ccbluex.liquidbounce.utils.timing.TickedActions.setIcon
import org.lwjgl.opengl.Display

object ClientConfiguration : Configurable("ClientConfiguration"), MinecraftInstance {
    var clientTitle by boolean("ClientTitle", true)
    var showPlayTime by boolean("ShowPlayTime", true)
    var showRealPlayer by boolean("ShowRealPlayer", true)
    var customBackground by boolean("CustomBackground", true)
    var particles by boolean("Particles", false)
    var stylisedAlts by boolean("StylisedAlts", true)
    var unformattedAlts by boolean("CleanAlts", true)
    var altsLength by int("AltsLength", 16, 4..20)
    var altsPrefix by text("AltsPrefix", "")
    // The game language can be overridden by the user. empty=default
    var overrideLanguage by text("OverrideLanguage","")


    fun updateClientWindow() {
        if (clientTitle) {
            Display.setTitle(FireBounce.getDynamicTitle())
            if (setIcon) {
                IconUtils.favicon?.let { icons ->
                    Display.setIcon(icons)
                }
                setIcon = false
            }
        } else {
            // Set original title
            Display.setTitle("Minecraft 1.8.9")
            // Update favicon
            mc.setWindowIcon()
        }
    }

}