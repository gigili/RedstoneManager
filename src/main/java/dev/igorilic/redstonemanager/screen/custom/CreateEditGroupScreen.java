package dev.igorilic.redstonemanager.screen.custom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class CreateEditGroupScreen extends Screen {
    private Consumer<String> groupName;
    private String oldName;
    private Screen parent;

    private EditBox input;

    protected CreateEditGroupScreen(Consumer<String> groupName, String oldName, Screen parent) {
        super(Component.translatable("tooltip.redstonemanager.manager.create_group"));
        this.groupName = groupName;
        this.oldName = oldName;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = width / 2;
        int centerY = height / 2;

        Component header =
                this.oldName.isEmpty()
                        ? Component.translatable("tooltip.redstonemanager.manager.create_group")
                        : Component.translatable("tooltip.redstonemanager.manager.edit_group");

        StringWidget wgHeader = new StringWidget(header, font);
        wgHeader.setX(centerX - wgHeader.getWidth() / 2);
        wgHeader.setY(centerY - 33);

        this.addRenderableWidget(wgHeader);

        // Input field centered
        input = new EditBox(font, centerX - 75, centerY - 17, 150, 20, Component.literal(""));
        if (!oldName.isEmpty()) {
            input.setValue(oldName);
        }
        this.addRenderableWidget(input);

        // Button centered
        Component btnLabel = this.oldName.isEmpty()
                ? Component.translatable("label.redstonemanager.manager.create")
                : Component.translatable("label.redstonemanager.manager.save");

        Button btnSave = Button.builder(btnLabel, (b) -> {
            groupName.accept(input.getValue());
            Minecraft.getInstance().setScreen(parent);
        }).bounds(centerX - 23, centerY + 10, 46, 20).build();

        this.addRenderableWidget(btnSave);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int padding = 16;
        int spacingBetweenLines = 6;

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int textHeight = 9; // font line height
        int buttonHeight = 20;
        int inputWidth = 150;

        // Calculate total height of the content block
        int boxHeight = textHeight + spacingBetweenLines + buttonHeight + padding * 4;
        int boxWidth = inputWidth + padding * 4;

        // Box coordinates
        int x1 = centerX - boxWidth / 2;
        int y1 = centerY - boxHeight / 2;
        int x2 = centerX + boxWidth / 2;
        int y2 = centerY + boxHeight / 2;

        // Fill with dark gray, low-transparency background
        guiGraphics.fill(x1, y1, x2, y2 - 5, 0xDD555555); // ARGB — 0xDD = ~87% opacity

        // Then render everything else
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (input.isFocused()) {
            if (input.keyPressed(keyCode, scanCode, modifiers) || input.canConsumeInput()) {
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
