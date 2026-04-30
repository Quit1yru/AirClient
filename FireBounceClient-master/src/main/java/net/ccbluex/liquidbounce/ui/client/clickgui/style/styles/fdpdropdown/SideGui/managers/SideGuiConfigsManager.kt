/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.SideGui.managers

import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.FireBounce.fileManager
import net.ccbluex.liquidbounce.api.ClientApi
import net.ccbluex.liquidbounce.config.SettingsUtils.applyScript
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.objects.Drag
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.render.DrRenderUtils
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.client.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.kotlin.SharedScopes
import org.lwjgl.input.Mouse
import java.awt.Color
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files

object SideGuiConfigsManager {
    data class OnlineConfig(val name: String, val settingId: String)
    var autoSettingsList: List<OnlineConfig>? = null
    var showLocalConfigs = false
    var wasMousePressed = false
    private var isFetching = false
    private var scrollOffset = 0f
    private var lastMouseY = 0

    fun drawConfigsCategory(mouseX: Int, mouseY: Int, alpha: Int, drag: Drag, rectWidth: Float) {
        if (autoSettingsList == null && !isFetching) {
            isFetching = true
            SharedScopes.IO.launch {
                try {
                    val settings = ClientApi.getSettingsList()
                    autoSettingsList = settings.map { OnlineConfig(it.name, it.settingId) }
                } catch (e: Exception) {
                    autoSettingsList = null
                } finally {
                    isFetching = false
                }
            }
        }

        val buttonToggleWidth = 70f
        val buttonToggleHeight = 20f
        val buttonSpacing = 10f

        val xStart = drag.x + 25
        val openFolderButtonWidth = buttonToggleWidth * 2
        val openFolderButtonX = xStart
        val openFolderButtonY = drag.y + 30
        val isOpenFolderHovered = DrRenderUtils.isHovering(
            openFolderButtonX, openFolderButtonY,
            openFolderButtonWidth, buttonToggleHeight,
            mouseX, mouseY
        )
        val openFolderButtonColor = if (isOpenFolderHovered) Color(70, 70, 70, alpha).rgb else Color(50, 50, 50, alpha).rgb
        DrRenderUtils.drawRect2(
            openFolderButtonX.toDouble(),
            openFolderButtonY.toDouble(),
            openFolderButtonWidth.toDouble(),
            buttonToggleHeight.toDouble(),
            openFolderButtonColor
        )
        Fonts.InterBold_26.drawString("OPEN FOLDER", openFolderButtonX + 10, openFolderButtonY + 5, DrRenderUtils.applyOpacity(-1, alpha / 255f))

        val onlineButtonX = xStart
        val onlineButtonY = openFolderButtonY + buttonToggleHeight + buttonSpacing
        val isOnlineHovered = DrRenderUtils.isHovering(onlineButtonX, onlineButtonY, buttonToggleWidth, buttonToggleHeight, mouseX, mouseY)
        val onlineButtonColor = when {
            !showLocalConfigs -> Color(100, 150, 100, alpha).rgb
            isOnlineHovered   -> Color(70, 70, 70, alpha).rgb
            else              -> Color(50, 50, 50, alpha).rgb
        }
        DrRenderUtils.drawRect2(
            onlineButtonX.toDouble(),
            onlineButtonY.toDouble(),
            buttonToggleWidth.toDouble(),
            buttonToggleHeight.toDouble(),
            onlineButtonColor
        )
        Fonts.InterBold_26.drawString("ONLINE", onlineButtonX + 10, onlineButtonY + 5, DrRenderUtils.applyOpacity(-1, alpha / 255f))

        val localButtonX = onlineButtonX + buttonToggleWidth + buttonSpacing
        val localButtonY = onlineButtonY
        val isLocalHovered = DrRenderUtils.isHovering(localButtonX, localButtonY, buttonToggleWidth, buttonToggleHeight, mouseX, mouseY)
        val localButtonColor = when {
            showLocalConfigs -> Color(100, 150, 100, alpha).rgb
            isLocalHovered   -> Color(70, 70, 70, alpha).rgb
            else             -> Color(50, 50, 50, alpha).rgb
        }
        DrRenderUtils.drawRect2(
            localButtonX.toDouble(),
            localButtonY.toDouble(),
            buttonToggleWidth.toDouble(),
            buttonToggleHeight.toDouble(),
            localButtonColor
        )
        Fonts.InterBold_26.drawString("LOCAL", localButtonX + 10, localButtonY + 5, DrRenderUtils.applyOpacity(-1, alpha / 255f))

        if (!wasMousePressed && Mouse.isButtonDown(0)) {
            when {
                isOpenFolderHovered -> openFolder()
                isOnlineHovered     -> showLocalConfigs = false
                isLocalHovered      -> showLocalConfigs = true
            }
            wasMousePressed = true
        }
        if (!Mouse.isButtonDown(0)) wasMousePressed = false

        // Handle scrolling
        val configListY = localButtonY + buttonToggleHeight + buttonSpacing
        val configListHeight = 290f // Approximate remaining space in the window
        val isMouseOverConfigList = mouseX >= drag.x + 20 && mouseX <= drag.x + rectWidth - 20 &&
                                   mouseY >= configListY && mouseY <= configListY + configListHeight

        if (isMouseOverConfigList && Mouse.hasWheel()) {
            val wheel = Mouse.getDWheel()
            if (wheel != 0) {
                scrollOffset += if (wheel > 0) -20f else 20f
                // Limit scrolling to prevent going too far negative
                scrollOffset = scrollOffset.coerceAtMost(0f)
            }
        }

        drawConfigList(mouseX, mouseY, alpha, configListY, drag, rectWidth)
    }

    private fun drawConfigList(mouseX: Int, mouseY: Int, alpha: Int, startY: Float, drag: Drag, rectWidth: Float) {
        var configX = drag.x + 25
        var configY = startY + scrollOffset
        val buttonWidth = (rectWidth - 50) / 4 - 10
        val buttonHeight = 20f
        val configsPerRow = 4
        var configCount = 0

        // Enable scissor test to limit drawing to the config area
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST)
        val scaledRes = net.minecraft.client.gui.ScaledResolution(net.ccbluex.liquidbounce.utils.client.MinecraftInstance.mc)
        val scale = scaledRes.scaleFactor.toFloat()
        val scissorX = (drag.x + 20) * scale
        val scissorY = (startY - 5) * scale
        val scissorWidth = (rectWidth - 40) * scale
        val scissorHeight = 295f * scale  // Approximate remaining height
        org.lwjgl.opengl.GL11.glScissor(scissorX.toInt(), scaledRes.scaledHeight * scale.toInt() - (scissorY + scissorHeight).toInt(), scissorWidth.toInt(), scissorHeight.toInt())

        if (showLocalConfigs) {
            val localConfigs = fileManager.settingsDir.listFiles { _, name -> name.endsWith(".txt") }
            if (!localConfigs.isNullOrEmpty()) {
                for (file in localConfigs) {
                    if (configY < startY + 300f && configY + buttonHeight > startY - 50f) { // Only draw visible buttons
                        drawSingleConfigButton(mouseX, mouseY, alpha, configX, configY, buttonWidth, buttonHeight) {
                            val configName = file.name.removeSuffix(".txt")
                            Fonts.InterBold_26.drawString(configName, configX + 5, configY + 5, DrRenderUtils.applyOpacity(-1, alpha / 255f))
                            if (DrRenderUtils.isHovering(configX, configY, buttonWidth, buttonHeight, mouseX, mouseY) && Mouse.isButtonDown(0)) {
                                loadLocalConfig(configName, file)
                            }
                        }
                    }
                    configX += buttonWidth + 10
                    configCount++
                    if (configCount % configsPerRow == 0) {
                        configX = drag.x + 25
                        configY += buttonHeight + 5
                    }
                }
            } else {
                Fonts.InterBold_26.drawString("No local configurations available.", configX, configY, DrRenderUtils.applyOpacity(-1, alpha / 255f))
            }
        } else {
            if (!autoSettingsList.isNullOrEmpty()) {
                for (autoSetting in autoSettingsList!!) {
                    if (configY < startY + 300f && configY + buttonHeight > startY - 50f) { // Only draw visible buttons
                        drawSingleConfigButton(mouseX, mouseY, alpha, configX, configY, buttonWidth, buttonHeight) {
                            Fonts.InterBold_26.drawString(autoSetting.name, configX + 5, configY + 5, DrRenderUtils.applyOpacity(-1, alpha / 255f))
                            if (DrRenderUtils.isHovering(configX, configY, buttonWidth, buttonHeight, mouseX, mouseY) && Mouse.isButtonDown(0)) {
                                loadOnlineConfig(autoSetting.settingId, autoSetting.name)
                            }
                        }
                    }
                    configX += buttonWidth + 10
                    configCount++
                    if (configCount % configsPerRow == 0) {
                        configX = drag.x + 25
                        configY += buttonHeight + 5
                    }
                }
            } else {
                Fonts.InterBold_26.drawString("No online configurations available.", configX, configY, DrRenderUtils.applyOpacity(-1, alpha / 255f))
            }
        }

        // Disable scissor test
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST)

        // Draw scrollbar if needed
        drawScrollbar(drag, rectWidth, startY, 295f)
    }

    private fun drawScrollbar(drag: Drag, rectWidth: Float, startY: Float, height: Float) {
        val totalContentHeight = getContentHeight()
        if (totalContentHeight <= height) return // No scrollbar needed if content fits

        val scrollbarX = drag.x + rectWidth - 25
        val scrollbarY = startY
        val scrollbarWidth = 8f
        val scrollbarHeight = height

        // Calculate scrollbar thumb position and size
        val maxScroll = totalContentHeight - height
        val scrollPercent = (-scrollOffset / maxScroll).coerceIn(0f, 1f)
        val thumbHeight = (height / totalContentHeight * scrollbarHeight).coerceAtLeast(20f)
        val thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollPercent

        // Draw scrollbar track
        DrRenderUtils.drawRect2(scrollbarX.toDouble(), scrollbarY.toDouble(), scrollbarWidth.toDouble(), scrollbarHeight.toDouble(), Color(40, 40, 40, 150).rgb)
        // Draw scrollbar thumb
        DrRenderUtils.drawRect2(scrollbarX.toDouble(), thumbY.toDouble(), scrollbarWidth.toDouble(), thumbHeight.toDouble(), Color(100, 100, 100, 200).rgb)
    }

    private fun getContentHeight(): Float {
        val buttonHeight = 20f
        val buttonSpacing = 5f
        val configsPerRow = 4
        
        val configCount = if (showLocalConfigs) {
            val localConfigs = fileManager.settingsDir.listFiles { _, name -> name.endsWith(".txt") }
            localConfigs?.size ?: 0
        } else {
            autoSettingsList?.size ?: 0
        }
        
        val rows = kotlin.math.ceil(configCount.toDouble() / configsPerRow).toInt()
        return rows * (buttonHeight + buttonSpacing)
    }

    private inline fun drawSingleConfigButton(
        mouseX: Int,
        mouseY: Int,
        alpha: Int,
        configX: Float,
        configY: Float,
        width: Float,
        height: Float,
        drawContent: () -> Unit
    ) {
        val isHovered = DrRenderUtils.isHovering(configX, configY, width, height, mouseX, mouseY)
        val buttonColor = if (isHovered) Color(70, 70, 70, alpha).rgb else Color(50, 50, 50, alpha).rgb
        DrRenderUtils.drawRect2(configX.toDouble(), configY.toDouble(), width.toDouble(), height.toDouble(), buttonColor)
        drawContent()
    }

    private fun loadLocalConfig(configName: String, file: File) {
        try {
            displayChatMessage("Loading local configuration: $configName...")
            val localConfigContent = Files.readAllBytes(file.toPath()).toString(StandardCharsets.UTF_8)
            applyScript(localConfigContent)
            displayChatMessage("Local configuration $configName loaded successfully!")
        } catch (e: IOException) {
            displayChatMessage("Error loading local configuration: ${e.message}")
        }
    }

    private fun loadOnlineConfig(settingId: String, configName: String) {
        try {
            displayChatMessage("Loading configuration: $configName...")
            val configScript = ClientApi.getSettingsScript("legacy", settingId)
            applyScript(configScript)
            displayChatMessage("Configuration $configName loaded successfully!")
        } catch (e: Exception) {
            displayChatMessage("Error loading configuration: ${e.message}")
        }
    }

    private fun openFolder() {
        try {
            Desktop.getDesktop().open(fileManager.settingsDir)
            displayChatMessage("Opening configuration folder...")
        } catch (e: IOException) {
            displayChatMessage("Error opening folder: ${e.message}")
        }
    }
}