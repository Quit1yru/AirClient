/*
 * FireBounce Legit Client - 优化版
 * 修复按钮搜索框重叠问题，改进整体样式，搜索框居中
 * 修复：移除按钮文字阴影，修复 launch 协程作用域报错
 */
@file:Suppress("DEPRECATION")

package net.ccbluex.liquidbounce.ui.client.altmanager

import com.thealtening.AltService
import kotlinx.coroutines.launch
import me.liuli.elixir.account.CrackedAccount
import me.liuli.elixir.account.MicrosoftAccount
import me.liuli.elixir.account.MinecraftAccount
import me.liuli.elixir.account.MojangAccount
import net.ccbluex.liquidbounce.FireBounce.CLIENT_CLOUD
import net.ccbluex.liquidbounce.event.EventManager.call
import net.ccbluex.liquidbounce.event.SessionUpdateEvent
import net.ccbluex.liquidbounce.file.FileManager.accountsConfig
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.lang.translationButton
import net.ccbluex.liquidbounce.lang.translationMenu
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.GuiDonatorCape
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.GuiLoginIntoAccount
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.GuiSessionLogin
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.altgenerator.GuiTheAltening
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.io.*
import net.ccbluex.liquidbounce.utils.kotlin.SharedScopes
import net.ccbluex.liquidbounce.utils.kotlin.swap
import net.ccbluex.liquidbounce.utils.login.UserUtils.isValidTokenOffline
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.ui.AbstractScreen
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiSlot
import net.minecraft.client.gui.GuiTextField
import net.minecraft.util.Session
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.util.*

class GuiAltManager(private val prevGui: GuiScreen) : AbstractScreen() {

    var status = "§7Idle..."

    private lateinit var loginButton: GuiButton
    private lateinit var randomAltButton: GuiButton
    private lateinit var randomNameButton: GuiButton
    private lateinit var addButton: GuiButton
    private lateinit var removeButton: GuiButton
    private lateinit var copyButton: GuiButton
    private lateinit var altsList: GuiList
    private lateinit var searchField: GuiTextField

    // 动画状态
    private val buttonHoverProgress = mutableMapOf<GuiButton, Float>()
    private val buttonPressProgress = mutableMapOf<GuiButton, Float>()

    // WinUI 配色方案
    private val winUIBackground = Color(24, 24, 24, 255)
    private val winUICardBackground = Color(40, 40, 40, 255)
    private val winUICardHover = Color(50, 50, 50, 255)
    private val winUIAccent = Color(0, 120, 212)
    private val winUIAccentHover = Color(25, 140, 230)
    private val winUIAccentPressed = Color(0, 90, 158)
    private val winUIBorder = Color(65, 65, 65)
    private val winUIInputBackground = Color(30, 30, 30)
    private val winUITextPrimary = Color(255, 255, 255)
    private val winUITextSecondary = Color(180, 180, 180)
    private val winUITextTertiary = Color(120, 120, 120)

    // 布局常量
    private val headerHeight = 55
    private val sideButtonWidth = 90
    private val padding = 15

    private fun smoothStep(progress: Float): Float {
        return progress * progress * (3f - 2f * progress)
    }

    private fun updateAnimation(current: Float, target: Float, speed: Float = 0.2f): Float {
        val delta = target - current
        return if (kotlin.math.abs(delta) < 0.01f) target else current + delta * speed
    }

    private fun lerpColor(from: Color, to: Color, progress: Float): Color {
        val r = (from.red + (to.red - from.red) * progress).toInt()
        val g = (from.green + (to.green - from.green) * progress).toInt()
        val b = (from.blue + (to.blue - from.blue) * progress).toInt()
        val a = (from.alpha + (to.alpha - from.alpha) * progress).toInt()
        return Color(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255), a.coerceIn(0, 255))
    }

    override fun initGui() {
        // SearchField 初始化
        val searchFieldWidth = 200
        val searchFieldHeight = 22
        val searchFieldX = (width / 2) - (searchFieldWidth / 2)
        val searchFieldY = 16

        searchField = GuiTextField(
            2,
            Fonts.fontSemibold40,
            searchFieldX,
            searchFieldY,
            searchFieldWidth,
            searchFieldHeight
        ).apply {
            maxStringLength = Int.MAX_VALUE
        }

        // 列表初始化
        altsList = GuiList(this).apply {
            registerScrollButtons(7, 8)
            val currentAccountIndex = accountsConfig.accounts.indexOfFirst {
                it.name == mc.session.username
            }
            if (currentAccountIndex != -1) {
                elementClicked(currentAccountIndex, false, 0, 0)
                scrollBy(currentAccountIndex * this.getSlotHeight())
            }
        }

        // 按钮初始化
        val buttonHeight = 26
        val spacing = 6
        val buttonStartY = headerHeight + 10

        // 左侧按钮
        val leftX = padding
        var currentLeftButtonIndex = 0

        loginButton = +GuiButton(
            3, leftX, buttonStartY + currentLeftButtonIndex * (buttonHeight + spacing),
            sideButtonWidth, buttonHeight, translationButton("altManager.login")
        ).also { currentLeftButtonIndex++ }

        randomAltButton = +GuiButton(
            4, leftX, buttonStartY + currentLeftButtonIndex * (buttonHeight + spacing),
            sideButtonWidth, buttonHeight, translationButton("altManager.randomAlt")
        ).also { currentLeftButtonIndex++ }

        randomNameButton = +GuiButton(
            5, leftX, buttonStartY + currentLeftButtonIndex * (buttonHeight + spacing),
            sideButtonWidth, buttonHeight, translationButton("altManager.randomName")
        ).also { currentLeftButtonIndex++ }

        +GuiButton(
            6, leftX, buttonStartY + currentLeftButtonIndex * (buttonHeight + spacing),
            sideButtonWidth, buttonHeight, translationButton("altManager.directLogin")
        ).also { currentLeftButtonIndex++ }

        +GuiButton(
            10, leftX, buttonStartY + currentLeftButtonIndex * (buttonHeight + spacing),
            sideButtonWidth, buttonHeight, translationButton("altManager.sessionLogin")
        ).also { currentLeftButtonIndex++ }

        if (activeGenerators.getOrDefault("thealtening", true)) {
            +GuiButton(
                9, leftX, buttonStartY + currentLeftButtonIndex * (buttonHeight + spacing),
                sideButtonWidth, buttonHeight, translationButton("altManager.theAltening")
            ).also { currentLeftButtonIndex++ }
        }

        +GuiButton(
            11, leftX, buttonStartY + currentLeftButtonIndex * (buttonHeight + spacing),
            sideButtonWidth, buttonHeight, translationButton("altManager.cape")
        ).also { currentLeftButtonIndex++ }

        +GuiButton(
            0, leftX, height - 35,
            sideButtonWidth, buttonHeight, translationButton("back")
        )

        // 右侧按钮
        val rightX = width - sideButtonWidth - padding
        var currentRightButtonIndex = 0

        addButton = +GuiButton(
            1, rightX, buttonStartY + currentRightButtonIndex * (buttonHeight + spacing),
            sideButtonWidth, buttonHeight, translationButton("add")
        ).also { currentRightButtonIndex++ }

        removeButton = +GuiButton(
            2, rightX, buttonStartY + currentRightButtonIndex * (buttonHeight + spacing),
            sideButtonWidth, buttonHeight, translationButton("remove")
        ).also { currentRightButtonIndex++ }

        +GuiButton(
            13, rightX, buttonStartY + currentRightButtonIndex * (buttonHeight + spacing),
            sideButtonWidth, buttonHeight, translationButton("moveUp")
        ).also { currentRightButtonIndex++ }

        +GuiButton(
            14, rightX, buttonStartY + currentRightButtonIndex * (buttonHeight + spacing),
            sideButtonWidth, buttonHeight, translationButton("moveDown")
        ).also { currentRightButtonIndex++ }

        +GuiButton(
            7, rightX, buttonStartY + currentRightButtonIndex * (buttonHeight + spacing),
            sideButtonWidth, buttonHeight, translationButton("import")
        ).also { currentRightButtonIndex++ }

        +GuiButton(
            12, rightX, buttonStartY + currentRightButtonIndex * (buttonHeight + spacing),
            sideButtonWidth, buttonHeight, translationButton("export")
        ).also { currentRightButtonIndex++ }

        copyButton = +GuiButton(
            8, rightX, buttonStartY + currentRightButtonIndex * (buttonHeight + spacing),
            sideButtonWidth, buttonHeight, translationButton("altManager.copy")
        )
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        assumeNonVolatile {
            // 背景
            drawBackground(0)
            RenderUtils.drawRect(0f, 0f, width.toFloat(), height.toFloat(), winUIBackground.rgb)

            // Header 背景
            RenderUtils.drawRoundedRect(10f, 10f, (width - 10).toFloat(), headerHeight.toFloat(), winUICardBackground.rgb, 8f)
            RenderUtils.drawRoundedBorder(10f, 10f, (width - 10).toFloat(), headerHeight.toFloat(), 1f, winUIBorder.rgb, 8f)

            // 标题
            Fonts.fontSemibold40.drawString(
                translationMenu("altManager"),
                25f, 22f, winUITextPrimary.rgb
            )

            // 计数
            val countStr = if (searchField.text.isEmpty()) {
                "${accountsConfig.accounts.size} Alts"
            } else {
                "${altsList.accounts.size} Found"
            }
            val countWidth = Fonts.Bold36.getStringWidth(countStr)
            Fonts.Bold36.drawString(
                countStr,
                (width - 25 - countWidth).toFloat(), 24f, winUITextSecondary.rgb
            )

            // 列表背景框
            RenderUtils.drawRoundedRect(
                115f, (headerHeight + 5).toFloat(),
                (width - 115).toFloat(), (height - 35).toFloat(),
                Color(30, 30, 30, 150).rgb, 8f
            )

            // 绘制列表
            altsList.drawScreen(mouseX, mouseY, partialTicks)

            // 搜索框
            val sfX = searchField.xPosition.toFloat()
            val sfY = searchField.yPosition.toFloat()
            val sfW = searchField.width.toFloat()
            val sfH = searchField.height.toFloat()

            RenderUtils.drawRoundedRect(sfX - 2, sfY - 2, sfX + sfW + 2, sfY + sfH + 2, winUIInputBackground.rgb, 5f)
            val borderColor = if (searchField.isFocused) winUIAccent else winUIBorder
            RenderUtils.drawRoundedBorder(sfX - 2, sfY - 2, sfX + sfW + 2, sfY + sfH + 2, 1f, borderColor.rgb, 5f)

            searchField.drawTextBox()

            if (searchField.text.isEmpty() && !searchField.isFocused) {
                Fonts.fontSemibold35.drawString(
                    "Type to search...",
                    sfX + 4, sfY + (sfH - Fonts.fontSemibold35.fontHeight) / 2f + 1,
                    winUITextTertiary.rgb
                )
            }

            // 状态栏
            val statusY = height - 25f
            val statusWidth = Fonts.Bold36.getStringWidth(status)
            Fonts.Bold36.drawString(
                status,
                (width / 2f - statusWidth / 2f),
                statusY,
                if(status.contains("§c")) Color(255, 80, 80).rgb else winUITextSecondary.rgb
            )

            // 用户信息
            val infoX = 20f
            val infoY = height - 70f
            Fonts.Bold36.drawString("User: §f${mc.session.username}", infoX, infoY, winUITextSecondary.rgb)
            val typeStr = when {
                altService.currentService == AltService.EnumAltService.THEALTENING -> "TheAltening"
                isValidTokenOffline(mc.session.token) -> "Premium"
                else -> "Cracked"
            }
            Fonts.Bold36.drawString("Type: §f$typeStr", infoX, infoY + 10f, winUITextSecondary.rgb)
        }

        // 绘制自定义按钮
        // 注意：不要调用 super.drawScreen，因为它会绘制原版带阴影的按钮
        drawWinUIButtons(mouseX, mouseY)
    }

    private fun drawWinUIButtons(mouseX: Int, mouseY: Int) {
        buttonList.forEach { button ->
            if (button !is GuiButton) return@forEach

            val isHovered = mouseX >= button.xPosition && mouseY >= button.yPosition &&
                    mouseX < button.xPosition + button.width && mouseY < button.yPosition + button.height

            val currentHover = buttonHoverProgress.getOrPut(button) { 0f }
            val targetHover = if (isHovered && button.enabled) 1f else 0f
            buttonHoverProgress[button] = updateAnimation(currentHover, targetHover, 0.25f)

            val currentPress = buttonPressProgress.getOrPut(button) { 0f }
            val targetPress = if (isHovered && org.lwjgl.input.Mouse.isButtonDown(0) && button.enabled) 1f else 0f
            buttonPressProgress[button] = updateAnimation(currentPress, targetPress, 0.3f)

            val hoverProgress = smoothStep(buttonHoverProgress[button] ?: 0f)
            val pressProgress = smoothStep(buttonPressProgress[button] ?: 0f)

            val isPrimary = button.id == 3 || button.id == 1
            val isDestructive = button.id == 2

            val baseColor = when {
                !button.enabled -> Color(45, 45, 45)
                isPrimary -> winUIAccent
                isDestructive -> Color(180, 40, 40, 200)
                else -> winUICardBackground
            }

            val hoverColor = when {
                !button.enabled -> Color(45, 45, 45)
                isPrimary -> winUIAccentHover
                isDestructive -> Color(200, 50, 50, 220)
                else -> winUICardHover
            }

            val pressColor = when {
                !button.enabled -> Color(45, 45, 45)
                isPrimary -> winUIAccentPressed
                isDestructive -> Color(150, 30, 30)
                else -> Color(60, 60, 60)
            }

            var buttonColor = lerpColor(baseColor, hoverColor, hoverProgress)
            buttonColor = lerpColor(buttonColor, pressColor, pressProgress)

            // 阴影
            if (button.enabled && hoverProgress > 0.05f) {
                RenderUtils.drawRoundedRect(
                    button.xPosition.toFloat() - 1,
                    button.yPosition.toFloat() + 2,
                    (button.xPosition + button.width).toFloat() + 1,
                    (button.yPosition + button.height).toFloat() + 3,
                    Color(0, 0, 0, (40 * hoverProgress).toInt()).rgb,
                    6f
                )
            }

            // 背景
            RenderUtils.drawRoundedRect(
                button.xPosition.toFloat(),
                button.yPosition.toFloat(),
                (button.xPosition + button.width).toFloat(),
                (button.yPosition + button.height).toFloat(),
                buttonColor.rgb,
                6f
            )

            // 边框
            if (!isPrimary && !isDestructive && button.enabled) {
                val borderColor = lerpColor(winUIBorder, winUIAccent, hoverProgress * 0.5f)
                RenderUtils.drawRoundedBorder(
                    button.xPosition.toFloat(),
                    button.yPosition.toFloat(),
                    (button.xPosition + button.width).toFloat(),
                    (button.yPosition + button.height).toFloat(),
                    1f,
                    borderColor.rgb,
                    6f
                )
            }

            // 文字 (shadow = false)
            val textColor = if (!button.enabled) winUITextTertiary else winUITextPrimary
            Fonts.fontSemibold35.drawCenteredString(
                button.displayString,
                button.xPosition + button.width / 2f,
                button.yPosition + (button.height - 6) / 2f,
                textColor.rgb,
                false
            )
        }
    }

    public override fun actionPerformed(button: GuiButton) {
        if (!button.enabled) return

        when (button.id) {
            0 -> mc.displayGuiScreen(prevGui)
            1 -> mc.displayGuiScreen(GuiLoginIntoAccount(this))
            2 -> {
                status = if (altsList.selectedSlot != -1 && altsList.selectedSlot < altsList.size) {
                    accountsConfig.removeAccount(altsList.accounts[altsList.selectedSlot])
                    saveConfig(accountsConfig)
                    "§aThe account has been removed."
                } else {
                    "§cSelect an account to remove."
                }
            }
            3 -> {
                status = altsList.selectedAccount?.let {
                    loginButton.enabled = false
                    randomAltButton.enabled = false
                    randomNameButton.enabled = false

                    login(it, {
                        status = "§aLogged into §f§l${mc.session.username}§a."
                    }, { exception ->
                        status = "§cLogin failed: ${exception.message}"
                    }, {
                        loginButton.enabled = true
                        randomAltButton.enabled = true
                        randomNameButton.enabled = true
                    })

                    "§aLogging in..."
                } ?: "§cSelect an account first."
            }
            4 -> {
                status = altsList.accounts.randomOrNull()?.let {
                    loginButton.enabled = false
                    randomAltButton.enabled = false
                    randomNameButton.enabled = false

                    login(it, {
                        status = "§aLogged into random alt §f§l${mc.session.username}§a."
                        val index = accountsConfig.accounts.indexOf(it)
                        if(index != -1) {
                            altsList.selectedSlot = index
                            altsList.scrollBy(index * altsList.getSlotHeight() - altsList.amountScrolled.toInt())
                        }
                    }, { exception ->
                        status = "§cRandom login failed: ${exception.message}"
                    }, {
                        loginButton.enabled = true
                        randomAltButton.enabled = true
                        randomNameButton.enabled = true
                    })

                    "§aLogging into random alt..."
                } ?: "§cNo accounts available."
            }
            5 -> {
                val randomName = "Guest" + (Math.random() * 10000).toInt()
                altService.switchService(AltService.EnumAltService.MOJANG)
                mc.session = Session(randomName, "", "", "mojang")
                call(SessionUpdateEvent)
                status = "§aLogged into §f§l$randomName§a (Cracked)."
            }
            6 -> mc.displayGuiScreen(GuiLoginIntoAccount(this, directLogin = true))
            7 -> {
                val file = MiscUtils.openFileChooser(FileFilters.TEXT) ?: return
                var count = 0
                // 修复：使用 SharedScopes.IO.launch 替代直接调用 launch
                SharedScopes.IO.launch {
                    try {
                        file.readLines().forEach {
                            val accountData = it.split(':', limit = 2)
                            if (accountData.size > 1) {
                                accountsConfig.addMojangAccount(accountData[0], accountData[1])
                                count++
                            } else if (accountData.isNotEmpty() && accountData[0].length < 16) {
                                accountsConfig.addCrackedAccount(accountData[0])
                                count++
                            }
                        }
                        saveConfig(accountsConfig)
                        status = "§aImported $count accounts successfully."
                    } catch (e: Exception) {
                        status = "§cImport failed: ${e.message}"
                    }
                }
            }
            12 -> {
                if (accountsConfig.accounts.isEmpty()) {
                    status = "§cNothing to export."
                    return
                }
                val file = MiscUtils.saveFileChooser()
                if (file != null && !file.isDirectory) {
                    try {
                        if (!file.exists()) file.createNewFile()
                        val content = accountsConfig.accounts.joinToString("\n") { account ->
                            when (account) {
                                is MojangAccount -> "${account.email}:${account.password}"
                                is MicrosoftAccount -> "${account.name}:${account.session.token}"
                                else -> account.name
                            }
                        }
                        file.writeText(content)
                        status = "§aExported successfully!"
                    } catch (e: Exception) {
                        status = "§cExport failed: ${e.message}"
                    }
                }
            }
            8 -> {
                val currentAccount = altsList.selectedAccount
                if (currentAccount != null) {
                    val data = when (currentAccount) {
                        is MojangAccount -> "${currentAccount.email}:${currentAccount.password}"
                        is MicrosoftAccount -> "${currentAccount.name}:${currentAccount.session.token}"
                        else -> currentAccount.name
                    }
                    MiscUtils.copy(data)
                    status = "§aCopied to clipboard."
                } else {
                    status = "§cSelect an account."
                }
            }
            9 -> mc.displayGuiScreen(GuiTheAltening(this))
            10 -> mc.displayGuiScreen(GuiSessionLogin(this))
            11 -> mc.displayGuiScreen(GuiDonatorCape(this))
            13 -> {
                val currentAccount = altsList.selectedAccount ?: return
                val index = accountsConfig.accounts.indexOf(currentAccount)
                if (index > 0) {
                    accountsConfig.accounts.swap(index, index - 1)
                    accountsConfig.saveConfig()
                    altsList.selectedSlot--
                }
            }
            14 -> {
                val currentAccount = altsList.selectedAccount ?: return
                val index = accountsConfig.accounts.indexOf(currentAccount)
                if (index < accountsConfig.accounts.size - 1) {
                    accountsConfig.accounts.swap(index, index + 1)
                    accountsConfig.saveConfig()
                    altsList.selectedSlot++
                }
            }
        }
    }

    public override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (searchField.isFocused) {
            searchField.textboxKeyTyped(typedChar, keyCode)
            return
        }

        when (keyCode) {
            Keyboard.KEY_ESCAPE -> mc.displayGuiScreen(prevGui)
            Keyboard.KEY_UP -> {
                altsList.selectedSlot -= 1
                if(altsList.selectedSlot < 0) altsList.selectedSlot = 0
            }
            Keyboard.KEY_DOWN -> {
                altsList.selectedSlot += 1
                if(altsList.selectedSlot >= altsList.size) altsList.selectedSlot = altsList.size - 1
            }
            Keyboard.KEY_RETURN -> altsList.elementClicked(altsList.selectedSlot, true, 0, 0)
            Keyboard.KEY_PRIOR -> altsList.scrollBy(-height + 100)
            Keyboard.KEY_NEXT -> altsList.scrollBy(height - 100)
            Keyboard.KEY_DELETE -> actionPerformed(removeButton)
            else -> super.keyTyped(typedChar, keyCode)
        }
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        altsList.handleMouseInput()
    }

    public override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        searchField.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun updateScreen() = searchField.updateCursorCounter()

    @Suppress("RedundantVisibilityModifier")
    private inner class GuiList(prevGui: GuiScreen) :
        GuiSlot(mc, prevGui.width, prevGui.height, 60, prevGui.height - 35, 36) {

        val accounts: List<MinecraftAccount>
            get() {
                val search = searchField.text?.trim()?.lowercase(Locale.getDefault())
                return if (search.isNullOrEmpty()) {
                    accountsConfig.accounts
                } else {
                    accountsConfig.accounts.filter {
                        it.name.lowercase().contains(search) ||
                                (it is MojangAccount && it.email.lowercase().contains(search))
                    }
                }
            }

        var selectedSlot = -1

        val selectedAccount get() = if (selectedSlot in accounts.indices) accounts[selectedSlot] else null

        override fun isSelected(id: Int) = selectedSlot == id

        public override fun getSize() = accounts.size

        public override fun elementClicked(clickedElement: Int, doubleClick: Boolean, var3: Int, var4: Int) {
            selectedSlot = clickedElement

            if (doubleClick) {
                altsList.selectedAccount?.let {
                    status = "§aLogging in..."
                    login(it, {
                        status = "§aLogged into §f§l${mc.session.username}§a."
                    }, { ex ->
                        status = "§cError: ${ex.message}"
                    }, {})
                }
            }
        }

        override fun drawSlot(id: Int, x: Int, y: Int, var4: Int, var5: Int, var6: Int) {
            val account = accounts.getOrNull(id) ?: return

            val isSelected = isSelected(id)
            val itemWidth = width - 240
            val itemX = (width / 2) - (itemWidth / 2)
            val itemHeight = 32

            val bgColor = if (isSelected) winUICardHover else winUICardBackground
            val borderColor = if (isSelected) winUIAccent else Color(60, 60, 60, 100)

            RenderUtils.drawRoundedRect(
                itemX.toFloat(), y.toFloat(),
                (itemX + itemWidth).toFloat(), (y + itemHeight).toFloat(),
                bgColor.rgb, 6f
            )

            RenderUtils.drawRoundedBorder(
                itemX.toFloat(), y.toFloat(),
                (itemX + itemWidth).toFloat(), (y + itemHeight).toFloat(),
                if (isSelected) 1.5f else 1f,
                borderColor.rgb, 6f
            )

            val displayName = if (account is MojangAccount && account.name.isEmpty()) account.email else account.name
            val typeName = when (account) {
                is CrackedAccount -> "Cracked"
                is MicrosoftAccount -> "Microsoft"
                is MojangAccount -> "Mojang"
                else -> "Unknown"
            }

            val typeColor = when (account) {
                is MicrosoftAccount -> Color(80, 200, 255)
                is MojangAccount -> Color(100, 200, 100)
                is CrackedAccount -> Color(180, 180, 180)
                else -> Color.WHITE
            }

            Fonts.fontSemibold40.drawString(
                displayName,
                itemX + 10f, y + 6f,
                if (isSelected) Color.WHITE.rgb else Color(230, 230, 230).rgb
            )

            Fonts.fontSemibold35.drawString(
                typeName,
                (itemX + itemWidth - Fonts.fontSemibold35.getStringWidth(typeName) - 10).toFloat(),
                y + 10f,
                typeColor.rgb
            )

            if (account.name == mc.session.username) {
                RenderUtils.drawRoundedRect(
                    itemX + 4f, y + 12f,
                    itemX + 8f, y + 16f,
                    Color.GREEN.rgb, 2f
                )
            }
        }

        override fun drawBackground() {}
    }

    companion object {
        val altService = AltService()
        private val activeGenerators = mutableMapOf<String, Boolean>()

        fun loadActiveGenerators() {
            try {
                activeGenerators += HttpClient.get("$CLIENT_CLOUD/generators.json").jsonBody<Map<String, Boolean>>()!!
            } catch (t: Throwable) {
                LOGGER.error("Failed to load generators", t)
            }
        }

        fun login(
            account: MinecraftAccount, success: () -> Unit, error: (Exception) -> Unit, done: () -> Unit
        ) = SharedScopes.IO.launch {
            if (altService.currentService != AltService.EnumAltService.MOJANG) {
                try {
                    altService.switchService(AltService.EnumAltService.MOJANG)
                } catch (e: Exception) {
                    error(e)
                }
            }

            try {
                account.update()
                mc.session = Session(
                    account.session.username,
                    account.session.uuid,
                    account.session.token,
                    "microsoft"
                )
                call(SessionUpdateEvent)
                success()
            } catch (e: Exception) {
                error(e)
            }
            done()
        }
    }
}