package dev.igorilic.redstonemanager.item.custom.pouch.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.igorilic.redstonemanager.RedstoneManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class PouchScreen extends AbstractContainerScreen<PouchMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/rm-pouch-gui.png");

    public PouchScreen(PouchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 233;
        this.imageWidth = 175;
        this.inventoryLabelY = this.imageHeight - 91;
        this.titleLabelY = this.titleLabelY - 2;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float v, int i, int i1) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int baseX = (width - imageWidth) / 2;
        int baseY = (height - imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, baseX, baseY, 0, 0, imageWidth, imageHeight, 256, 256);
    }


    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
