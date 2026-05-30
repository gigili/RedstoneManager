package dev.igorilic.redstonemanager.item.custom.pouch.ui;

import dev.igorilic.redstonemanager.RedstoneManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class PouchScreen extends AbstractContainerScreen<PouchMenu> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/rm-pouch-gui.png");

    public PouchScreen(PouchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 175, 233);
        this.inventoryLabelY = this.imageHeight - 91;
        this.titleLabelY = this.titleLabelY - 2;
    }

    protected void drawCustomBg(@NotNull GuiGraphicsExtractor guiGraphics, int screenX, int screenY, float partialTick) {
        int baseX = (width - imageWidth) / 2;
        int baseY = (height - imageHeight) / 2;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, baseX, baseY, 0, 0, imageWidth, imageHeight, 256, 256);
    }


    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int screenX, int screenY, float partialTick) {
        drawCustomBg(guiGraphics, screenX, screenY, partialTick);
        super.extractRenderState(guiGraphics, screenX, screenY, partialTick);
    }
}
