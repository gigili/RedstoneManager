package dev.igorilic.redstonemanager.screen.custom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.function.Consumer;

public class ColorPickerScreen extends Screen {
    private final Consumer<Integer> onColorSelected;
    private float hue = 0f, saturation = 1f, brightness = 1f;
    private int selectedColor;

    private int hueX, hueY, hueWidth, hueHeight;
    private int sbX, sbY, sbSize;

    private boolean draggingHue = false;
    private boolean draggingSB = false;

    public ColorPickerScreen(Consumer<Integer> onColorSelected) {
        super(Component.literal("Color Picker"));
        this.onColorSelected = onColorSelected;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;

        hueX = centerX - 75;
        hueY = centerY - 80;
        hueWidth = 150;
        hueHeight = 15;

        sbX = centerX - 75;
        sbY = centerY - 60;
        sbSize = 150;

        addRenderableWidget(Button.builder(Component.literal("Confirm"), btn -> {
            onColorSelected.accept(selectedColor);
            Minecraft.getInstance().setScreen(null);
        }).bounds(centerX - 40, sbY + sbSize + 10, 80, 20).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inHueBar(mouseX, mouseY)) {
            draggingHue = true;
            updateHue(mouseX);
            return true;
        } else if (inSBBox(mouseX, mouseY)) {
            draggingSB = true;
            updateSB(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void updateColor() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        selectedColor = rgb & 0xFFFFFF;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        drawHueBar(guiGraphics);
        drawSaturationBrightnessSquare(guiGraphics);
        drawColorPreview(guiGraphics);
    }

    private void drawHueBar(GuiGraphics guiGraphics) {
        for (int x = 0; x < hueWidth; x++) {
            float h = (float) x / hueWidth;
            int color = Color.HSBtoRGB(h, 1f, 1f);
            guiGraphics.fill(hueX + x, hueY, hueX + x + 1, hueY + hueHeight, 0xFF000000 | color);
        }

        int hx = (int) (hue * hueWidth);
        guiGraphics.fill(hueX + hx - 1, hueY - 2, hueX + hx + 2, hueY + hueHeight + 2, 0xFFFFFFFF);
    }

    private void drawSaturationBrightnessSquare(GuiGraphics guiGraphics) {
        for (int y = 0; y < sbSize; y++) {
            for (int x = 0; x < sbSize; x++) {
                float s = (float) x / sbSize;
                float b = 1f - (float) y / sbSize;
                int color = Color.HSBtoRGB(hue, s, b);
                guiGraphics.fill(sbX + x, sbY + y, sbX + x + 1, sbY + y + 1, 0xFF000000 | color);
            }
        }

        // Draw crosshair after the SB fill
        int cx = (int) (saturation * sbSize);
        int cy = (int) ((1f - brightness) * sbSize);
        int px = sbX + cx;
        int py = sbY + cy;

        guiGraphics.fill(px - 1, py - 5, px + 2, py + 6, 0xFF000000); // vertical bar
        guiGraphics.fill(px - 5, py - 1, px + 6, py + 2, 0xFF000000); // horizontal bar
    }

    private void drawColorPreview(GuiGraphics guiGraphics) {
        int previewX = width / 2 + 80;
        int previewY = sbY;
        guiGraphics.fill(previewX, previewY, previewX + 40, previewY + 40, 0xFF000000 | selectedColor);
        guiGraphics.drawString(font, "#" + Integer.toHexString(selectedColor).toUpperCase(), previewX, previewY + 45, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingHue = false;
        draggingSB = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (draggingHue) {
            updateHue(mouseX);
            return true;
        } else if (draggingSB) {
            updateSB(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    private boolean inHueBar(double x, double y) {
        return x >= hueX && x < hueX + hueWidth && y >= hueY && y < hueY + hueHeight;
    }

    private boolean inSBBox(double x, double y) {
        return x >= sbX && x < sbX + sbSize && y >= sbY && y < sbY + sbSize;
    }

    private void updateHue(double x) {
        hue = (float) ((x - hueX) / (double) hueWidth);
        hue = Mth.clamp(hue, 0f, 1f);
        updateColor();
    }

    private void updateSB(double x, double y) {
        saturation = Mth.clamp((float) ((x - sbX) / sbSize), 0f, 1f);
        brightness = Mth.clamp(1f - (float) ((y - sbY) / sbSize), 0f, 1f);
        updateColor();
    }
}
