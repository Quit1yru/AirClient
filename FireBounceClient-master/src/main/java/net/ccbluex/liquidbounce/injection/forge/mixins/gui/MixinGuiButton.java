/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.*;

import java.awt.*;

import static net.minecraft.client.renderer.GlStateManager.resetColor;

@Mixin(GuiButton.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiButton extends Gui {

    @Shadow
    public boolean visible;

    @Shadow
    public int xPosition;

    @Shadow
    public int yPosition;

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    protected boolean hovered;

    @Shadow
    public boolean enabled;

    @Shadow
    protected abstract void mouseDragged(Minecraft mc, int mouseX, int mouseY);

    @Shadow
    public String displayString;

    @Shadow
    @Final
    protected static ResourceLocation buttonTextures;

    @Shadow
    public int id;

    // WinUI 动画变量
    @Unique
    private float animProgress = 0f;
    @Unique
    private long lastTime = System.currentTimeMillis();

    /**
     * @author CCBlueX (Modified for WinUI Style)
     * @reason WinUI Style Rendering
     */
    @Overwrite
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (visible) {
            // 1. 更新悬停状态
            hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;

            // 2. 特殊控件逻辑 (滑块)
            float sliderWidth = 0;
            boolean isSlider = false;

            if ((Object) this instanceof GuiOptionSlider) {
                sliderWidth = width * ((GuiOptionSlider) (Object) this).sliderValue;
                isSlider = true;
                // 滑块的hover逻辑通常是只要在控件内就算hover
                hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
            }

            if ((Object) this instanceof GuiScreenOptionsSounds.Button) {
                sliderWidth = width * ((GuiScreenOptionsSounds.Button) (Object) this).field_146156_o;
                isSlider = true;
                hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
            }

            // 3. 计算动画 Delta Time
            long now = System.currentTimeMillis();
            long delta = now - lastTime;
            lastTime = now;

            // 4. 动画插值 (0.0 -> 1.0)
            // 悬停且启用时增加，否则减少
            float targetProgress = (hovered && enabled) ? 1.0f : 0.0f;
            float speed = 0.01f; // 动画速度

            if (animProgress < targetProgress) {
                animProgress = Math.min(targetProgress, animProgress + (delta * speed));
            } else if (animProgress > targetProgress) {
                animProgress = Math.max(targetProgress, animProgress - (delta * speed));
            }

            // 5. 颜色定义 (WinUI Dark Mode)
            // Normal: RGB(32, 32, 32), Alpha 180
            Color colorNormal = new Color(32, 32, 32, 180);
            // Hover: RGB(60, 60, 60), Alpha 220
            Color colorHover = new Color(60, 60, 60, 220);
            // Accent (用于滑块): 类似于 WinUI 的强调色 (蓝色)
            Color colorAccent = new Color(0, 120, 215, 200);

            // 6. 混合背景颜色 (基于 animProgress)
            int bgColor = interpolateColor(colorNormal, colorHover, animProgress);

            // 如果禁用了，使用更暗的颜色
            if (!enabled) {
                bgColor = new Color(20, 20, 20, 120).getRGB();
            }

            // 7. 绘制圆角背景
            float radius = 4.0F; // WinUI 标准圆角

            // 绘制底板
            RenderUtils.INSTANCE.drawRoundedRect(xPosition, yPosition, xPosition + width, yPosition + height, bgColor, radius, RenderUtils.RoundedCorners.ALL);

            // 如果是滑块，绘制进度条
            if (isSlider) {
                // 在底板之上绘制进度部分
                if (sliderWidth > 2) { // 避免宽度太小绘制异常
                    RenderUtils.INSTANCE.drawRoundedRect(xPosition, yPosition, xPosition + sliderWidth, yPosition + height, colorAccent.getRGB(), radius, RenderUtils.RoundedCorners.ALL);
                }
            }

            // 8. 拖动逻辑 (对于滑块是必须的)
            mouseDragged(mc, mouseX, mouseY);

            // 9. 绘制文字
            AWTFontRenderer.Companion.setAssumeNonVolatile(true);

            int textColor = 14737632; // 默认灰色
            if (!enabled) {
                textColor = 10526880; // 禁用深灰
            } else if (hovered) {
                textColor = 16777215; // 悬停纯白
            }

            Fonts.fontSemibold35.drawCenteredString(displayString, xPosition + width / 2.0F, yPosition + height / 2.0F - 3, textColor);

            AWTFontRenderer.Companion.setAssumeNonVolatile(false);

            resetColor();
        }
    }

    /**
     * 简单的颜色插值辅助方法
     */
    @Unique
    private int interpolateColor(Color color1, Color color2, float fraction) {
        int red = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * fraction);
        int green = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * fraction);
        int blue = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * fraction);
        int alpha = (int) (color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * fraction);
        return new Color(red, green, blue, alpha).getRGB();
    }
}