/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.config

import net.ccbluex.liquidbounce.FireBounce.moduleManager
import net.ccbluex.liquidbounce.api.ClientApi
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.Targets
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extra.ColorUtils.translateAlternateColorCodes
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.utils.io.get
import net.ccbluex.liquidbounce.utils.kotlin.StringUtils
import org.lwjgl.input.Keyboard
import kotlin.reflect.KMutableProperty0

/**
 * Utility class for handling settings and scripts in FireBounce.
 */
object SettingsUtils {

    fun loadFromUrl(url: String) = if (url.startsWith("http")) {
        HttpClient.get(url).use {
            if (!it.isSuccessful) {
                error(it.message)
            }

            it.body.string()
        }
    } else {
        ClientApi.getSettingsScript(settingId = url)
    }

    /**
     * Execute settings script silently (without showing each individual change).
     * @param script The script to apply.
     * @param silent If true, suppresses individual change messages.
     */
    fun applyScript(script: String, silent: Boolean = true) {
        script.lineSequence().forEachIndexed { index, s ->
            if (s.isEmpty() || s.startsWith('#')) {
                return@forEachIndexed
            }

            val args = s.split(' ').toTypedArray()

            if (args.size <= 1) {
                if (!silent) {
                    chat("§7[§3§lAutoSettings§7] §cSyntax error at line '$index' in setting script.\n§8§lLine: §7$s")
                }
                return@forEachIndexed
            }

            when (args[0]) {
                "chat" -> {
                    if (!silent) {
                        chat(
                            "§e${
                                translateAlternateColorCodes(
                                    StringUtils.toCompleteString(
                                        args,
                                        1
                                    )
                                )
                            }"
                        )
                    }
                }

                "unchat" -> {
                    if (!silent) {
                        chat(
                            translateAlternateColorCodes(
                                StringUtils.toCompleteString(
                                    args,
                                    1
                                )
                            )
                        )
                    }
                }

                "load" -> {
                    val url = StringUtils.toCompleteString(args, 1)
                    runCatching {
                        applyScript(loadFromUrl(url), silent)
                    }.onSuccess {
                        if (!silent) {
                            chat("§7[§3§lAutoSettings§7] §7Loaded settings §a§l$url§7.")
                        }
                    }.onFailure {
                        if (!silent) {
                            chat("§7[§3§lAutoSettings§7] §7Failed to load settings §a§l$url§7.")
                        }
                    }
                }

                "targetPlayer", "targetPlayers" -> setTargetSetting(Targets::player, args, silent)
                "targetMobs" -> setTargetSetting(Targets::mob, args, silent)
                "targetAnimals" -> setTargetSetting(Targets::animal, args, silent)
                "targetInvisible" -> setTargetSetting(Targets::invisible, args, silent)
                "targetDead" -> setTargetSetting(Targets::dead, args, silent)
                "targetVillager" -> setTargetSetting(Targets::villager, args, silent)
                "targetBedWarsMob" -> setTargetSetting(Targets::bedwarsMob, args, silent)

                else -> {
                    if (args.size < 3) {
                        if (!silent) {
                            chat("§7[§3§lAutoSettings§7] §cSyntax error at line '$index' in setting script.\n§8§lLine: §7$s")
                        }
                        return@forEachIndexed
                    }

                    val moduleName = args[0]
                    val valueName = args[1]
                    val value = args[2]
                    val module = moduleManager[moduleName]

                    if (module == null) {
                        if (!silent) {
                            chat("§7[§3§lAutoSettings§7] §cModule §a§l$moduleName§c does not exist!")
                        }
                        return@forEachIndexed
                    }

                    when (valueName) {
                        "toggle" -> setToggle(module, value, silent)
                        "bind" -> setBind(module, value, silent)
                        else -> setValue(module, valueName, value, args, silent)
                    }
                }
            }
        }

        FileManager.saveConfig(FileManager.valuesConfig)
    }

    // Utility functions for setting target settings
    private fun setTargetSetting(setting: KMutableProperty0<Boolean>, args: Array<String>, silent: Boolean) {
        setting.set(args[1].toBoolean())
        if (!silent) {
            chat("§7[§3§lAutoSettings§7] §a§l${args[0]}§7 set to §c§l${args[1]}§7.")
        }
    }

    // Utility functions for setting toggles
    private fun setToggle(module: Module, value: String, silent: Boolean) {
        module.state = value.toBoolean()
        if (!silent) {
            chat("§7[§3§lAutoSettings§7] §a§l${module.getName()} §7was toggled §c§l${if (module.state) "on" else "off"}§7.")
        }
    }

    // Utility functions for setting binds
    private fun setBind(module: Module, value: String, silent: Boolean) {
        module.keyBind = Keyboard.getKeyIndex(value)
        if (!silent) {
            chat(
                "§7[§3§lAutoSettings§7] §a§l${module.getName()} §7was bound to §c§l${
                    Keyboard.getKeyName(
                        module.keyBind
                    )
                }§7."
            )
        }
    }

    // Utility functions for setting values
    private fun setValue(module: Module, valueName: String, value: String, args: Array<String>, silent: Boolean) {
        val moduleValue = module[valueName]

        if (moduleValue == null) {
            if (!silent) {
                chat("§7[§3§lAutoSettings§7] §cValue §a§l$valueName§c wasn't found in module §a§l${module.getName()}§c.")
            }
            return
        }

        try {
            moduleValue.fromText(value)
            if (!silent) {
                chat("§7[§3§lAutoSettings§7] §a§l${module.getName()}§7 value §8§l${moduleValue.name}§7 set to §c§l$value§7.")
            }
        } catch (e: Exception) {
            if (!silent) {
                chat("§7[§3§lAutoSettings§7] §a§l${e.javaClass.name}§7(${e.message}) §cAn Exception occurred while setting §a§l$value§c to §a§l${moduleValue.name}§c in §a§l${module.getName()}§c.")
            }
        }
    }
    /**
     * Generate settings script.
     * @param values Include values in the script.
     * @param binds Include key binds in the script.
     * @param states Include module states in the script.
     * @return The generated script.
     */
    fun generateScript(values: Boolean, binds: Boolean, states: Boolean): String {
        val all = values && binds && states

        return buildString {
            for (module in moduleManager) {
                if (all || !module.subjective) {
                    if (values) {
                        for (value in module.values) {
                            if (all || !value.subjective && value.shouldRender()) {
                                val valueString = "${module.name} ${value.name} ${value.toText()}"

                                if (valueString.isNotBlank()) {
                                    appendLine(valueString)
                                }
                            }
                        }
                    }

                    if (states) {
                        appendLine("${module.name} toggle ${module.state}")
                    }

                    if (binds) {
                        val keyName = try {
                            Keyboard.getKeyName(module.keyBind)
                        } catch (_: Exception) {
                            "NONE"
                        } ?: "NONE"
                        
                        if (keyName != "NONE" || module.keyBind >= 0) {
                            appendLine("${module.name} bind $keyName")
                        }
                    }
                }
            }
        }.trimEnd()
    }

}
