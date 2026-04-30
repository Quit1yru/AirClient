/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.features.special.ClientFixes
import net.ccbluex.liquidbounce.features.special.ClientFixes.blockFML
import net.ccbluex.liquidbounce.features.special.ClientFixes.blockPayloadPackets
import net.ccbluex.liquidbounce.features.special.ClientFixes.blockProxyPacket
import net.ccbluex.liquidbounce.features.special.ClientFixes.blockResourcePackExploit
import net.ccbluex.liquidbounce.features.special.ClientFixes.clientBrand
import net.ccbluex.liquidbounce.features.special.ClientFixes.customBrandValue
import net.ccbluex.liquidbounce.features.special.ClientFixes.fmlFixesEnabled
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ui.AbstractScreen
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import java.io.IOException

class GuiClientFixes(private val prevGui: GuiScreen) : AbstractScreen() {

    private lateinit var enabledButton: GuiButton
    private lateinit var fmlButton: GuiButton
    private lateinit var proxyButton: GuiButton
    private lateinit var payloadButton: GuiButton
    private lateinit var customBrandButton: GuiButton
    private lateinit var resourcePackButton: GuiButton
    private lateinit var customBrandTextField: GuiTextField

    override fun initGui() {
        enabledButton = +GuiButton(
            1,
            width / 2 - 100,
            height / 4 + 35,
            "AntiForge (" + (if (fmlFixesEnabled) "On" else "Off") + ")"
        )
        fmlButton =
            +GuiButton(2, width / 2 - 100, height / 4 + 35 + 25, "Block FML (" + (if (blockFML) "On" else "Off") + ")")
        proxyButton = +GuiButton(
            3,
            width / 2 - 100,
            height / 4 + 35 + 25 * 2,
            "Block FML Proxy Packet (" + (if (blockProxyPacket) "On" else "Off") + ")"
        )
        payloadButton = +GuiButton(
            4,
            width / 2 - 100,
            height / 4 + 35 + 25 * 3,
            "Block Non-MC Payloads (" + (if (blockPayloadPackets) "On" else "Off") + ")"
        )
        customBrandButton = +GuiButton(5, width / 2 - 100, height / 4 + 35 + 25 * 4, "Brand ($clientBrand)")
        resourcePackButton = +GuiButton(
            6,
            width / 2 - 100,
            height / 4 + 50 + 25 * 5,
            "Block Resource Pack Exploit (" + (if (blockResourcePackExploit) "On" else "Off") + ")"
        )

        // 自定义品牌输入框
        customBrandTextField = GuiTextField(7, mc.fontRendererObj, width / 2 - 100, height / 4 + 60 + 25 * 6, 200, 20)
        customBrandTextField.text = customBrandValue
        customBrandTextField.visible = clientBrand == "CustomBrand"

        +GuiButton(0, width / 2 - 100, height / 4 + 85 + 25 * 6, "Back")
    }

    public override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            1 -> {
                fmlFixesEnabled = !fmlFixesEnabled
                enabledButton.displayString = "AntiForge (${if (fmlFixesEnabled) "On" else "Off"})"
            }

            2 -> {
                blockFML = !blockFML
                fmlButton.displayString = "Block FML (${if (blockFML) "On" else "Off"})"
            }

            3 -> {
                blockProxyPacket = !blockProxyPacket
                proxyButton.displayString = "Block FML Proxy Packet (${if (blockProxyPacket) "On" else "Off"})"
            }

            4 -> {
                blockPayloadPackets = !blockPayloadPackets
                payloadButton.displayString = "Block FML Payload Packets (${if (blockPayloadPackets) "On" else "Off"})"
            }

            5 -> {
                val brands = listOf(*ClientFixes.possibleBrands)

                // Switch to next client brand
                clientBrand = brands[(brands.indexOf(clientBrand) + 1) % brands.size]
                customBrandButton.displayString = "Brand ($clientBrand)"

                // 更新输入框可见性
                customBrandTextField.visible = clientBrand == "CustomBrand"
            }

            6 -> {
                blockResourcePackExploit = !blockResourcePackExploit
                resourcePackButton.displayString =
                    "Block Resource Pack Exploit (${if (blockResourcePackExploit) "On" else "Off"})"
            }

            0 -> mc.displayGuiScreen(prevGui)
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)
        Fonts.fontBold180.drawCenteredString("Fixes", width / 2f, height / 8f + 5f, 4673984, true)

        // 绘制自定义品牌标签
        if (customBrandTextField.visible) {
            mc.fontRendererObj.drawString("Custom Brand:", width / 2f - 100f, height / 4f + 50 + 25 * 6, 0xFFFFFF, true)
        }

        super.drawScreen(mouseX, mouseY, partialTicks)

        // 绘制输入框
        customBrandTextField.drawTextBox()
    }

    @Throws(IOException::class)
    public override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode) {
            mc.displayGuiScreen(prevGui)
            return
        }

        // 处理输入框输入
        if (customBrandTextField.visible && customBrandTextField.isFocused) {
            customBrandTextField.textboxKeyTyped(typedChar, keyCode)
            customBrandValue = customBrandTextField.text
            return
        }

        super.keyTyped(typedChar, keyCode)
    }

    @Throws(IOException::class)
    public override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)

        // 处理输入框点击
        if (customBrandTextField.visible) {
            customBrandTextField.mouseClicked(mouseX, mouseY, mouseButton)
        }
    }

    override fun onGuiClosed() {
        saveConfig(valuesConfig)
        super.onGuiClosed()
    }
}