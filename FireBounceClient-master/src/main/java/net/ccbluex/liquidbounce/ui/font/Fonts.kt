/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.font

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.file.FileManager.fontsDir
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.io.extractZipTo
import net.ccbluex.liquidbounce.utils.io.readJson
import net.ccbluex.liquidbounce.utils.io.writeJson
import net.minecraft.client.gui.FontRenderer
import java.awt.Font
import java.io.File
import java.io.IOException
import kotlin.system.measureTimeMillis

data class FontInfo(val name: String, val size: Int = -1, val isCustom: Boolean = false)

data class CustomFontInfo(val name: String, val fontFile: String, val fontSize: Int)

private val FONT_REGISTRY = LinkedHashMap<FontInfo, FontRenderer>()

object Fonts : MinecraftInstance {

    /**
     * Custom Fonts
     */
    private val configFile = File(fontsDir, "fonts.json")
    private var customFontInfoList: List<CustomFontInfo>
        get() = with(configFile) {
            if (exists()) {
                try {
                    readJson().asJsonArray.map {
                        it as JsonObject
                        val fontFile = it["fontFile"].asString
                        val fontSize = it["fontSize"].asInt
                        val name = if (it.has("name")) it["name"].asString else fontFile
                        CustomFontInfo(name, fontFile, fontSize)
                    }
                } catch (e: Exception) {
                    LOGGER.error("Failed to load fonts", e)
                    emptyList()
                }
            } else {
                createNewFile()
                writeText("[]")
                emptyList()
            }
        }
        set(value) = configFile.writeJson(value)

    val minecraftFontInfo = FontInfo(name = "Minecraft Font")
    val minecraftFont: FontRenderer by lazy {
        mc.fontRendererObj
    }

    lateinit var fontExtraBold35: GameFontRenderer
    lateinit var fontExtraBold40: GameFontRenderer
    lateinit var fontSemibold35: GameFontRenderer
    lateinit var fontSemibold40: GameFontRenderer
    lateinit var fontRegular40: GameFontRenderer
    lateinit var fontRegular45: GameFontRenderer
    lateinit var fontRegular35: GameFontRenderer
    lateinit var fontRegular30: GameFontRenderer
    lateinit var fontBold180: GameFontRenderer
    lateinit var fontGoogleSans18: GameFontRenderer
    lateinit var fontGoogleSans30: GameFontRenderer
    lateinit var fontGoogleSans35: GameFontRenderer
    lateinit var fontGoogleSans40: GameFontRenderer
    lateinit var fontGoogleSans45: GameFontRenderer
    lateinit var OtherTest1: GameFontRenderer
    lateinit var OtherTest2: GameFontRenderer
    lateinit var OtherTest3: GameFontRenderer
    lateinit var Dreamscape18: GameFontRenderer
    lateinit var Dreamscape24: GameFontRenderer
    lateinit var Dreamscape36: GameFontRenderer
    lateinit var Dreamscape48: GameFontRenderer
    lateinit var Dreamscape60: GameFontRenderer
    lateinit var Bold12: GameFontRenderer
    lateinit var Bold18: GameFontRenderer
    lateinit var Bold24: GameFontRenderer
    lateinit var Bold36: GameFontRenderer
    lateinit var Bold48: GameFontRenderer

    // Mapped fonts for FDPDropdown
    val InterBold_26: GameFontRenderer by lazy { fontGoogleSans35 }
    val InterMedium_18: GameFontRenderer by lazy { fontGoogleSans30 }
    val InterBold_18: GameFontRenderer by lazy { fontGoogleSans30 }
    val InterMedium_14: GameFontRenderer by lazy { fontGoogleSans18 }

    private fun <T : FontRenderer> register(fontInfo: FontInfo, fontRenderer: T): T {
        FONT_REGISTRY[fontInfo] = fontRenderer
        return fontRenderer
    }

    fun registerCustomAWTFont(customFontInfo: CustomFontInfo, save: Boolean = true): GameFontRenderer? {
        val font = getFontFromFileOrNull(customFontInfo.fontFile, customFontInfo.fontSize) ?: return null

        val result = register(
            FontInfo(customFontInfo.name, customFontInfo.fontSize, isCustom = true),
            font.asGameFontRenderer()
        )

        if (save) {
            customFontInfoList += customFontInfo
        }

        return result
    }

    fun loadFonts() {
        LOGGER.info("Start to load fonts.")
        val time = measureTimeMillis {
            downloadFonts()
            LOGGER.info("Start to load fonts from file.")

            register(minecraftFontInfo, minecraftFont)

            val defaultFallbackFont = Font("Arial", Font.PLAIN, 30).asGameFontRenderer()

            OtherTest1 = try {
                register(
                    FontInfo(name = "Test1", size = 30),
                    getFontFromFile("1.ttf", 30).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 30), defaultFallbackFont)
            }

            OtherTest2 = try {
                register(
                    FontInfo(name = "Test2", size = 30),
                    getFontFromFile("2.ttf", 30).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 30), defaultFallbackFont)
            }

            OtherTest3 = try {
                register(
                    FontInfo(name = "Test3", size = 30),
                    getFontFromFile("3.ttf", 30).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 30), defaultFallbackFont)
            }

            fontRegular30 = try {
                register(
                    FontInfo(name = "Outfit Regular", size = 30),
                    getFontFromFile("Outfit-Regular.ttf", 30).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 30), defaultFallbackFont)
            }

            fontSemibold35 = try {
                register(
                    FontInfo(name = "Outfit Semibold", size = 35),
                    getFontFromFile("Outfit-Semibold.ttf", 35).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 35), defaultFallbackFont)
            }

            fontRegular35 = try {
                register(
                    FontInfo(name = "Outfit Regular", size = 35),
                    getFontFromFile("Outfit-Regular.ttf", 35).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 35), defaultFallbackFont)
            }

            fontRegular40 = try {
                register(
                    FontInfo(name = "Outfit Regular", size = 40),
                    getFontFromFile("Outfit-Regular.ttf", 40).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 40), defaultFallbackFont)
            }

            fontSemibold40 = try {
                register(
                    FontInfo(name = "Outfit Semibold", size = 40),
                    getFontFromFile("Outfit-Semibold.ttf", 40).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 40), defaultFallbackFont)
            }

            fontRegular45 = try {
                register(
                    FontInfo(name = "Outfit Regular", size = 45),
                    getFontFromFile("Outfit-Regular.ttf", 45).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 45), defaultFallbackFont)
            }

            fontExtraBold35 = try {
                register(
                    FontInfo(name = "Outfit Extrabold", size = 35),
                    getFontFromFile("Outfit-Extrabold.ttf", 35).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 35), defaultFallbackFont)
            }

            fontExtraBold40 = try {
                register(
                    FontInfo(name = "Outfit Extrabold", size = 40),
                    getFontFromFile("Outfit-Extrabold.ttf", 40).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 40), defaultFallbackFont)
            }

            fontBold180 = try {
                register(
                    FontInfo(name = "Outfit Bold", size = 180),
                    getFontFromFile("Outfit-Bold.ttf", 180).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 180), defaultFallbackFont)
            }

            fontGoogleSans18 = try {
                register(
                    FontInfo(name = "Google Sans", size = 18),
                    getFontFromFile2("ProductSans-Bold.ttf", 18).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 18), defaultFallbackFont)
            }

            fontGoogleSans30 = try {
                register(
                    FontInfo(name = "Google Sans", size = 30),
                    getFontFromFile2("ProductSans-Bold.ttf", 30).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 30), defaultFallbackFont)
            }

            fontGoogleSans35 = try {
                register(
                    FontInfo(name = "Google Sans", size = 35),
                    getFontFromFile2("ProductSans-Bold.ttf", 35).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 35), defaultFallbackFont)
            }

            fontGoogleSans40 = try {
                register(
                    FontInfo(name = "Google Sans", size = 40),
                    getFontFromFile2("ProductSans-Bold.ttf", 40).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 40), defaultFallbackFont)
            }

            fontGoogleSans45 = try {
                register(
                    FontInfo(name = "Google Sans", size = 45),
                    getFontFromFile2("ProductSans-Bold.ttf", 45).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 45), defaultFallbackFont)
            }

            Dreamscape18 = try {
                register(
                    FontInfo(name = "Dreamscape", size = 18),
                    getFontFromFile2("Dreamscape.ttf", 18).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 18), defaultFallbackFont)
            }

            Dreamscape24 = try {
                register(
                    FontInfo(name = "Dreamscape", size = 24),
                    getFontFromFile2("Dreamscape.ttf", 24).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 24), defaultFallbackFont)
            }

            Dreamscape36 = try {
                register(
                    FontInfo(name = "Dreamscape", size = 36),
                    getFontFromFile2("Dreamscape.ttf", 36).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 36), defaultFallbackFont)
            }

            Dreamscape48 = try {
                register(
                    FontInfo(name = "Dreamscape", size = 48),
                    getFontFromFile2("Dreamscape.ttf", 48).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 48), defaultFallbackFont)
            }

            Dreamscape60 = try {
                register(
                    FontInfo(name = "Dreamscape", size = 60),
                    getFontFromFile2("Dreamscape.ttf", 60).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 60), defaultFallbackFont)
            }

            Bold12 = try {
                register(
                    FontInfo(name = "Bold", size = 12),
                    getFontFromFile2("Bold.ttf", 12).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 12), defaultFallbackFont)
            }

            Bold18 = try {
                register(
                    FontInfo(name = "Bold", size = 18),
                    getFontFromFile2("Bold.ttf", 18).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 18), defaultFallbackFont)
            }

            Bold24 = try {
                register(
                    FontInfo(name = "Bold", size = 24),
                    getFontFromFile2("Bold.ttf", 24).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 24), defaultFallbackFont)
            }

            Bold36 = try {
                register(
                    FontInfo(name = "Bold", size = 36),
                    getFontFromFile2("Bold.ttf", 36).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 36), defaultFallbackFont)
            }

            Bold48 = try {
                register(
                    FontInfo(name = "Bold", size = 48),
                    getFontFromFile2("Bold.ttf", 48).asGameFontRenderer()
                )
            } catch (_: Exception) {
                register(FontInfo(name = "Default", size = 48), defaultFallbackFont)
            }

            loadCustomFonts()
        }
        LOGGER.info("Loaded ${FONT_REGISTRY.size} fonts in ${time}ms")
    }

    private fun loadCustomFonts() {
        FONT_REGISTRY.keys.removeIf { it.isCustom }

        customFontInfoList.forEach {
            registerCustomAWTFont(it, save = false)
        }
    }

    fun downloadFonts() {
        fontsDir.mkdirs()
        val outputFile = File(fontsDir, "outfit.zip")
        if (!outputFile.exists()) {
            LOGGER.info("Downloading fonts...")
            val localResource = "/assets/minecraft/liquidbounce/fonts/outfit.zip"
            runCatching {
                javaClass.getResourceAsStream(localResource)?.use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                        LOGGER.info("Successfully copied local fonts")
                    }
                } ?: throw IOException("Local font resource not found")
            }.onFailure { e ->
                LOGGER.error("Failed to download fonts", e)
                return
            }
            LOGGER.info("Extracting fonts...")
            outputFile.extractZipTo(fontsDir)
        }

        val otherFonts = listOf(
            "ProductSans-Bold.ttf", "Dreamscape.ttf", "Bold.ttf",
            "1.ttf", "2.ttf", "3.ttf"
        )

        otherFonts.forEach { fontName ->
            val fontFile = File(fontsDir, fontName)
            if (!fontFile.exists()) {
                try {
                    val resourcePath = "/assets/minecraft/liquidbounce/fonts/$fontName"
                    javaClass.getResourceAsStream(resourcePath)?.use { input ->
                        fontFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    LOGGER.warn("Failed to copy font $fontName: ${e.message}")
                }
            }
        }
    }

    fun getFontRenderer(name: String, size: Int): FontRenderer {
        return FONT_REGISTRY.entries.firstOrNull { (fontInfo, _) ->
            fontInfo.size == size && fontInfo.name.equals(name, true)
        }?.value ?: minecraftFont
    }

    fun getFontDetails(fontRenderer: FontRenderer): FontInfo? {
        return FONT_REGISTRY.keys.firstOrNull { FONT_REGISTRY[it] == fontRenderer }
    }

    val fonts: List<FontRenderer>
        get() = FONT_REGISTRY.values.toList()

    val customFonts: Map<FontInfo, FontRenderer>
        get() = FONT_REGISTRY.filterKeys { it.isCustom }

    fun removeCustomFont(fontInfo: FontInfo): CustomFontInfo? {
        if (!fontInfo.isCustom) {
            return null
        }

        FONT_REGISTRY.remove(fontInfo)
        return customFontInfoList.firstOrNull {
            it.name == fontInfo.name && it.fontSize == fontInfo.size
        }?.also {
            customFontInfoList -= it
        }
    }

    private fun getFontFromFileOrNull(file: String, size: Int): Font? = try {
        val fontFile = File(fontsDir, file)
        if (!fontFile.exists()) {
            val resourcePath = "/assets/minecraft/liquidbounce/fonts/$file"
            javaClass.getResourceAsStream(resourcePath)?.use { inputStream ->
                val tempFile = File.createTempFile("font", ".ttf")
                tempFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                Font.createFont(Font.TRUETYPE_FONT, tempFile).deriveFont(Font.PLAIN, size.toFloat())
            } ?: Font.decode("Arial-$size")
        } else {
            fontFile.inputStream().use { inputStream ->
                Font.createFont(Font.TRUETYPE_FONT, inputStream).deriveFont(Font.PLAIN, size.toFloat())
            }
        }
    } catch (e: Exception) {
        LOGGER.warn("Exception during loading font[name=${file}, size=${size}]", e)
        Font.decode("Arial-$size")
    }

    private fun getFontFromFileOrNull2(file: String, size: Int): Font? = try {
        val resourcePath = "/assets/minecraft/liquidbounce/fonts/$file"
        val inputStream = Fonts::class.java.getResourceAsStream(resourcePath)
            ?: File(fontsDir, file).inputStream()

        inputStream.use {
            Font.createFont(Font.TRUETYPE_FONT, it).deriveFont(Font.PLAIN, size.toFloat())
        }
    } catch (e: Exception) {
        LOGGER.warn("Exception during loading font[name=${file}, size=${size}]", e)
        Font.decode("Arial-$size")
    }

    private fun getFontFromFile(file: String, size: Int): Font =
        getFontFromFileOrNull(file, size) ?: Font("Arial", Font.PLAIN, size)

    private fun getFontFromFile2(file: String, size: Int): Font =
        getFontFromFileOrNull2(file, size)?: Font("Arial", Font.PLAIN, size)

    private fun Font.asGameFontRenderer(): GameFontRenderer {
        return GameFontRenderer(this@asGameFontRenderer)
    }
    fun getFont(fontName: String, size: Int) =
        try {
            val fontFile = File(fontsDir, fontName)
            val inputStream = if (fontFile.exists()) {
                fontFile.inputStream()
            } else {
                val resourcePath = "/assets/minecraft/liquidbounce/fonts/$fontName"
                Fonts::class.java.getResourceAsStream(resourcePath)
            }

            if (inputStream != null) {
                var awtClientFont = Font.createFont(Font.TRUETYPE_FONT, inputStream)
                awtClientFont = awtClientFont.deriveFont(Font.PLAIN, size.toFloat())
                inputStream.close()
                awtClientFont
            } else {
                Font("Arial", Font.PLAIN, size)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Font("Arial", Font.PLAIN, size)
        }!!
}