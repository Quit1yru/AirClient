/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.features.special.BungeeCordSpoof;
import net.ccbluex.liquidbounce.file.FileManager;
import net.ccbluex.liquidbounce.lang.LanguageKt;
import net.ccbluex.liquidbounce.ui.client.GuiClientFixes;
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager;
import net.ccbluex.liquidbounce.ui.client.tools.GuiTools;
import net.ccbluex.liquidbounce.utils.client.ViaForgeIntegration;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(value = GuiMultiplayer.class, priority = 1001)
public abstract class MixinGuiMultiplayer extends MixinGuiScreen {

    private GuiButton bungeeCordSpoofButton;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void initGui(CallbackInfo callbackInfo) {
        // 检查是否存在ViaForge按钮
        GuiButton viaForgeButton = null;
        for (Object button : buttonList) {
            if (button instanceof GuiButton) {
                GuiButton btn = (GuiButton) button;
                if (btn.displayString != null && btn.displayString.equals("ViaForge")) {
                    viaForgeButton = btn;
                    break;
                }
            }
        }

        int xOffset = 0;
        int yPos = 10; // 稍微向下一点，留出顶部边距
        int btnHeight = 24; // WinUI 风格高度

        if (viaForgeButton != null) {
            xOffset += 60; // 如果存在外部ViaForge按钮，调整偏移量
            yPos = viaForgeButton.yPosition;
        }

        // 添加ViaForge按钮，如果外部没有的话
        if (viaForgeButton == null) {
            buttonList.add(new GuiButton(995, 5 + xOffset, yPos, 55, btnHeight, "ViaForge"));
            xOffset += 60;
        }

        // Left Side Buttons
        // 直接使用 GuiButton，因为 MixinGuiButton 已经全局处理了渲染样式
        buttonList.add(new GuiButton(997, 5 + xOffset, yPos, 50, btnHeight, "Fixes"));
        buttonList.add(bungeeCordSpoofButton = new GuiButton(998, 60 + xOffset, yPos, 110, btnHeight, getBungeeText()));

        // Right Side Buttons
        buttonList.add(new GuiButton(996, width - 135, yPos, 80, btnHeight, LanguageKt.translationMenu("altManager")));
        buttonList.add(new GuiButton(999, width - 50, yPos, 45, btnHeight, "Tools"));
    }

    @Unique
    private String getBungeeText() {
        return "BungeeCord: " + (BungeeCordSpoof.INSTANCE.getEnabled() ? "On" : "Off");
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void actionPerformed(GuiButton button, CallbackInfo callbackInfo) throws IOException {
        switch (button.id) {
            case 995:
                // ViaForge按钮点击事件 - 安全地打开ViaForge版本选择界面
                ViaForgeIntegration.openProtocolSelector((GuiScreen) (Object) this);
                break;
            case 996:
                mc.displayGuiScreen(new GuiAltManager((GuiScreen) (Object) this));
                break;
            case 997:
                mc.displayGuiScreen(new GuiClientFixes((GuiScreen) (Object) this));
                break;
            case 998:
                BungeeCordSpoof.INSTANCE.setEnabled(!BungeeCordSpoof.INSTANCE.getEnabled());
                bungeeCordSpoofButton.displayString = getBungeeText();
                FileManager.INSTANCE.getValuesConfig().saveConfig();
                break;
            case 999:
                mc.displayGuiScreen(new GuiTools((GuiScreen) (Object) this));
                break;
        }
    }
}