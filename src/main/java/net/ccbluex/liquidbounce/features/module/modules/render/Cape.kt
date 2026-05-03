package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.ListValue
import net.ccbluex.liquidbounce.config.choices
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.randomNumber
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.ResourceLocation
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipFile
import javax.imageio.ImageIO

object Cape : Module("Cape", Category.RENDER) {

    private val capeFolder = File(FileManager.dir, "capes")
    private val capeMap = mutableMapOf<String, ResourceLocation>()

    private val capeModeValue: ListValue

    val capeMode: String
        get() = capeModeValue.get()

    init {
        extractCapesFromJar()
        val capeNames = scanAndLoadCapes()
        capeModeValue = choices("Cape", capeNames.toTypedArray(), capeNames.first())
    }

    private fun extractCapesFromJar() {
        try {
            capeFolder.mkdirs()
            
            val jarPath = javaClass.protectionDomain.codeSource.location.path
            if (!jarPath.endsWith(".jar")) {
                copyCapesFromResources()
                return
            }

            val zipFile = ZipFile(jarPath)
            val entries = zipFile.entries()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                if (name.startsWith("assets/minecraft/airclient/cape/") && name.endsWith(".png")) {
                    val fileName = File(name).name
                    val targetFile = File(capeFolder, fileName)
                    
                    if (!targetFile.exists()) {
                        zipFile.getInputStream(entry).use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
            zipFile.close()
        } catch (e: Exception) {
            copyCapesFromResources()
        }
    }

    private fun copyCapesFromResources() {
        try {
            capeFolder.mkdirs()
            
            val capeDir = File(javaClass.classLoader.getResource("assets/minecraft/airclient/cape")?.file ?: return)
            if (capeDir.exists() && capeDir.isDirectory) {
                capeDir.listFiles { file -> file.extension.equals("png", ignoreCase = true) }?.forEach { file ->
                    val targetFile = File(capeFolder, file.name)
                    if (!targetFile.exists()) {
                        try {
                            file.copyTo(targetFile, overwrite = false)
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        } catch (e: Exception) {
            try {
                val knownCapes = listOf(
                    "cape1.png", "cape2.png", "cape3.png", "cape4.png",
                    "astolfo.png", "ravenanime.png", "ravenxd.png",
                    "Augustus.png", "Astolfotrap.png",
                    "rainbow.png",
                    "gradient_blue.png", "gradient_purple.png", "gradient_pink.png",
                    "gradient_green.png", "gradient_orange.png", "gradient_red.png",
                    "gradient_cyan.png", "gradient_yellow.png",
                    "gradient_horizontal_blue.png", "gradient_horizontal_purple.png", "gradient_horizontal_pink.png",
                    "solid_black.png", "solid_white.png", "solid_red.png", "solid_blue.png",
                    "solid_green.png", "solid_purple.png", "solid_pink.png", "solid_orange.png",
                    "solid_cyan.png", "solid_yellow.png", "solid_darkblue.png", "solid_darkgreen.png", "solid_darkred.png",
                    "striped_red_blue.png", "striped_black_white.png", "striped_purple_yellow.png",
                    "striped_green_white.png", "striped_orange_black.png", "striped_pink_cyan.png",
                    "checker_black_white.png", "checker_red_black.png", "checker_blue_white.png",
                    "checker_purple_pink.png", "checker_green_black.png",
                    "diagonal_blue_red.png", "diagonal_green_black.png", "diagonal_purple_cyan.png", "diagonal_orange_pink.png",
                    "fade_black.png", "fade_red.png", "fade_blue.png", "fade_green.png", "fade_purple.png", "fade_orange.png",
                    "neon_blue.png", "neon_pink.png", "neon_green.png", "neon_orange.png", "neon_cyan.png", "neon_yellow.png",
                    "tricolor_flag.png", "tricolor_germany.png", "tricolor_italy.png", "tricolor_rainbow.png", "tricolor_ocean.png",
                    "half_black_white.png", "half_red_blue.png", "half_green_purple.png", "half_orange_pink.png", "half_cyan_yellow.png"
                )

                for (capeName in knownCapes) {
                    val targetFile = File(capeFolder, capeName)
                    if (!targetFile.exists()) {
                        try {
                            val resource = javaClass.classLoader.getResourceAsStream("assets/minecraft/airclient/cape/$capeName")
                            if (resource != null) {
                                FileOutputStream(targetFile).use { output ->
                                    resource.copyTo(output)
                                }
                                resource.close()
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun scanAndLoadCapes(): MutableList<String> {
        val capeNames = mutableListOf("None")
        capeMap.clear()

        if (capeFolder.exists() && capeFolder.isDirectory) {
            val files = capeFolder.listFiles { file -> file.extension.equals("png", ignoreCase = true) }
            files?.sortedBy { it.nameWithoutExtension }?.forEach { file ->
                try {
                    val name = file.nameWithoutExtension
                    val bufferedImage = ImageIO.read(FileInputStream(file))
                    if (bufferedImage != null) {
                        val resourceLocation = ResourceLocation("airclient/capes/${randomNumber(16)}")
                        mc.textureManager.loadTexture(resourceLocation, DynamicTexture(bufferedImage))
                        capeNames.add(name)
                        capeMap[name] = resourceLocation
                    }
                } catch (e: Exception) {
                }
            }
        }

        return capeNames
    }

    fun getCapeForPlayer(uuid: UUID): ResourceLocation? {
        if (!state) return null
        
        val currentPlayer = mc.thePlayer ?: return null
        
        if (uuid != currentPlayer.uniqueID) return null
        
        return capeMap[capeMode]
    }

    override val tag
        get() = capeMode

    fun refreshCapes() {
        extractCapesFromJar()
        val capeNames = scanAndLoadCapes()
        capeModeValue.updateValues(capeNames.toTypedArray())
    }
}
