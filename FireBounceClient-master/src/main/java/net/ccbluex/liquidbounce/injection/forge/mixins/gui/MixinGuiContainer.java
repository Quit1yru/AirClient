package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.config.ColorValue;
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoArmor;
import net.ccbluex.liquidbounce.features.module.modules.player.InventoryCleaner;
import net.ccbluex.liquidbounce.features.module.modules.world.ChestStealer;
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.utils.timing.TickTimer;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(GuiContainer.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiContainer extends MixinGuiScreen {

    // Separate TickTimer instances to avoid timing conflicts
    @Unique
    final TickTimer tick0 = new TickTimer();
    @Unique
    final TickTimer tick1 = new TickTimer();
    @Unique
    final TickTimer tick2 = new TickTimer();

    @Inject(method = "initGui", at = @At("RETURN"), cancellable = true)
    private void init(CallbackInfo ci) {
        if (ChestStealer.INSTANCE.handleEvents() && ChestStealer.INSTANCE.getSilentGUI()) {
            if (mc.currentScreen instanceof GuiChest) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    private void drawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (ChestStealer.INSTANCE.handleEvents() && ChestStealer.INSTANCE.getSilentGUI()) {
            if (mc.currentScreen instanceof GuiChest) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void drawSlot(Slot slot, CallbackInfo ci) {
        // Instances
        final InventoryManager inventoryManager = InventoryManager.INSTANCE;
        final ChestStealer chestStealer = ChestStealer.INSTANCE;
        final InventoryCleaner inventoryCleaner = InventoryCleaner.INSTANCE;
        final AutoArmor autoArmor = AutoArmor.INSTANCE;

        // Slot X/Y
        int x = slot.xDisplayPosition;
        int y = slot.yDisplayPosition;

        // Get the current slot being operated on
        int currentSlotChestStealer = inventoryManager.getChestStealerCurrentSlot();
        int currentSlotInvCleaner = inventoryManager.getInvCleanerCurrentSlot();
        int currentSlotAutoArmor = inventoryManager.getAutoArmorCurrentSlot();

        // WinUI Style Rendering Logic
        if (mc.currentScreen instanceof GuiChest) {
            if (chestStealer.handleEvents() && !chestStealer.getSilentGUI() && chestStealer.getHighlightSlot()) {
                if (slot.slotNumber == currentSlotChestStealer && currentSlotChestStealer != -1 && currentSlotChestStealer != inventoryManager.getChestStealerLastSlot()) {

                    // Render WinUI Highlight
                    Color bgColor = ((ColorValue) chestStealer.getBackgroundColor()).selectedColor();
                    Color borderColor = ((ColorValue) chestStealer.getBorderColor()).selectedColor();
                    renderWinUISlotHighlight(x, y, bgColor, borderColor);

                    // Logic to update last slot
                    if (!slot.getHasStack() && tick0.hasTimePassed(100)) {
                        inventoryManager.setChestStealerLastSlot(currentSlotChestStealer);
                        tick0.reset();
                    } else {
                        tick0.update();
                    }
                }
            }
        }

        if (mc.currentScreen instanceof GuiInventory) {
            if (inventoryManager.getHighlightSlotValue().get()) {
                Color invBgColor = ((ColorValue) inventoryManager.getBackgroundColor()).selectedColor();
                Color invBorderColor = ((ColorValue) inventoryManager.getBorderColor()).selectedColor();

                if (inventoryCleaner.handleEvents()) {
                    if (slot.slotNumber == currentSlotInvCleaner && currentSlotInvCleaner != -1 && currentSlotInvCleaner != inventoryManager.getInvCleanerLastSlot()) {

                        // Render WinUI Highlight
                        renderWinUISlotHighlight(x, y, invBgColor, invBorderColor);

                        if (!slot.getHasStack() && tick1.hasTimePassed(100)) {
                            inventoryManager.setInvCleanerLastSlot(currentSlotInvCleaner);
                            tick1.reset();
                        } else {
                            tick1.update();
                        }
                    }
                }

                if (autoArmor.handleEvents()) {
                    if (slot.slotNumber == currentSlotAutoArmor && currentSlotAutoArmor != -1 && currentSlotAutoArmor != inventoryManager.getAutoArmorLastSlot()) {

                        // Render WinUI Highlight
                        renderWinUISlotHighlight(x, y, invBgColor, invBorderColor);

                        if (!slot.getHasStack() && tick2.hasTimePassed(100)) {
                            inventoryManager.setAutoArmorLastSlot(currentSlotAutoArmor);
                            tick2.reset();
                        } else {
                            tick2.update();
                        }
                    }
                }
            }
        }
    }

    /**
     * Renders a WinUI 3 style highlight for a slot.
     * Features: Rounded corners, pulsing animation.
     */
    @Unique
    private void renderWinUISlotHighlight(int x, int y, Color bgColor, Color borderColor) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Calculate pulsing alpha for "active" feel (WinUI style breathing)
        double pulse = Math.sin(System.currentTimeMillis() / 200.0) * 0.2 + 0.8; // oscillates between 0.6 and 1.0 roughly

        // Background (Fill)
        // Usually lighter alpha
        int bgAlpha = (int) (bgColor.getAlpha() * pulse * 0.6);
        int bgRGB = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), Math.max(0, Math.min(255, bgAlpha))).getRGB();

        // Border (Stroke)
        // Usually stronger alpha
        int borderAlpha = (int) (borderColor.getAlpha() * pulse);
        int borderRGB = new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), Math.max(0, Math.min(255, borderAlpha))).getRGB();

        float radius = 4.0F; // WinUI standard radius

        // Draw Fill
        RenderUtils.INSTANCE.drawRoundedRect(x, y, x + 16, y + 16, bgRGB, radius, RenderUtils.RoundedCorners.ALL);

        // Draw Border (Outline) - Assuming RenderUtils has an outline method or we draw a slightly larger/hollow rect
        // Since standard drawRoundedRect fills, we can draw a slightly larger one behind or assume RenderUtils has outline support.
        // Here we simulate outline by drawing the filled rect (background) and then a "border" effect if RenderUtils supports it,
        // otherwise, drawing just the filled rect with rounded corners looks very modern already.

        // Optional: If you have drawRoundedRectOutline
        // RenderUtils.INSTANCE.drawRoundedRectOutline(x, y, x + 16, y + 16, borderRGB, radius, 1.5f);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}