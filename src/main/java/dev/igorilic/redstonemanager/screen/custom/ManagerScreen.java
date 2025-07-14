package dev.igorilic.redstonemanager.screen.custom;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.block.entity.RedstoneManagerBlockEntity;
import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.item.custom.RedstoneLinkerItem;
import dev.igorilic.redstonemanager.network.MenuInteractPacketC2S;
import dev.igorilic.redstonemanager.network.PacketCreateGroup;
import dev.igorilic.redstonemanager.network.PacketToggleAllLevers;
import dev.igorilic.redstonemanager.network.PacketToggleLever;
import dev.igorilic.redstonemanager.util.IUpdatable;
import dev.igorilic.redstonemanager.util.LinkerGroup;
import dev.igorilic.redstonemanager.util.MousePositionManagerUtil;
import dev.igorilic.redstonemanager.util.MouseUtil;
import dev.igorilic.redstonemanager.util.entries.DisplayEntry;
import dev.igorilic.redstonemanager.util.entries.HeaderEntry;
import dev.igorilic.redstonemanager.util.entries.ItemEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class ManagerScreen extends AbstractContainerScreen<ManagerMenu> implements IUpdatable {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/gui_no_slots_large.png");
    private static final ResourceLocation GUI_NO_SCROLL_TEXTURE = ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/gui_no_slots_no_scroll_large.png");
    private static final ResourceLocation GUI_SCROLL_TEXTURE = ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/gui_scroll.png");
    private static final ResourceLocation GUI_ROW_TEXTURE = ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/gui_row.png");
    private static final ResourceLocation GUI_TOGGLE_BUTTONS = ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/gui_toggle_buttons.png");

    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SCROLLBAR_X_OFFSET = 174; // adjust to fit GUI width
    private static final int SCROLLBAR_Y_OFFSET = 18;
    private static final int COLUMNS = 9;

    private final RedstoneManagerBlockEntity blockEntity;

    private int scrollIndex = 0;
    public static int visibleRows = 7;
    private final int itemsPerPage;

    private boolean isDraggingScrollbar = false;
    private int dragOffsetY = 0;

    private final List<DisplayEntry> flattenedEntries = new ArrayList<>();
    private Map<String, LinkerGroup> items;

    private void regenerateFlattenedEntries() {
        flattenedEntries.clear();
        for (Map.Entry<String, LinkerGroup> entry : items.entrySet()) {
            flattenedEntries.add(new HeaderEntry(entry.getKey()));
            List<ItemStack> stacks = entry.getValue().getItems();
            for (ItemStack stack : stacks) {
                flattenedEntries.add(new ItemEntry(stack));
            }

            int remainder = stacks.size() % 9;
            boolean lastIsEmpty = !stacks.isEmpty() && stacks.getLast().isEmpty();

            int toAdd = 9;

            if (remainder != 0) {
                toAdd = 9 - remainder;
            }

            if (lastIsEmpty) {
                toAdd = 0;
            }

            for (int i = 0; i < toAdd; i++) {
                flattenedEntries.add(new ItemEntry(ItemStack.EMPTY));
            }
        }
    }

    public ManagerScreen(ManagerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        MousePositionManagerUtil.setLastKnownPosition();
        this.blockEntity = menu.blockEntity;
        this.items = this.blockEntity.getItems();
        this.itemsPerPage = visibleRows * COLUMNS;
        this.imageHeight = 243;
        this.imageWidth = 193;
        this.inventoryLabelY = this.imageHeight - 96;
        regenerateFlattenedEntries();
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        Button btn = Button.builder(
                        Component.literal("New Group"),
                        (b) -> {
                            assert Minecraft.getInstance().player != null;
                            Minecraft.getInstance().player.connection.send(new PacketCreateGroup(blockEntity.getBlockPos(), "Default #" + ThreadLocalRandom.current().nextInt(1000)));
                        }
                )
                .bounds(leftPos + imageWidth - 84, topPos + 1, 60, 15)
                .build();
        this.addRenderableWidget(btn);
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
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int baseX = (width - imageWidth) / 2;
        int baseY = (height - imageHeight) / 2;

        if (canScroll()) {
            guiGraphics.blit(GUI_TEXTURE, baseX, baseY, 0, 0, imageWidth, imageHeight, 256, 256);
            drawScrollbar(guiGraphics);
        } else {
            guiGraphics.blit(GUI_NO_SCROLL_TEXTURE, baseX, baseY, 0, 0, imageWidth, imageHeight, 256, 256);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        //guiGraphics.enableScissor(leftPos + 7, topPos + 17, leftPos + 7 + 162, topPos + 17 + (18 * visibleRows));
        renderItems(guiGraphics, mouseX, mouseY);
        //guiGraphics.disableScissor();
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawScrollbar(GuiGraphics guiGraphics) {
        int scrollbarX = leftPos + SCROLLBAR_X_OFFSET;
        int scrollbarY = topPos + SCROLLBAR_Y_OFFSET;
        int handleHeight = 15;

        float maxScroll = getTotalRows() - visibleRows;
        float scrollFraction = scrollIndex / (maxScroll * COLUMNS); // use total index range
        int handleY = Math.max(scrollbarY + (int) ((getScrollbarHeight() - handleHeight) * scrollFraction), scrollbarY + 1);

        guiGraphics.blit(GUI_SCROLL_TEXTURE, scrollbarX, handleY, 168, 0, 12, 15, 12, 15);
    }

    private void renderItems(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int startX = leftPos + 7;
        int rowY = topPos + 19;

        int renderedRows = 0;
        int flattenedIndex = 0;
        int virtualRow = 0;

        while (flattenedIndex < flattenedEntries.size()) {
            DisplayEntry entry = flattenedEntries.get(flattenedIndex);

            // If the current virtual row is above the scroll, skip it
            if (virtualRow < scrollIndex / COLUMNS) {
                if (entry instanceof HeaderEntry) {
                    virtualRow++;
                    flattenedIndex++;
                } else if (entry instanceof ItemEntry) {
                    // Count up to one item row (COLUMNS entries)
                    int itemCount = 0;
                    while (flattenedIndex < flattenedEntries.size() && itemCount < COLUMNS) {
                        DisplayEntry subEntry = flattenedEntries.get(flattenedIndex);
                        if (!(subEntry instanceof ItemEntry)) break;
                        itemCount++;
                        flattenedIndex++;
                    }
                    virtualRow++;
                }
                continue;
            }

            // Render visible content after scrollIndex
            if (renderedRows >= visibleRows) break;

            if (entry instanceof HeaderEntry(String groupName)) {
                guiGraphics.drawString(font, groupName, startX + 3, rowY + 3, 0xFFFFFF);
                renderToggleButtons(guiGraphics, startX + 3, rowY + 3, groupName);
                rowY += 18;
                renderedRows++;
                flattenedIndex++;
                virtualRow++;
            } else if (entry instanceof ItemEntry) {
                int itemCount = 0;
                int rowStartIndex = flattenedIndex;

                while (itemCount < COLUMNS && flattenedIndex < flattenedEntries.size()) {
                    DisplayEntry subEntry = flattenedEntries.get(flattenedIndex);
                    if (!(subEntry instanceof ItemEntry(ItemStack stack))) break;

                    int col = itemCount;
                    int x = startX + col * 18;

                    if (col == 0) {
                        RenderSystem.setShaderTexture(0, GUI_ROW_TEXTURE);
                        guiGraphics.blit(GUI_ROW_TEXTURE, startX, rowY, 0, 0, 162, 18, 162, 18);
                    }

                    if (stack.getItem() instanceof RedstoneLinkerItem) {
                        guiGraphics.renderItem(stack, x + 1, rowY + 1);
                        renderLinkerItemBackground(stack, guiGraphics, x + 1, rowY + 1);

                        if (mouseX >= x && mouseX < x + 16 && mouseY >= rowY && mouseY < rowY + 16) {
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
                    }

                    itemCount++;
                    flattenedIndex++;
                }

                if (flattenedIndex > rowStartIndex) {
                    rowY += 18;
                    renderedRows++;
                    virtualRow++;
                } else {
                    // Avoid infinite loop
                    flattenedIndex++;
                }
            } else {
                flattenedIndex++; // Fallback safeguard
            }
        }
    }

    private void renderToggleButtons(@NotNull GuiGraphics guiGraphics, int startX, int startY, String groupName) {
        LinkerGroup linker = items.get(groupName);
        boolean allOn = linker.isPowered();

        if (allOn) {
            guiGraphics.blit(GUI_TOGGLE_BUTTONS, startX + 136, startY - 1, 0, 0, 20, 11, 41, 11);
        } else {
            guiGraphics.blit(GUI_TOGGLE_BUTTONS, startX + 136, startY - 1, 20.5f, 0, 20, 11, 41, 11);
        }
    }

    private void renderLinkerItemBackground(ItemStack linkerItem, GuiGraphics guiGraphics, int slotX, int slotY) {
        boolean isLeverOn = isLeverOn(linkerItem);
        int color = isLeverOn ? 0x7700FF00 : 0x77FF0000; // Green/Red with transparency
        int slotSize = 16;
        guiGraphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, color);
    }

    private int getScrollbarHeight() {
        return 18 * 3; // Was 18 * visibleRows, but we have scroll fixed to 3 rows height
    }

    private boolean isLeverOn(ItemStack stack) {
        if (minecraft == null || minecraft.level == null) return false;
        BlockPos leverPos = stack.get(ModDataComponents.COORDINATES);
        if (leverPos == null) return false;
        BlockState state = minecraft.level.getBlockState(leverPos);
        return state.is(Blocks.LEVER) && state.getValue(LeverBlock.POWERED);
    }

    private int getTotalRows() {
        int totalHeaders = (int) flattenedEntries.stream().filter(e -> e instanceof HeaderEntry).count();
        int totalItems = (int) flattenedEntries.stream().filter(e -> e instanceof ItemEntry).count();
        int itemRows = (totalItems + COLUMNS - 1) / COLUMNS;
        return totalHeaders + itemRows;
    }

    private boolean canScroll() {
        return getTotalRows() > visibleRows;
    }

    private boolean isShiftDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) ||
                InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MousePositionManagerUtil.getLastKnownPosition();

        int startX = leftPos + 9;
        int startY = topPos + 20;
        int slotSize = 18;

        ItemStack carried = menu.getCarried();
        int rowY = startY;
        int flattenedIndex = 0;
        int virtualRow = 0;
        int renderedRows = 0;
        String group = "";

        while (flattenedIndex < flattenedEntries.size()) {
            DisplayEntry entry = flattenedEntries.get(flattenedIndex);

            // Skip rows above scroll
            if (virtualRow < scrollIndex / COLUMNS) {
                if (entry instanceof HeaderEntry) {
                    virtualRow++;
                    flattenedIndex++;
                } else if (entry instanceof ItemEntry) {
                    int itemCount = 0;
                    while (itemCount < COLUMNS && flattenedIndex < flattenedEntries.size()) {
                        if (!(flattenedEntries.get(flattenedIndex) instanceof ItemEntry)) break;
                        itemCount++;
                        flattenedIndex++;
                    }
                    virtualRow++;
                }
                continue;
            }

            // Stop when we've handled visible rows
            if (renderedRows >= visibleRows) break;

            if (entry instanceof HeaderEntry(String groupName)) {
                int rowX = startX + 137;
                if (mouseX >= rowX && mouseX < rowX + 20 && mouseY >= rowY && mouseY < rowY + 11) {
                    assert Minecraft.getInstance().player != null;
                    Minecraft.getInstance().player.connection.send(new PacketToggleAllLevers(blockEntity.getBlockPos(), groupName));
                    return true;
                }

                rowY += 18;
                flattenedIndex++;
                virtualRow++;
                renderedRows++;
                group = groupName;
            } else if (entry instanceof ItemEntry) {
                int itemCount = 0;
                int rowStartIndex = flattenedIndex;

                while (itemCount < COLUMNS && flattenedIndex < flattenedEntries.size()) {
                    DisplayEntry subEntry = flattenedEntries.get(flattenedIndex);
                    if (!(subEntry instanceof ItemEntry(ItemStack stack))) break;

                    int x = startX + itemCount * slotSize;
                    int y = rowY;

                    if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                        LocalPlayer player = Minecraft.getInstance().player;
                        if (!stack.isEmpty()) {
                            if (button == 1) {
                                assert player != null;
                                player.connection.send(new PacketToggleLever(blockEntity.getBlockPos(), stack));
                            } else if (button == 0) {
                                assert player != null;
                                int clickType = carried.isEmpty() ? ClickType.PICKUP.ordinal() : ClickType.SWAP.ordinal();
                                if (isShiftDown()) {
                                    clickType = ClickType.QUICK_MOVE.ordinal();
                                }
                                player.connection.send(new MenuInteractPacketC2S(stack, clickType, 0, blockEntity.getBlockPos(), group));
                            }
                        } else if (!carried.isEmpty()) {
                            assert player != null;
                            player.connection.send(new MenuInteractPacketC2S(ItemStack.EMPTY, ClickType.PICKUP_ALL.ordinal(), 0, blockEntity.getBlockPos(), group));
                        }
                        return true;
                    }

                    itemCount++;
                    flattenedIndex++;
                }

                if (flattenedIndex > rowStartIndex) {
                    rowY += 18;
                    renderedRows++;
                    virtualRow++;
                } else {
                    flattenedIndex++; // Prevent infinite loop
                }
            } else {
                flattenedIndex++;
            }
        }

        // Scrollbar interaction
        int scrollbarX = leftPos + SCROLLBAR_X_OFFSET + 1;
        int scrollbarY = topPos + SCROLLBAR_Y_OFFSET;
        int handleHeight = 15;

        float maxScrollRows = Math.max(0, getTotalRows() - visibleRows);
        if (maxScrollRows > 0) {
            float scrollFraction = (float) (scrollIndex / COLUMNS) / maxScrollRows;
            int handleY = Math.max(scrollbarY + (int) ((getScrollbarHeight() - handleHeight) * scrollFraction), scrollbarY + 1);

            if (MouseUtil.isMouseOver(mouseX, mouseY, scrollbarX, handleY, SCROLLBAR_WIDTH, handleHeight)) {
                isDraggingScrollbar = true;
                dragOffsetY = (int) mouseY - handleY;
                return true;
            } else {
                isDraggingScrollbar = false;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar) {
            int scrollbarY = topPos + SCROLLBAR_Y_OFFSET;
            int handleHeight = 15;
            int maxScroll = Math.max(0, (getTotalRows() - visibleRows) * COLUMNS);

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
        if (!canScroll()) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);

        int totalRows = getTotalRows();
        int maxScrollRows = totalRows - visibleRows;
        int newRow = (scrollIndex / COLUMNS) - (int) Math.signum(scrollY);

        // Clamp to valid row range
        newRow = Mth.clamp(newRow, 0, maxScrollRows);
        scrollIndex = newRow * COLUMNS;

        return true;
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

    @Override
    public void update() {
        this.items = this.blockEntity.getItems();
        regenerateFlattenedEntries();
    }
}
