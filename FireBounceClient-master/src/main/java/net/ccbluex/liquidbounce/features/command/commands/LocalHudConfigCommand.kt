package net.ccbluex.liquidbounce.features.command.commands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.ui.client.hud.HUD
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.kotlin.SharedScopes

object LocalHudConfigCommand : Command("localhudconfig", "lhud") {

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        val usedAlias = args[0].lowercase()

        if (args.size <= 1) {
            chatSyntax("$usedAlias <load/save/list/create/delete/rename/current/reset>")
            return
        }

        SharedScopes.IO.launch {
            when (args[1].lowercase()) {
                "load" -> loadConfig(args)
                "save" -> saveConfig(args)
                "list" -> listConfigs()
                "create" -> createConfig(args)
                "delete" -> deleteConfig(args)
                "rename" -> renameConfig(args)
                "current" -> showCurrentConfig()
                "reset" -> resetConfig()
                else -> chatSyntax("$usedAlias <load/save/list/create/delete/rename/current/reset>")
            }
        }
    }

    private suspend fun loadConfig(args: Array<String>) {
        withContext(Dispatchers.IO) {
            if (args.size <= 2) {
                chatSyntax("${args[0]} load <configName>")
                return@withContext
            }

            try {
                val configName = args[2]
                if (FileManager.hudConfig.switchConfig(configName)) {
                    chat("§6Loaded HUD configuration: §e$configName")
                    HUD.addNotification(Notification.informative("HUD Config", "Loaded: $configName"))
                    playEdit()
                } else {
                    chat("§cConfiguration not found: §e$configName")
                }
            } catch (e: Exception) {
                chat("§cFailed to load configuration: §3${e.message}")
                LOGGER.error("Failed to load HUD configuration", e)
            }
        }
    }

    private suspend fun saveConfig(args: Array<String>) {
        withContext(Dispatchers.IO) {
            try {
                if (args.size > 2) {
                    // 保存为指定名称
                    val configName = args[2]
                    if (FileManager.hudConfig.saveAsConfig(configName)) {
                        chat("§6HUD configuration saved as: §e$configName")
                        HUD.addNotification(Notification.informative("HUD Config", "Saved as: $configName"))
                    } else {
                        chat("§cFailed to save configuration as: §e$configName")
                    }
                } else {
                    // 保存到当前配置
                    chat("§9Saving HUD configuration...")
                    FileManager.saveConfig(FileManager.hudConfig)
                    chat("§6HUD configuration saved successfully!")
                    HUD.addNotification(Notification.informative("HUD Config", "HUD configuration saved"))
                }
                playEdit()
            } catch (e: Exception) {
                chat("§cFailed to save HUD configuration: §3${e.message}")
                LOGGER.error("Failed to save HUD configuration", e)
            }
        }
    }

    private suspend fun listConfigs() {
        withContext(Dispatchers.IO) {
            try {
                val configs = FileManager.hudConfig.getAvailableConfigs()
                val current = FileManager.hudConfig.getCurrentConfigName()

                chat("§6Available HUD Configurations:")
                for (config in configs) {
                    if (config == current) {
                        chat("§a> §e$config §a← current")
                    } else {
                        chat("§7> $config")
                    }
                }
            } catch (e: Exception) {
                chat("§cFailed to list configurations: §3${e.message}")
                LOGGER.error("Failed to list HUD configurations", e)
            }
        }
    }

    private suspend fun createConfig(args: Array<String>) {
        withContext(Dispatchers.IO) {
            if (args.size <= 2) {
                chatSyntax("${args[0]} create <configName>")
                return@withContext
            }

            try {
                val configName = args[2]
                if (FileManager.hudConfig.createConfig(configName)) {
                    chat("§6Created new HUD configuration: §e$configName")
                    HUD.addNotification(Notification.informative("HUD Config", "Created: $configName"))
                    playEdit()
                } else {
                    chat("§cConfiguration already exists: §e$configName")
                }
            } catch (e: Exception) {
                chat("§cFailed to create configuration: §3${e.message}")
                LOGGER.error("Failed to create HUD configuration", e)
            }
        }
    }

    private suspend fun deleteConfig(args: Array<String>) {
        withContext(Dispatchers.IO) {
            if (args.size <= 2) {
                chatSyntax("${args[0]} delete <configName>")
                return@withContext
            }

            try {
                val configName = args[2]
                if (FileManager.hudConfig.deleteConfig(configName)) {
                    chat("§6Deleted HUD configuration: §e$configName")
                    HUD.addNotification(Notification.informative("HUD Config", "Deleted: $configName"))
                    playEdit()
                } else {
                    chat("§cCannot delete configuration: §e$configName")
                }
            } catch (e: Exception) {
                chat("§cFailed to delete configuration: §3${e.message}")
                LOGGER.error("Failed to delete HUD configuration", e)
            }
        }
    }

    private suspend fun renameConfig(args: Array<String>) {
        withContext(Dispatchers.IO) {
            if (args.size <= 3) {
                chatSyntax("${args[0]} rename <oldName> <newName>")
                return@withContext
            }

            try {
                val oldName = args[2]
                val newName = args[3]
                if (FileManager.hudConfig.renameConfig(oldName, newName)) {
                    chat("§6Renamed configuration: §e$oldName §6→ §e$newName")
                    HUD.addNotification(Notification.informative("HUD Config", "Renamed: $oldName to $newName"))
                    playEdit()
                } else {
                    chat("§cFailed to rename configuration")
                }
            } catch (e: Exception) {
                chat("§cFailed to rename configuration: §3${e.message}")
                LOGGER.error("Failed to rename HUD configuration", e)
            }
        }
    }

    private suspend fun showCurrentConfig() {
        withContext(Dispatchers.IO) {
            try {
                val current = FileManager.hudConfig.getCurrentConfigName()
                chat("§6Current HUD configuration: §e$current")
            } catch (e: Exception) {
                chat("§cFailed to get current configuration: §3${e.message}")
                LOGGER.error("Failed to get current HUD configuration", e)
            }
        }
    }

    private suspend fun resetConfig() {
        withContext(Dispatchers.IO) {
            try {
                chat("§9Resetting HUD to default...")
                HUD.setDefault()
                FileManager.saveConfig(FileManager.hudConfig)
                chat("§6HUD reset to default successfully!")
                HUD.addNotification(Notification.informative("HUD Config", "HUD reset to default"))
                playEdit()
            } catch (e: Exception) {
                chat("§cFailed to reset HUD: §3${e.message}")
                LOGGER.error("Failed to reset HUD", e)
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("load", "save", "list", "create", "delete", "rename", "current", "reset")
                .filter { it.startsWith(args[0], true) }
            2 -> when (args[0].lowercase()) {
                "load", "delete" -> {
                    FileManager.hudConfig.getAvailableConfigs()
                        .filter { it.startsWith(args[1], true) }
                }
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "rename" -> {
                    FileManager.hudConfig.getAvailableConfigs()
                        .filter { it.startsWith(args[2], true) }
                }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}