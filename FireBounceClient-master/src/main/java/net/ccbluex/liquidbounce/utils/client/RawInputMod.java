package net.ccbluex.liquidbounce.utils.client;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RawInputMod {
    public static Thread inputThread;

    public static void start() {
        try {
            Minecraft.getMinecraft().mouseHelper = new RawMouseHelper();
            ArrayList<Controller> controllers = new ArrayList<>(Arrays.asList(ControllerEnvironment.getDefaultEnvironment().getControllers()));
            inputThread = new Thread(() -> {
                while (true) {
                    int i = 0;
                    while (i < controllers.size() && mouse == null) {
                        if (controllers.get(i).getType() == Controller.Type.MOUSE) {
                            controllers.get(i).poll();
                            if (((Mouse) controllers.get(i)).getX().getPollData() != 0.0 || ((Mouse) controllers.get(i)).getY().getPollData() != 0.0) {
                                mouse = (Mouse) controllers.get(i);
                            }
                        }
                        i++;
                    }
                    if (mouse != null) {
                        mouse.poll();
                        dx += (int) mouse.getX().getPollData();
                        dy += (int) mouse.getY().getPollData();
                        if (Minecraft.getMinecraft().currentScreen != null) {
                            dx = 0;
                            dy = 0;
                        }
                    }
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            inputThread.setName("inputThread");
            inputThread.start();
        } catch (Exception e) {
            // ignored
        }
    }

    public static void stop() {
        try {
            if (inputThread.isAlive()) inputThread.interrupt();
            Minecraft.getMinecraft().mouseHelper = new MouseHelper();
        } catch (Exception e) {
            // ignored
        }
    }

    public static Mouse mouse;
    public static List<Controller> controllers;

    // Delta for mouse
    public static int dx = 0;
    public static int dy = 0;
}

