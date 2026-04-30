package net.ccbluex.liquidbounce.utils.client;

import net.minecraft.client.gui.GuiScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/**
 * ViaForge Integration Utility Class
 * Provides safe integration with ViaForge protocol translation mod
 */
public class ViaForgeIntegration {
    private static final Logger logger = LogManager.getLogger("ViaForgeIntegration");

    /**
     * Checks if ViaForge is available in the classpath
     */
    public static boolean isViaForgeAvailable() {
        try {
            Class.forName("de.florianmichael.viaforge.ViaForge");
            return true;
        } catch (ClassNotFoundException e) {
            logger.info("ViaForge not found in classpath");
            return false;
        }
    }

    /**
     * Safely opens the protocol selector GUI if ViaForge is available
     * @param prevGuiScreen Previous GUI screen to return to
     */
    public static void openProtocolSelector(GuiScreen prevGuiScreen) {
        if (!isViaForgeAvailable()) {
            logger.warn("ViaForge not available, cannot open protocol selector");
            return;
        }

        try {
            // Attempt to open the ViaForge protocol selector
            Class<?> viaForgeClass = Class.forName("de.florianmichael.viaforge.ViaForge");
            
            // Try to get the instance and open the GUI
            Object viaForgeInstance = viaForgeClass.getMethod("getInstance").invoke(null);
            Class<?> guiProtocolSelectorClass = Class.forName("de.florianmichael.viaforge.gui.GuiProtocolSelector");
            
            // Create the protocol selector GUI with the previous screen
            Object protocolSelectorGui = guiProtocolSelectorClass.getConstructor(GuiScreen.class)
                .newInstance(prevGuiScreen);
            
            // Open the GUI
            net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen((net.minecraft.client.gui.GuiScreen) protocolSelectorGui);
            
        } catch (Exception e) {
            logger.error("Failed to open ViaForge protocol selector", e);
            
            // Attempt alternative method (for different ViaForge versions)
            tryAlternativeMethod(prevGuiScreen);
        }
    }

    /**
     * Tries alternative method for opening protocol selector (different ViaForge versions)
     */
    private static void tryAlternativeMethod(GuiScreen prevGuiScreen) {
        try {
            Class<?> viaForgeClass = Class.forName("de.florianmichael.viaforge.ViaForge");
            Method getManagerMethod = viaForgeClass.getMethod("getManager");
            Object manager = getManagerMethod.invoke(null);
            
            Class<?> protocolVersionClass = Class.forName("de.florianmichael.viaforge.common.platform.ProtocolVersion");
            Class<?> guiProtocolSelectorClass = Class.forName("de.florianmichael.viaforge.gui.GuiProtocolSelector");
            
            Object protocolSelectorGui = guiProtocolSelectorClass.getConstructor(GuiScreen.class, protocolVersionClass)
                .newInstance(prevGuiScreen, null);
            
            net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen((net.minecraft.client.gui.GuiScreen) protocolSelectorGui);
            
        } catch (Exception ex) {
            logger.error("Alternative method to open ViaForge protocol selector also failed", ex);
        }
    }
}