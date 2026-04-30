/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.script

import net.ccbluex.liquidbounce.FireBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.client.chat
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File


class LuaScript(val scriptFile: File) : MinecraftInstance {

    private val globals: Globals = JsePlatform.standardGlobals()
    private val scriptText = scriptFile.readText()

    // Script information
    lateinit var scriptName: String
    lateinit var scriptVersion: String
    lateinit var scriptAuthors: Array<String>

    private var state = false

    private val events = mutableMapOf<String, LuaValue>()

    private val registeredModules = mutableListOf<Module>()
    private val registeredCommands = mutableListOf<Command>()

    init {

        // 注册全局 API
        registerGlobalAPI()
    }

    private fun registerGlobalAPI() {
        // 注册全局变量和函数
        globals.set("mc", LuaValue.userdataOf(mc))
        globals.set("moduleManager", LuaValue.userdataOf(FireBounce.moduleManager))
        globals.set("commandManager", LuaValue.userdataOf(FireBounce.commandManager))
        globals.set("scriptManager", LuaValue.userdataOf(FireBounce.scriptManager))

        // 注册全局函数
        globals.set("registerScript", registerScriptFunction())
        globals.set("registerModule", registerModuleFunction())
        globals.set("registerCommand", registerCommandFunction())
        globals.set("on", onEventFunction())
        globals.set("import", importFunction())

        // 注册 Chat API
        globals.set("chat", createChatAPI())
    }

    private fun registerScriptFunction(): LuaValue {
        return object : OneArgFunction() {
            override fun call(scriptTable: LuaValue): LuaValue {
                scriptName = scriptTable.get("name").tojstring()
                scriptVersion = scriptTable.get("version").tojstring()

                // 处理 authors 数组
                val authorsTable = scriptTable.get("authors")
                scriptAuthors = if (authorsTable.istable()) {
                    val table = authorsTable.checktable()
                    (1..table.length()).map { i ->
                        table.get(i).tojstring()
                    }.toTypedArray()
                } else {
                    arrayOf(authorsTable.tojstring())
                }

                return NIL
            }
        }
    }

    private fun registerModuleFunction(): LuaValue {
        return object : TwoArgFunction() {
            override fun call(moduleTable: LuaValue, callback: LuaValue): LuaValue {
                val name = moduleTable.get("name").tojstring()
                val description = moduleTable.get("description").tojstring()
                val categoryString = moduleTable.get("category").tojstring()

                // 根据字符串匹配 Category
                val category = when(categoryString.lowercase()) {
                    "combat" -> net.ccbluex.liquidbounce.features.module.Category.COMBAT
                    "movement" -> net.ccbluex.liquidbounce.features.module.Category.MOVEMENT
                    "player" -> net.ccbluex.liquidbounce.features.module.Category.PLAYER
                    "render" -> net.ccbluex.liquidbounce.features.module.Category.RENDER
                    "world" -> net.ccbluex.liquidbounce.features.module.Category.WORLD
                    "fun" -> net.ccbluex.liquidbounce.features.module.Category.FUN
                    "misc" -> net.ccbluex.liquidbounce.features.module.Category.MISC
                    else -> net.ccbluex.liquidbounce.features.module.Category.MISC
                }

                // 创建适配 Lua 的模块 - 直接使用基础 Module 类
                val module = LuaAdaptedModule(name, category, description, moduleTable)
                FireBounce.moduleManager.registerModule(module)
                registeredModules.add(module)

                // 调用回调函数
                if (callback.isfunction()) {
                    callback.call(moduleTable, userdataOf(module))
                }

                return NIL
            }
        }
    }

    private fun registerCommandFunction(): LuaValue {
        return object : TwoArgFunction() {
            override fun call(commandTable: LuaValue, callback: LuaValue): LuaValue {
                // 创建适配 Lua 的命令
                val command = LuaAdaptedCommand(commandTable)
                FireBounce.commandManager.registerCommand(command)
                registeredCommands.add(command)

                if (callback.isfunction()) {
                    callback.call(commandTable, userdataOf(command))
                }

                return NIL
            }
        }
    }

    private fun onEventFunction(): LuaValue {
        return object : TwoArgFunction() {
            override fun call(eventName: LuaValue, handler: LuaValue): LuaValue {
                events[eventName.tojstring()] = handler
                return NIL
            }
        }
    }

    private fun importFunction(): LuaValue {
        return object : OneArgFunction() {
            override fun call(scriptPath: LuaValue): LuaValue {
                val file = File(FireBounce.scriptManager.scriptsFolder, scriptPath.tojstring())
                if (file.exists() && file.extension == "lua") {
                    globals.loadfile(file.absolutePath).call()
                }
                return NIL
            }
        }
    }

    private fun createChatAPI(): LuaValue {
        val chatTable = LuaValue.tableOf()

        chatTable.set("print", object : OneArgFunction() {
            override fun call(message: LuaValue): LuaValue {
                chat(message.tojstring())
                return NIL
            }
        })

        chatTable.set("send", object : OneArgFunction() {
            override fun call(message: LuaValue): LuaValue {
                chat(message.tojstring())
                return NIL
            }
        })

        return chatTable
    }

    fun loadScript() {
        try {
            globals.loadfile(scriptFile.absolutePath).call()
            callEvent("load")
            println("[LuaScript] Successfully loaded Lua script '$scriptName' v$scriptVersion by ${scriptAuthors.joinToString()}")
        } catch (e: Exception) {
            println("[LuaScript] Failed to load script '${scriptFile.name}': ${e.message}")
            e.printStackTrace()
        }
    }

    fun enable() {
        if (state) return
        callEvent("enable")
        state = true
        println("[LuaScript] Enabled script '$scriptName'")
    }

    fun disable() {
        if (!state) return

        // 取消注册所有模块和命令
        registeredModules.forEach { FireBounce.moduleManager.unregisterModule(it) }
        registeredCommands.forEach { FireBounce.commandManager.unregisterCommand(it) }

        callEvent("disable")
        state = false
        println("[LuaScript] Disabled script '$scriptName'")
    }

    private fun callEvent(eventName: String) {
        try {
            events[eventName]?.call()
        } catch (throwable: Throwable) {
            println("[LuaScript] Exception in script '$scriptName' during event '$eventName': ${throwable.message}")
            throwable.printStackTrace()
        }
    }

    fun isEnabled(): Boolean = state

    fun getScriptInfo(): String = "$scriptName v$scriptVersion by ${scriptAuthors.joinToString()}"

    // Lua 适配的模块类 - 直接继承基础 Module 类
    private class LuaAdaptedModule(
        name: String,
        category: net.ccbluex.liquidbounce.features.module.Category,
        description: String,
        private val luaTable: LuaValue
    ) : Module(name, category) {

        private val moduleDescription = description

        init {
            // 从 Lua 表中读取键绑定
            val keyBind = luaTable.get("keyBind")
            if (keyBind.isnumber()) {
                this.keyBind = keyBind.toint()
            }

            // 从 Lua 表中读取状态
            val state = luaTable.get("state")
            if (state.isboolean()) {
                this.state = state.toboolean()
            }

            // 设置描述（如果可能）
            try {
                val descriptionField = this::class.java.getDeclaredField("description")
                descriptionField.isAccessible = true
                descriptionField.set(this, moduleDescription)
            } catch (_: Exception) {
                // 如果无法设置描述，忽略错误
            }
        }

        override fun onEnable() {
            super.onEnable()
            // 调用 Lua 的 onEnable 函数
            val onEnableFunc = luaTable.get("onEnable")
            if (onEnableFunc.isfunction()) {
                try {
                    onEnableFunc.call()
                } catch (e: Exception) {
                    println("[LuaModule] Error in onEnable for $name: ${e.message}")
                }
            }
        }

        override fun onDisable() {
            super.onDisable()
            // 调用 Lua 的 onDisable 函数
            val onDisableFunc = luaTable.get("onDisable")
            if (onDisableFunc.isfunction()) {
                try {
                    onDisableFunc.call()
                } catch (e: Exception) {
                    println("[LuaModule] Error in onDisable for $name: ${e.message}")
                }
            }
        }

        // 重写 handleEvents 方法，返回正确的 Boolean 类型
        override fun handleEvents(): Boolean {
            val handleEventsFunc = luaTable.get("handleEvents")
            return if (handleEventsFunc.isfunction()) {
                try {
                    val result = handleEventsFunc.call()
                    result.toboolean()
                } catch (e: Exception) {
                    println("[LuaModule] Error in handleEvents for $name: ${e.message}")
                    super.handleEvents()
                }
            } else {
                super.handleEvents()
            }
        }

    }

    // Lua 适配的命令类
    private class LuaAdaptedCommand(
        private val luaTable: LuaValue
    ) : Command(
        luaTable.get("name").tojstring(),
        luaTable.get("description")?.tojstring() ?: "No description provided"
    ) {

        init {
            // 设置命令的别名
            val aliases = luaTable.get("aliases")
            if (aliases.istable()) {
                val aliasList = mutableListOf<String>()
                for (i in 1..aliases.length()) {
                    aliasList.add(aliases.get(i).tojstring())
                }
                // 尝试设置别名
                try {
                    val aliasesField = this::class.java.getDeclaredField("aliases")
                    aliasesField.isAccessible = true
                    aliasesField.set(this, aliasList.toTypedArray())
                } catch (_: Exception) {
                    // 如果无法设置别名，忽略错误
                }
            }
        }

        override fun execute(args: Array<String>) {
            val executeFunc = luaTable.get("execute")
            if (executeFunc.isfunction()) {
                try {
                    // 将参数转换为 Lua 数组
                    val argsTable = LuaValue.listOf(args.map { LuaValue.valueOf(it) }.toTypedArray())
                    executeFunc.call(LuaValue.valueOf(command), argsTable)
                } catch (e: Exception) {
                    println("[LuaCommand] Error executing command $command: ${e.message}")
                    chat("§cError executing command: ${e.message}")
                }
            } else {
                chat("§cThis command has no execute function defined.")
            }
        }

        override fun tabComplete(args: Array<String>): List<String> {
            val tabCompleteFunc = luaTable.get("tabComplete")
            return if (tabCompleteFunc.isfunction()) {
                try {
                    val argsTable = LuaValue.listOf(args.map { LuaValue.valueOf(it) }.toTypedArray())
                    val result = tabCompleteFunc.call(LuaValue.valueOf(command), argsTable)
                    if (result.istable()) {
                        (1..result.length()).map { result.get(it).tojstring() }
                    } else if (result.isstring()) {
                        listOf(result.tojstring())
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    println("[LuaCommand] Error in tabComplete for $command: ${e.message}")
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }
}