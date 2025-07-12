package dev.igorilic.redstonemanager.screen.custom;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.block.entity.RedstoneManagerBlockEntity;
import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.network.MenuInteractPacketC2S;
import dev.igorilic.redstonemanager.network.PacketToggleLever;
import dev.igorilic.redstonemanager.util.MousePositionManagerUtil;
import dev.igorilic.redstonemanager.util.MouseUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ManagerScreen extends AbstractContainerScreen<ManagerMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/gui_no_slots_large.png");
    private static final ResourceLocation GUI_NO_SCROLL_TEXTURE = ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/gui_no_scroll.png");
    private static final ResourceLocation GUI_SCROLL_TEXTURE = ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/gui_scroll.png");
    private static final ResourceLocation GUI_ROW_TEXTURE = ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/gui_row.png");

    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SCROLLBAR_X_OFFSET = 174; // adjust to fit GUI width
    private static final int SCROLLBAR_Y_OFFSET = 18;
    private static final int COLUMNS = 9;

    private final RedstoneManagerBlockEntity blockEntity;

    private final List<ItemStack> linkers;

    private int scrollIndex = 0;
    public static int visibleRows = 7;
    private final int itemsPerPage;

    private boolean isDraggingScrollbar = false;
    private int dragOffsetY = 0;

    private Inventory playerInventory;

    public ManagerScreen(ManagerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        MousePositionManagerUtil.setLastKnownPosition();
        this.blockEntity = menu.blockEntity;
        this.linkers = this.blockEntity.getLinkers();
        this.itemsPerPage = visibleRows * COLUMNS;
        this.imageHeight = 243;
        this.imageWidth = 209;
        this.inventoryLabelY = this.imageHeight - 96;
        this.playerInventory = playerInventory;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);


        int slotRowWidth = 162;
        int slotRowHeight = 18;
        int startX = leftPos + 7;
        int startY = topPos + 17;

        int baseX = (width - imageWidth) / 2;
        int baseY = (height - imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, baseX, baseY, 0, 0, imageWidth, imageHeight, 256, 256);

        int scrollbarX = leftPos + SCROLLBAR_X_OFFSET;
        int scrollbarY = topPos + SCROLLBAR_Y_OFFSET;
        int handleHeight = 15;

        int scrollbarHeight = getScrollbarHeight();
        float maxScroll = (float) ((linkers.size() + COLUMNS - 1) / COLUMNS - visibleRows);
        float scrollFraction = scrollIndex / (maxScroll * COLUMNS); // use total index range

        if (maxScroll > 0) {
            int handleY = Math.max(scrollbarY + (int) ((scrollbarHeight - handleHeight) * scrollFraction), scrollbarY + 1);
            guiGraphics.blit(GUI_SCROLL_TEXTURE, scrollbarX, handleY, 168, 0, 12, handleHeight, 12, 15);
        }

        RenderSystem.setShaderTexture(0, GUI_ROW_TEXTURE); // Make sure to bind row texture
        for (int i = 0; i < visibleRows; i++) {
            int y = startY + (i * slotRowHeight);
            guiGraphics.blit(GUI_ROW_TEXTURE, startX, y, 0, 0, slotRowWidth, slotRowHeight, slotRowWidth, slotRowHeight);
        }
    }

    private int getScrollbarHeight() {
        return 18 * 3; // Was 18 * visibleRows, but we have scroll fixed to 3 rows height
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int startX = leftPos + 8;
        int startY = topPos + 18;

        for (int i = 0; i < itemsPerPage; i++) {
            int index = scrollIndex + i;
            if (index >= linkers.size()) break;

            ItemStack stack = linkers.get(index);

            int row = i / COLUMNS;
            int col = i % COLUMNS;

            int x = startX + col * 18;
            int y = startY + row * 18;

            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                BlockPos leverPos = stack.get(ModDataComponents.COORDINATES);
                Component extraLabel = null;
                if (leverPos == null) {
                    extraLabel = Component.translatable("errors.redstonemanager.manager.not_linked");
                } else if (blockEntity.getLevel() != null) {
                    BlockState leverState = blockEntity.getLevel().getBlockState(leverPos);
                    if (!leverState.is(Blocks.LEVER)) {
                        extraLabel = Component.translatable("errors.redstonemanager.manager.invalid_link");
                    } else {
                        boolean isLeverOn = isLeverOn(stack);
                        extraLabel = isLeverOn ? Component.translatable("tooltip.redstonemanager.manager.turn_off") : Component.translatable("tooltip.redstonemanager.manager.turn_on");
                    }
                }

                if (extraLabel != null) {
                    guiGraphics.renderTooltip(font, extraLabel, mouseX, mouseY - 12);
                }
                guiGraphics.renderTooltip(font, stack, mouseX, mouseY);
            }

            renderLinkerItemBackground(stack, guiGraphics, x, y);
            guiGraphics.renderItem(stack, x, y);
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderLinkerItemBackground(ItemStack linkerItem, GuiGraphics guiGraphics, int slotX, int slotY) {
        boolean isLeverOn = isLeverOn(linkerItem);
        int color = isLeverOn ? 0x7700FF00 : 0x77FF0000; // Green/Red with transparency
        int slotSize = 16;
        guiGraphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, color);
    }

    private boolean isLeverOn(ItemStack stack) {
        if (minecraft == null || minecraft.level == null) return false;
        BlockPos leverPos = stack.get(ModDataComponents.COORDINATES);
        if (leverPos == null) return false;
        BlockState state = minecraft.level.getBlockState(leverPos);
        return state.is(Blocks.LEVER) && state.getValue(LeverBlock.POWERED);
    }

    private void clampScrollIndex() {
        int totalRows = (linkers.size() + COLUMNS - 1) / COLUMNS;
        int maxStartRow = Math.max(0, totalRows - visibleRows);
        int maxScrollIndex = maxStartRow * COLUMNS;

        scrollIndex = Mth.clamp(scrollIndex, 0, maxScrollIndex);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MousePositionManagerUtil.getLastKnownPosition();

        int startX = leftPos + 9;
        int startY = topPos + 20;
        int slotSize = 18;

        ItemStack carried = menu.getCarried();
        for (int i = 0; i < itemsPerPage; i++) {
            int index = scrollIndex + i;

            int row = i / COLUMNS;
            int col = i % COLUMNS;
            int x = startX + col * slotSize;
            int y = startY + row * slotSize;

            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                if (index < linkers.size()) {
                    ItemStack stack = linkers.get(index);
                    if (!stack.isEmpty() && button == 1) {
                        assert Minecraft.getInstance().player != null;
                        Minecraft.getInstance().player.connection.send(
                                new PacketToggleLever(blockEntity.getBlockPos(), stack)
                        );
                    } else if (!stack.isEmpty() && button == 0) {
                        assert Minecraft.getInstance().player != null;
                        int clickType = carried.isEmpty() ? ClickType.PICKUP.ordinal() : ClickType.SWAP.ordinal();
                        if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) ||
                                InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT)) {
                            clickType = ClickType.QUICK_MOVE.ordinal();
                        }

                        Minecraft.getInstance().player.connection.send(
                                new MenuInteractPacketC2S(stack, clickType, 0)
                        );
                    }
                }
                return true;
            }
        }


        // Handle scrollbar click
        int scrollbarX = leftPos + SCROLLBAR_X_OFFSET + 1;
        int scrollbarY = topPos + SCROLLBAR_Y_OFFSET;
        int handleHeight = 15;
        int maxScroll = Math.max(0, linkers.size() - itemsPerPage);
        if (maxScroll > 0) {
            float scrollPercent = scrollIndex / (float) maxScroll;
            int handleY = scrollbarY + (int) ((getScrollbarHeight() - handleHeight) * scrollPercent);

            if (MouseUtil.isMouseOver(mouseX, mouseY, scrollbarX, handleY, SCROLLBAR_WIDTH, handleHeight)) {
                isDraggingScrollbar = true;
                dragOffsetY = (int) mouseY - handleY;
                return true;
            } else {
                isDraggingScrollbar = false;
            }
        }

        if (button == 0) {
            clampScrollIndex();
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar) {
            int scrollbarY = topPos + SCROLLBAR_Y_OFFSET;
            int handleHeight = 15;
            int maxScroll = Math.max(0, ((linkers.size() + COLUMNS - 1) / COLUMNS - visibleRows) * COLUMNS);

            int relativeY = (int) mouseY - scrollbarY - dragOffsetY;
            float percent = Mth.clamp(relativeY / (float) (getScrollbarHeight() - handleHeight), 0.0F, 1.0F);

            int rawIndex = (int) (percent * maxScroll);
            scrollIndex = Mth.clamp((rawIndex / COLUMNS) * COLUMNS, 0, maxScroll);

            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, ((linkers.size() + COLUMNS - 1) / COLUMNS - visibleRows) * COLUMNS);
        if (maxScroll > 0) {
            // Scroll by whole rows
            int newScrollIndex = scrollIndex - (int) scrollY * COLUMNS;
            // Clamp within bounds
            newScrollIndex = Mth.clamp(newScrollIndex, 0, maxScroll);
            // Snap to multiple of COLUMNS (whole rows)
            scrollIndex = (newScrollIndex / COLUMNS) * COLUMNS;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        /*if (groupNameField.isFocused()) {
            if (groupNameField.keyPressed(keyCode, scanCode, modifiers) || groupNameField.canConsumeInput()) {
                return true;
            }
        }*/

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
