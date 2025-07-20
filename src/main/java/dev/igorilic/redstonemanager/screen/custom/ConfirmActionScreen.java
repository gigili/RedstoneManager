package dev.igorilic.redstonemanager.screen.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ConfirmActionScreen extends Screen {
    private final Component confirmText;
    private final Component confirmLabel;
    private final Component confirmDesc;
    private final Consumer<Boolean> onConfirm;
    private final Screen parent;
    private Boolean hasDescription;

    protected ConfirmActionScreen(
            Component confirmText,
            Component confirmLabel,
            Consumer<Boolean> onConfirm,
            Screen parent
    ) {
        super(Component.literal(""));
        this.confirmText = confirmText;
        this.confirmLabel = confirmLabel;
        this.onConfirm = onConfirm;
        this.parent = parent;
        this.confirmDesc = Component.literal("");
        this.hasDescription = false;
    }

    protected ConfirmActionScreen(
            Component confirmText,
            Component confirmDescription,
            Component confirmLabel,
            Consumer<Boolean> onConfirm,
            Screen parent
    ) {
        super(Component.literal(""));
        this.confirmText = confirmText;
        this.confirmDesc = confirmDescription;
        this.confirmLabel = confirmLabel;
        this.onConfirm = onConfirm;
        this.parent = parent;
        this.hasDescription = !confirmDesc.getString().isEmpty();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = width / 2;
        int centerY = height / 2;

        confirmText.plainCopy().withStyle(ChatFormatting.BOLD);
        StringWidget wgtConfirmText = new StringWidget(confirmText, font) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                float scale = 1.5f; // Set desired scale
                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                poseStack.translate(this.getX(), this.getY(), 0);
                poseStack.scale(scale, scale, 1f);

                // Offset X/Y because scaling doesn't affect widget bounds
                guiGraphics.drawString(font, this.getMessage(), 0, 0, this.getColor(), true);

                poseStack.popPose();
            }
        };
        wgtConfirmText.setX(centerX - (int) (font.width(confirmText) / 1.5) + 5);
        wgtConfirmText.setY(centerY - (hasDescription ? 30 : 15));
        this.addRenderableWidget(wgtConfirmText);

        if (hasDescription) {
            StringWidget wgDescriptionText = new StringWidget(confirmDesc, font);
            wgDescriptionText.setX(centerX - (font.width(confirmDesc) / 2) + 40);
            wgDescriptionText.setY(centerY - 10);
            this.addRenderableWidget(wgDescriptionText);
        }

        Button btnAction = Button.builder(confirmLabel, (b) -> {
                    onConfirm.accept(true);
                    Minecraft.getInstance().setScreen(parent);
                })
                .bounds(centerX - ((font.width(confirmLabel) + 10) / 2) - 10, centerY + 10, font.width(confirmLabel) + 10, 20)
                .build();

        Component cancelLabel = Component.translatable("label.redstonemanager.manager.cancel");
        Button btnCancel = Button.builder(cancelLabel, (b) -> {
                    onConfirm.accept(false);
                    Minecraft.getInstance().setScreen(parent);
                })
                .bounds(centerX - ((font.width(confirmLabel) + 10) / 2) + 50, centerY + 10, font.width(cancelLabel) + 10, 20)
                .build();

        this.addRenderableWidget(btnAction);
        this.addRenderableWidget(btnCancel);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int padding = 16;
        int spacingBetweenLines = 6;
        float titleScale = 1.5f;

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Measure scaled title width and height
        int titleRawWidth = font.width(confirmText);
        int titleScaledWidth = (int) (titleRawWidth * titleScale);
        int titleScaledHeight = (int) (font.lineHeight * titleScale);

        // Description (not scaled)
        int descHeight = hasDescription ? font.lineHeight : 0;
        int descWidth = hasDescription ? font.width(confirmDesc) : 0;

        int maxContentWidth = Math.max(titleScaledWidth, descWidth);
        int textBlockHeight = titleScaledHeight + (hasDescription ? descHeight + spacingBetweenLines : 0);
        int buttonHeight = 20;

        int totalBoxWidth = maxContentWidth + padding * 2;
        int totalBoxHeight = textBlockHeight + spacingBetweenLines + buttonHeight + padding * 2;

        int x1 = centerX - totalBoxWidth / 2 + 15;
        int y1 = centerY - totalBoxHeight / 2;
        int x2 = centerX + totalBoxWidth / 2 + 15;
        int y2 = centerY + totalBoxHeight / 2;

        // Draw semi-transparent background
        guiGraphics.fill(x1, y1, x2 + 45, y2, 0xDD222222); // ~87% opaque dark gray

        // Render everything else
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }


    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
