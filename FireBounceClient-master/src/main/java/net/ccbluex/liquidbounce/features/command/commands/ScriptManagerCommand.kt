/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.FireBounce.isStarting
import net.ccbluex.liquidbounce.FireBounce.moduleManager
import net.ccbluex.liquidbounce.FireBounce.scriptManager
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.file.FileManager.clickGuiConfig
import net.ccbluex.liquidbounce.file.FileManager.hudConfig
import net.ccbluex.liquidbounce.file.FileManager.loadConfig
import net.ccbluex.liquidbounce.file.FileManager.loadConfigs
import net.ccbluex.liquidbounce.file.FileManager.modulesConfig
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.io.FileFilters
import net.ccbluex.liquidbounce.utils.io.MiscUtils
import net.ccbluex.liquidbounce.utils.io.extractZipTo
import java.awt.Desktop

object ScriptManagerCommand : Command("scriptmanager", "scripts") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        val usedAlias = args[0].lowercase()

        if (args.size < 2) {
            chatSyntax("$usedAlias <import/delete/reload/folder/list>")
            return
        }

        when (args[1].lowercase()) {
            "import" -> {
                try {
                    val file = MiscUtils.openFileChooser(FileFilters.JAVASCRIPT, FileFilters.ARCHIVE, FileFilters.LUA) ?: return

                    when (file.extension.lowercase()) {
                        "js", "lua" -> {
                            scriptManager.importScript(file)
                            loadConfig(clickGuiConfig)
                            chat("Successfully imported script.")
                        }

                        "zip" -> {
                            // 对于 ZIP 文件，我们只导入 JavaScript 脚本以保持兼容性
                            val existingJavaScriptFiles = scriptManager.availableJavaScriptFiles.toSet()
                            val existingLuaFiles = scriptManager.availableLuaFiles.toSet()

                            file.extractZipTo(scriptManager.scriptsFolder)

                            // 加载新添加的 JavaScript 脚本
                            scriptManager.availableJavaScriptFiles.filterNot {
                                it in existingJavaScriptFiles
                            }.forEach(scriptManager::loadScript)

                            // 加载新添加的 Lua 脚本
                            scriptManager.availableLuaFiles.filterNot {
                                it in existingLuaFiles
                            }.forEach(scriptManager::loadScript)

                            loadConfigs(clickGuiConfig, hudConfig)
                            chat("Successfully imported scripts from archive.")
                        }

                        else -> chat("The file extension has to be .js, .lua or .zip")
                    }
                } catch (t: Throwable) {
                    LOGGER.error("Something went wrong while importing a script.", t)
                    chat("${t.javaClass.name}: ${t.message}")
                }
            }

            "delete" -> {
                try {
                    if (args.size <= 2) {
                        chatSyntax("$usedAlias delete <index>")
                        // 显示脚本列表
                        displayScriptsList()
                        return
                    }

                    val scriptIndex = args[2].toInt()
                    val allScripts = scriptManager.getAllScripts()

                    if (scriptIndex >= allScripts.size) {
                        chat("Index $scriptIndex is too high. Available scripts: 0-${allScripts.size - 1}")
                        displayScriptsList()
                        return
                    }

                    val script = allScripts[scriptIndex]
                    scriptManager.deleteScript(script)
                    loadConfigs(clickGuiConfig, hudConfig)
                    chat("Successfully deleted script.")
                } catch (_: NumberFormatException) {
                    chatSyntaxError()
                } catch (t: Throwable) {
                    LOGGER.error("Something went wrong while deleting a script.", t)
                    chat("${t.javaClass.name}: ${t.message}")
                }
            }

            "reload" -> {
                try {
                    CommandManager.registerCommands()
                    isStarting = true

                    scriptManager.reloadScripts()

                    for (module in moduleManager) moduleManager.generateCommand(module)
                    loadConfig(modulesConfig)

                    isStarting = false
                    loadConfigs(valuesConfig, clickGuiConfig, hudConfig)

                    chat("Successfully reloaded all scripts.")
                } catch (t: Throwable) {
                    LOGGER.error("Something went wrong while reloading all scripts.", t)
                    chat("${t.javaClass.name}: ${t.message}")
                }
            }

            "folder" -> {
                try {
                    Desktop.getDesktop().open(scriptManager.scriptsFolder)
                    chat("Successfully opened scripts folder.")
                } catch (t: Throwable) {
                    LOGGER.error("Something went wrong while trying to open your scripts folder.", t)
                    chat("${t.javaClass.name}: ${t.message}")
                }
            }

            "list" -> {
                displayScriptsList()
            }

            else -> {
                chatSyntax("$usedAlias <import/delete/reload/folder/list>")
            }
        }
    }

    /**
     * Display list of all loaded scripts
     */
    private fun displayScriptsList() {
        val allScripts = scriptManager.getAllScripts()

        if (allScripts.isEmpty()) {
            chat("No scripts loaded.")
            return
        }

        chat("§c§lLoaded Scripts (§e${allScripts.size}§c§l)")

        allScripts.forEachIndexed { index, script ->
            when (script) {
                is net.ccbluex.liquidbounce.script.Script -> {
                    // JavaScript script
                    chat("$index: §a§l${script.scriptName} §7(JS) §av${script.scriptVersion} §3by §a${script.scriptAuthors.joinToString(", ")}")
                }
                is net.ccbluex.liquidbounce.script.LuaScript -> {
                    // Lua script
                    chat("$index: §b§l${script.scriptName} §7(Lua) §bv${script.scriptVersion} §3by §b${script.scriptAuthors.joinToString(", ")}")
                }
                else -> {
                    chat("$index: §cUnknown script type")
                }
            }
        }

        chat("§7Use '.scriptmanager delete <index>' to remove a script")
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("delete", "import", "folder", "reload", "list")
                .filter { it.startsWith(args[0], true) }

            2 -> {
                if (args[0].equals("delete", true)) {
                    // 为 delete 命令提供脚本索引的自动完成
                    val allScripts = scriptManager.getAllScripts()
                    (0 until allScripts.size).map { it.toString() }
                        .filter { it.startsWith(args[1], true) }
                } else {
                    emptyList()
                }
            }

            else -> emptyList()
        }
    }
}