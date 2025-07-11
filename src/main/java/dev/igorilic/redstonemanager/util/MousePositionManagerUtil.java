package dev.igorilic.redstonemanager.util;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class MousePositionManagerUtil {
    // This is used to store the last known mouse position when transitioning between screens

    private static double lastMouseX = -1;
    private static double lastMouseY = -1;

    public static void getLastKnownPosition() {
        Minecraft mc = Minecraft.getInstance();
        lastMouseX = mc.mouseHandler.xpos();
        lastMouseY = mc.mouseHandler.ypos();
    }

    public static void setLastKnownPosition() {
        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().getWindow();
        GLFW.glfwSetCursorPos(window, lastMouseX, lastMouseY);
    }

    public static void clear() {
        Minecraft mc = Minecraft.getInstance();
        double centerX = mc.getWindow().getScreenWidth() / 2.0;
        double centerY = mc.getWindow().getScreenHeight() / 2.0;

        lastMouseX = centerX;
        lastMouseY = centerY;
    }
}
