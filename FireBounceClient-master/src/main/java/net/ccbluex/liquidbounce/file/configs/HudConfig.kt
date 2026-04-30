/*
 * FireBounce Hacked Client
 */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.FontValue
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.ui.client.hud.HUD
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.utils.client.ClientUtils
import net.ccbluex.liquidbounce.utils.io.json
import net.ccbluex.liquidbounce.utils.io.jsonArray
import net.ccbluex.liquidbounce.utils.io.readJson
import net.ccbluex.liquidbounce.utils.io.writeJson
import java.io.File
import java.io.IOException

class HudConfig(file: File) : FileConfig(file) {

    // HUD 多配置文件夹
    private val hudConfigsDir = File(file.parentFile, "hud-configs")

    // 当前配置文件名（从文件加载）
    private var currentConfig = loadCurrentConfigName()

    override fun loadDefault() = HUD.setDefault()

    /**
     * 从文件加载当前配置名
     */
    private fun loadCurrentConfigName(): String {
        val configFile = File(hudConfigsDir, ".current")
        return if (configFile.exists()) {
            configFile.readText().trim()
        } else {
            "default"
        }
    }

    /**
     * 保存当前配置名到文件
     */
    private fun saveCurrentConfigName() {
        if (!hudConfigsDir.exists()) {
            hudConfigsDir.mkdirs()
        }
        val configFile = File(hudConfigsDir, ".current")
        configFile.writeText(currentConfig)
    }

    /**
     * 获取当前配置文件
     */
    private fun getCurrentConfigFile(): File {
        return File(hudConfigsDir, "$currentConfig.json")
    }

    /**
     * 切换到指定配置并加载
     */
    fun switchConfig(configName: String): Boolean {
        val configFile = File(hudConfigsDir, "$configName.json")
        if (!configFile.exists()) {
            return false
        }

        currentConfig = configName
        saveCurrentConfigName() // 保存到文件
        loadConfig() // 立即加载新配置
        return true
    }

    /**
     * 创建新配置（基于当前HUD状态）
     */
    fun createConfig(configName: String): Boolean {
        val configFile = File(hudConfigsDir, "$configName.json")
        if (configFile.exists()) {
            return false
        }

        // 确保文件夹存在
        if (!hudConfigsDir.exists()) {
            hudConfigsDir.mkdirs()
        }

        // 切换到新配置并保存当前状态
        val previousConfig = currentConfig
        currentConfig = configName
        saveConfig()
        saveCurrentConfigName()

        // 恢复之前的配置
        currentConfig = previousConfig
        saveCurrentConfigName()
        return true
    }

    /**
     * 删除配置
     */
    fun deleteConfig(configName: String): Boolean {
        val configFile = File(hudConfigsDir, "$configName.json")
        if (!configFile.exists() || configName == "default") {
            return false
        }

        // 如果删除的是当前配置，切换到default
        if (configName == currentConfig) {
            currentConfig = "default"
            saveCurrentConfigName()
            loadConfig() // 加载默认配置
        }

        return configFile.delete()
    }

    /**
     * 获取所有可用的配置名
     */
    fun getAvailableConfigs(): List<String> {
        if (!hudConfigsDir.exists()) {
            return listOf("default")
        }

        return hudConfigsDir.listFiles { file ->
            file.isFile && file.extension.equals("json", ignoreCase = true) && file.name != ".current"
        }?.map { it.nameWithoutExtension } ?: listOf("default")
    }

    /**
     * 获取当前配置名
     */
    fun getCurrentConfigName(): String = currentConfig

    /**
     * 保存当前配置（使用指定名称）
     */
    fun saveAsConfig(configName: String): Boolean {
        val previousConfig = currentConfig
        currentConfig = configName
        saveConfig()
        saveCurrentConfigName()
        currentConfig = previousConfig
        saveCurrentConfigName()
        return true
    }

    /**
     * 重命名配置
     */
    fun renameConfig(oldName: String, newName: String): Boolean {
        val oldFile = File(hudConfigsDir, "$oldName.json")
        val newFile = File(hudConfigsDir, "$newName.json")

        if (!oldFile.exists() || newFile.exists()) {
            return false
        }

        val success = oldFile.renameTo(newFile)

        // 如果重命名的是当前配置，更新当前配置名
        if (success && oldName == currentConfig) {
            currentConfig = newName
            saveCurrentConfigName()
        }

        return success
    }

    /**
     * Load config from current config file
     */
    @Throws(IOException::class)
    override fun loadConfig() {
        val configFile = getCurrentConfigFile()

        if (!configFile.exists()) {
            // 如果当前配置不存在，加载默认配置并保存
            HUD.setDefault()
            saveConfig()
            return
        }

        val jsonArray = configFile.readJson() as? JsonArray ?: return

        HUD.clearElements()

        try {
            for (jsonObject in jsonArray) {
                if (jsonObject !is JsonObject) continue
                if (!jsonObject.has("Type")) continue

                val type = jsonObject["Type"].asString
                val elementClass = HUD.ELEMENTS.entries.find { it.value.name == type }?.key

                if (elementClass == null) {
                    ClientUtils.LOGGER.warn("Unrecognized HUD element: '$type'")
                    continue
                }

                val element = elementClass.newInstance()

                element.x = jsonObject["X"].asDouble
                element.y = jsonObject["Y"].asDouble
                element.scale = jsonObject["Scale"].asFloat
                element.side = Side(
                    Side.Horizontal.getByName(jsonObject["HorizontalFacing"].asString) ?: Side.Horizontal.RIGHT,
                    Side.Vertical.getByName(jsonObject["VerticalFacing"].asString) ?: Side.Vertical.UP
                )

                for (value in element.values) {
                    if (jsonObject.has(value.name))
                        value.fromJson(jsonObject[value.name])
                }

                // Support for old HUD files
                if (jsonObject.has("font"))
                    element.values.find { it is FontValue }?.fromJson(jsonObject["font"])

                HUD.addElement(element)
            }

            // Add forced elements when missing
            for ((elementClass, info) in HUD.ELEMENTS) {
                if (info.force && HUD.elements.none { it.javaClass == elementClass }) {
                    HUD.addElement(elementClass.newInstance())
                }
            }

            ClientUtils.LOGGER.info("Loaded HUD config: $currentConfig")
        } catch (e: Exception) {
            ClientUtils.LOGGER.error("Error while loading HUD config: ${configFile.name}", e)
            HUD.setDefault()
        }
    }

    /**
     * Save config to current config file
     */
    @Throws(IOException::class)
    override fun saveConfig() {
        // 确保配置文件夹存在
        if (!hudConfigsDir.exists()) {
            hudConfigsDir.mkdirs()
        }

        val configFile = getCurrentConfigFile()

        val jsonArray = jsonArray {
            for (element in HUD.elements) {
                +json {
                    "Type" to element.name
                    "X" to element.x
                    "Y" to element.y
                    "Scale" to element.scale
                    "HorizontalFacing" to element.side.horizontal.sideName
                    "VerticalFacing" to element.side.vertical.sideName

                    element.values.forEach {
                        it.name to it.toJson()
                    }
                }
            }
        }

        configFile.writeJson(jsonArray)
        ClientUtils.LOGGER.info("Saved HUD config: $currentConfig")
    }

    override fun hasConfig(): Boolean {
        return hudConfigsDir.exists() && getCurrentConfigFile().exists()
    }
}