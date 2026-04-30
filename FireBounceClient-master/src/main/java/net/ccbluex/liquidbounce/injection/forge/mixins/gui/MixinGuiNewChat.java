/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {

    /**
     * 替换字体高度
     * 使用 35 号字体更适合聊天阅读
     */
    @Redirect(method = {"getChatComponent", "drawChat"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/FontRenderer;FONT_HEIGHT:I", opcode = Opcodes.GETFIELD))
    private int injectFontChatHeight(FontRenderer instance) {
        return HUD.INSTANCE.shouldModifyChatFont() ? Fonts.fontSemibold35.getHeight() : instance.FONT_HEIGHT;
    }

    /**
     * 替换文字绘制
     * 包含 Y 轴位置修正，防止文字偏上
     */
    @Redirect(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"))
    private int injectFontChatDraw(FontRenderer instance, String text, float x, float y, int color) {
        if (HUD.INSTANCE.shouldModifyChatFont()) {
            // 自定义字体通常需要稍微向下偏移一点才能垂直居中
            // +1.5f 或 +2.0f 通常效果最好
            return Fonts.fontSemibold35.drawStringWithShadow(text, x, y + 1.5f, color);
        } else {
            return instance.drawStringWithShadow(text, x, y, color);
        }
    }

    /**
     * 替换字符串宽度计算
     */
    @Redirect(method = "getChatComponent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"))
    private int injectFontChatWidth(FontRenderer instance, String text) {
        return HUD.INSTANCE.shouldModifyChatFont() ? Fonts.fontSemibold35.getStringWidth(text) : instance.getStringWidth(text);
    }

    /**
     * 新增：移除原版丑陋的黑色背景矩形
     * 让聊天看起来像现代 UI 的悬浮文字
     */
    @Redirect(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V"))
    private void removeChatBackground(int left, int top, int right, int bottom, int color) {
        // 如果开启了自定义聊天字体(即开启了美化)，则不绘制背景矩形
        if (!HUD.INSTANCE.shouldModifyChatFont()) {
            Gui.drawRect(left, top, right, bottom, color);
        }
    }
}