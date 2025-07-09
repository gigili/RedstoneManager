package dev.igorilic.redstonemanager.screen.custom;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.block.entity.RedstoneManagerBlockEntity;
import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.item.custom.RedstoneLinkerItem;
import dev.igorilic.redstonemanager.network.PacketToggleLever;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ManagerScreen extends AbstractContainerScreen<ManagerMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/single_slot.png");

    private Button toggleButton;
    private RedstoneManagerBlockEntity blockEntity;

    public ManagerScreen(ManagerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.blockEntity = menu.blockEntity;
    }

    @Override
    protected void init() {
        super.init();

        int buttonX = this.leftPos + 10;
        int buttonY = this.topPos + 50;

        this.toggleButton = Button.builder(
                        getButtonText(),
                        this::onToggleButtonPress
                )
                .bounds(buttonX, buttonY, 60, 20)
                .build();

        this.addRenderableWidget(this.toggleButton);
    }

    private Component getButtonText() {
        if (blockEntity == null || blockEntity.getLevel() == null) {
            return Component.translatable("errors.redstonemanager.manager.no_block_entity");
        }

        ItemStack linkerItem = blockEntity.inventory.getStackInSlot(0);
        if (linkerItem.isEmpty() || !(linkerItem.getItem() instanceof RedstoneLinkerItem)) {
            return Component.translatable("message.redstonemanager.manager.insert_linker");
        }

        BlockPos leverPos = linkerItem.get(ModDataComponents.COORDINATES);
        if (leverPos == null) {
            return Component.translatable("errors.redstonemanager.manager.not_linked");
        }

        BlockState leverState = blockEntity.getLevel().getBlockState(leverPos);
        if (!leverState.is(Blocks.LEVER)) {
            return Component.translatable("errors.redstonemanager.manager.invalid_link");
        }

        boolean isLeverOn = leverState.getValue(LeverBlock.POWERED);

        return isLeverOn ? Component.translatable("tooltip.redstonemanager.manager.turn_off") : Component.translatable("tooltip.redstonemanager.manager.turn_on");
    }

    private void onToggleButtonPress(Button button) {
        if (blockEntity == null || minecraft == null || minecraft.getConnection() == null) return;

        // Create and send the packet
        var packet = new PacketToggleLever(blockEntity.getBlockPos());
        minecraft.getConnection().send(packet);

        // Update button text optimistically
        this.toggleButton.setMessage(getButtonText());
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // Update button text periodically to reflect lever state changes
        if (this.toggleButton != null) {
            this.toggleButton.setMessage(getButtonText());
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

        // Render colored background for linked item
        renderLinkerItemBackground(guiGraphics, x, y);
    }

    private void renderLinkerItemBackground(GuiGraphics guiGraphics, int guiLeft, int guiTop) {
        ItemStack linkerItem = menu.getLinkerItem();
        if (!linkerItem.isEmpty() && linkerItem.getItem() instanceof RedstoneLinkerItem) {
            BlockPos leverPos = linkerItem.get(ModDataComponents.COORDINATES);
            if (leverPos != null) {
                boolean isLeverOn = isLeverOn(leverPos);
                int color = isLeverOn ? 0x7700FF00 : 0x77FF0000; // Green/Red with transparency
                int slotX = guiLeft + 80; // Adjust to your slot position
                int slotY = guiTop + 35;  // Adjust to your slot position
                guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, color);
            }
        }
    }

    private boolean isLeverOn(BlockPos leverPos) {
        if (minecraft == null || minecraft.level == null) return false;
        BlockState state = minecraft.level.getBlockState(leverPos);
        return state.is(Blocks.LEVER) && state.getValue(LeverBlock.POWERED);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        // Check if mouse is over the linker item slot
        if (isHovering(79, 34, 16, 16, mouseX, mouseY)) { // Adjust coordinates to match your slot
            ItemStack stack = menu.getLinkerItem();
            if (!stack.isEmpty() && stack.getItem() instanceof RedstoneLinkerItem) {
                guiGraphics.renderTooltip(
                        font,
                        getButtonText(),
                        mouseX, mouseY - 12
                );
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if clicked on linker item slot
        if (isHovering(79, 34, 16, 16, mouseX, mouseY)) { // Adjust coordinates to match your slot
            ItemStack stack = menu.getLinkerItem();
            if (!stack.isEmpty() && stack.getItem() instanceof RedstoneLinkerItem &&
                    stack.get(ModDataComponents.COORDINATES) != null) {
                // Send toggle packet
                minecraft.getConnection().send(new PacketToggleLever(blockEntity.getBlockPos()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
