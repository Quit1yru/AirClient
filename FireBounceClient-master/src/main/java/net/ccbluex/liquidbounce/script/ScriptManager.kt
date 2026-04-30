/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.script

import net.ccbluex.liquidbounce.file.FileManager.dir
import net.ccbluex.liquidbounce.script.ScriptManager.scriptsFolder
import net.ccbluex.liquidbounce.script.remapper.Remapper
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import java.io.File
import java.io.FileFilter

private val javascriptScripts = mutableListOf<Script>()
private val luaScripts = mutableListOf<LuaScript>()

object ScriptManager {

    val scriptsFolder = File(dir, "scripts")

    private val JS_FILE_FILTER = FileFilter {
        it.extension.lowercase() == "js"
    }

    private val LUA_FILE_FILTER = FileFilter {
        it.extension.lowercase() == "lua"
    }

    /**
     * Only includes JavaScript files in the root directory ([scriptsFolder])
     */
    val availableJavaScriptFiles: Array<File>
        get() = scriptsFolder.listFiles(JS_FILE_FILTER) ?: emptyArray()

    /**
     * Only includes Lua files in the root directory ([scriptsFolder])
     */
    val availableLuaFiles: Array<File>
        get() = scriptsFolder.listFiles(LUA_FILE_FILTER) ?: emptyArray()

    /**
     * Get all scripts (both JavaScript and Lua)
     */
    fun getAllScripts(): List<Any> = javascriptScripts + luaScripts

    /**
     * Get only JavaScript scripts
     */
    fun getJavaScriptScripts(): List<Script> = javascriptScripts.toList()

    /**
     * Get only Lua scripts
     */
    fun getLuaScripts(): List<LuaScript> = luaScripts.toList()

    /**
     * Loads all scripts inside the scripts folder.
     */
    fun loadScripts() {
        if (!scriptsFolder.exists())
            scriptsFolder.mkdir()

        // Load JavaScript scripts
        availableJavaScriptFiles.forEach(::loadJavaScriptScript)

        // Load Lua scripts
        availableLuaFiles.forEach(::loadLuaScript)
    }

    /**
     * Unloads all scripts.
     */
    fun unloadScripts() {
        javascriptScripts.clear()
        luaScripts.clear()
    }

    /**
     * Unloads all scripts and disables them first
     */
    fun unloadAllScripts() {
        disableScripts()
        unloadScripts()
    }

    /**
     * Loads a JavaScript script from a file.
     */
    fun loadJavaScriptScript(scriptFile: File) {
        try {
            if (!Remapper.mappingsLoaded) {
                error("The mappings were not loaded, re-start and check your internet connection.")
            }

            val script = Script(scriptFile)
            script.initScript()
            javascriptScripts += script
            LOGGER.info("[ScriptAPI] Successfully loaded JavaScript script '${scriptFile.name}'.")
        } catch (t: Throwable) {
            LOGGER.error("[ScriptAPI] Failed to load JavaScript script '${scriptFile.name}'.", t)
        }
    }

    /**
     * Loads a Lua script from a file.
     */
    fun loadLuaScript(scriptFile: File) {
        try {
            if (!Remapper.mappingsLoaded) {
                error("The mappings were not loaded, re-start and check your internet connection.")
            }

            val script = LuaScript(scriptFile)
            script.loadScript()
            luaScripts += script
            LOGGER.info("[ScriptAPI] Successfully loaded Lua script '${scriptFile.name}'.")
        } catch (t: Throwable) {
            LOGGER.error("[ScriptAPI] Failed to load Lua script '${scriptFile.name}'.", t)
        }
    }

    /**
     * Loads a script from a file (auto-detect type by extension)
     */
    fun loadScript(scriptFile: File) {
        when (scriptFile.extension.lowercase()) {
            "js" -> loadJavaScriptScript(scriptFile)
            "lua" -> loadLuaScript(scriptFile)
            else -> LOGGER.error("[ScriptAPI] Unsupported script type: ${scriptFile.extension}")
        }
    }

    /**
     * Enables all scripts.
     */
    fun enableScripts() {
        javascriptScripts.forEach { it.onEnable() }
        luaScripts.forEach { it.enable() }
    }

    /**
     * Disables all scripts.
     */
    fun disableScripts() {
        javascriptScripts.forEach { it.onDisable() }
        luaScripts.forEach { it.disable() }
    }

    /**
     * Enables a specific script by name
     */
    fun enableScript(scriptName: String): Boolean {
        val jsScript = javascriptScripts.find { it.scriptName == scriptName }
        if (jsScript != null) {
            jsScript.onEnable()
            return true
        }

        val luaScript = luaScripts.find { it.scriptName == scriptName }
        if (luaScript != null) {
            luaScript.enable()
            return true
        }

        return false
    }

    /**
     * Disables a specific script by name
     */
    fun disableScript(scriptName: String): Boolean {
        val jsScript = javascriptScripts.find { it.scriptName == scriptName }
        if (jsScript != null) {
            jsScript.onDisable()
            return true
        }

        val luaScript = luaScripts.find { it.scriptName == scriptName }
        if (luaScript != null) {
            luaScript.disable()
            return true
        }

        return false
    }

    /**
     * Imports a script.
     * @param file Script file to be imported.
     */
    fun importScript(file: File) {
        val scriptFile = File(scriptsFolder, file.name)
        file.copyTo(scriptFile)

        loadScript(scriptFile)
        LOGGER.info("[ScriptAPI] Successfully imported script '${scriptFile.name}'.")
    }

    /**
     * Deletes a script.
     * @param script Script to be deleted.
     */
    fun deleteScript(script: Any) {
        when (script) {
            is Script -> {
                script.onDisable()
                javascriptScripts.remove(script)
                script.scriptFile.delete()
            }
            is LuaScript -> {
                script.disable()
                luaScripts.remove(script)
                script.scriptFile.delete()
            }
        }
        LOGGER.info("[ScriptAPI] Successfully deleted script.")
    }

    /**
     * Reloads all scripts.
     */
    fun reloadScripts() {
        disableScripts()
        unloadScripts()
        loadScripts()
        enableScripts()

        LOGGER.info("[ScriptAPI] Successfully reloaded scripts.")
    }

    /**
     * Gets script information for display
     */
    fun getScriptInfo(): List<String> {
        val info = mutableListOf<String>()

        if (javascriptScripts.isNotEmpty()) {
            info.add("=== JavaScript Scripts ===")
            javascriptScripts.forEach { script ->
                info.add("${script.scriptName} v${script.scriptVersion} by ${script.scriptAuthors.joinToString()}")
            }
        }

        if (luaScripts.isNotEmpty()) {
            info.add("=== Lua Scripts ===")
            luaScripts.forEach { script ->
                info.add("${script.scriptName} v${script.scriptVersion} by ${script.scriptAuthors.joinToString()}")
            }
        }

        if (info.isEmpty()) {
            info.add("No scripts loaded")
        }

        return info
    }
}