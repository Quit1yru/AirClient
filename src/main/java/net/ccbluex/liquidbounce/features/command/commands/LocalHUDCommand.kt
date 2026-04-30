/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.command.commands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.config.SettingsUtils
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.file.FileManager.hudSettingsDir
import net.ccbluex.liquidbounce.ui.client.hud.HUD.addNotification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.kotlin.SharedScopes
import net.ccbluex.liquidbounce.utils.kotlin.StringUtils
import java.awt.Desktop
import java.io.File
import java.io.IOException

object LocalHUDCommand : Command("localhud", "localhudsetting", "hudsetting") {

    override fun execute(args: Array<String>) {
        val usedAlias = args[0].lowercase()

        if (args.size <= 1) {
            chatSyntax("$usedAlias <load/save/list/delete/folder>")
            return
        }

        SharedScopes.IO.launch {
            when (args[1].lowercase()) {
                "load" -> loadSettings(args)
                "save" -> saveSettings(args)
                "delete" -> deleteSettings(args)
                "list" -> listSettings()
                "folder" -> openSettingsFolder()
                else -> chatSyntax("$usedAlias <load/save/list/delete/folder>")
            }
        }
    }

    private suspend fun loadSettings(args: Array<String>) {
        withContext(Dispatchers.IO) {
            if (args.size <= 2) {
                chatSyntax("${args[0].lowercase()} load <name>")
                return@withContext
            }

            val settingsFile = File(hudSettingsDir, args[2] + ".txt")

            if (!settingsFile.exists()) {
                chat("§cHUD settings file does not exist! §e(Ensure its .txt)")
                return@withContext
            }

            try {
                chat("§9Loading HUD settings...")
                val settings = settingsFile.readText()
                chat("§9Applying HUD settings...")
                SettingsUtils.applyScriptForCategory(settings, Category.RENDER)
                chat("§6HUD settings applied successfully.")
                addNotification(Notification.informative("Local HUD Settings Command", "Updated HUD Settings"))
                playEdit()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun saveSettings(args: Array<String>) {
        withContext(Dispatchers.IO) {
            if (args.size <= 2) {
                chatSyntax("${args[0].lowercase()} save <name> [all/values/binds/states]...")
                return@withContext
            }

            val settingsFile = File(hudSettingsDir, args[2] + ".txt")

            try {
                if (settingsFile.exists())
                    settingsFile.delete()

                settingsFile.createNewFile()

                val option = if (args.size > 3) StringUtils.toCompleteString(args, 3).lowercase() else "default"
                val all = "all" in option
                val default = "default" in option
                val values = all || default || "values" in option
                val binds = all || "binds" in option
                val states = all || default || "states" in option

                if (!values && !binds && !states) {
                    chatSyntaxError()
                    return@withContext
                }

                chat("§9Creating HUD settings...")
                val settingsScript = SettingsUtils.generateScriptForCategory(values, binds, states, Category.RENDER)

                chat("§9Saving HUD settings...")
                settingsFile.writeText(settingsScript)

                chat("§6HUD settings saved successfully.")
            } catch (throwable: Throwable) {
                chat("§cFailed to create HUD config: §3${throwable.message}")
                LOGGER.error("Failed to create HUD config.", throwable)
            }
        }
    }

    private suspend fun deleteSettings(args: Array<String>) {
        withContext(Dispatchers.IO) {
            if (args.size <= 2) {
                chatSyntax("${args[0].lowercase()} delete <name>")
                return@withContext
            }

            val settingsFile = File(hudSettingsDir, args[2] + ".txt")

            if (!settingsFile.exists()) {
                chat("§cHUD settings file does not exist!")
                return@withContext
            }

            settingsFile.delete()
            chat("§6HUD settings file deleted successfully.")
        }
    }

    private suspend fun listSettings() {
        withContext(Dispatchers.IO) {
            chat("§cHUD Settings:")

            val settings = hudSettingsDir.listFiles() ?: return@withContext

            for (file in settings) {
                chat("> " + file.name.removeSuffix(".txt"))
            }
        }
    }

    private suspend fun openSettingsFolder() {
        withContext(Dispatchers.IO) {
            try {
                Desktop.getDesktop().open(hudSettingsDir)
            } catch (e: IOException) {
                LOGGER.error("Failed to open HUD settings folder.", e)
                chat("§cFailed to open HUD settings folder.")
            }
        }
    }


    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("delete", "list", "load", "save", "folder").filter { it.startsWith(args[0], true) }

            2 -> {
                when (args[0].lowercase()) {
                    "delete", "load", "save" -> {
                        val settings = hudSettingsDir.listFiles() ?: return emptyList()
                        settings.map { it.name.removeSuffix(".txt") }.filter { it.startsWith(args[1], true) }
                    }

                    else -> emptyList()
                }
            }

            3 -> {
                when (args[0].lowercase()) {
                    "save" -> listOf("all", "default", "values", "binds", "states").filter {
                        it.startsWith(
                            args[2],
                            true
                        )
                    }

                    else -> emptyList()
                }
            }

            else -> emptyList()
        }
    }
}
