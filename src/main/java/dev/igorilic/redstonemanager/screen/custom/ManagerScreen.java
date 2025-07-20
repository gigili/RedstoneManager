package dev.igorilic.redstonemanager.screen.custom;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.block.entity.RedstoneManagerBlockEntity;
import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.item.custom.RedstoneLinkerItem;
import dev.igorilic.redstonemanager.network.*;
import dev.igorilic.redstonemanager.util.*;
import dev.igorilic.redstonemanager.util.entries.DisplayEntry;
import dev.igorilic.redstonemanager.util.entries.HeaderEntry;
import dev.igorilic.redstonemanager.util.entries.ItemEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ManagerScreen extends AbstractContainerScreen<ManagerMenu> implements IUpdatable {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/gui_no_slots_large.png");
    private static final ResourceLocation GUI_NO_SCROLL_TEXTURE = ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/gui_no_slots_no_scroll_large.png");
    private static final ResourceLocation GUI_SCROLL_TEXTURE = ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/gui_scroll.png");
    private static final ResourceLocation GUI_ROW_TEXTURE = ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/gui_row.png");
    private static final ResourceLocation GUI_TOGGLE_BUTTONS = ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/gui_toggle_buttons.png");
    private static final ResourceLocation GUI_BUTTONS = ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "textures/gui/gui_buttons.png");

    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SCROLLBAR_X_OFFSET = 174; // adjust to fit GUI width
    private static final int SCROLLBAR_Y_OFFSET = 18;
    private static final int COLUMNS = 9;

    private final RedstoneManagerBlockEntity blockEntity;

    private int scrollIndex = 0;
    public static int visibleRows = 7;

    private boolean isDraggingScrollbar = false;
    private int dragOffsetY = 0;

    private final List<DisplayEntry> flattenedEntries = new ArrayList<>();
    private Map<String, LinkerGroup> items;

    private boolean isEditingGroup = false;
    private String oldGroupName = "";

    private void regenerateFlattenedEntries() {
        flattenedEntries.clear();
        items.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    flattenedEntries.add(new HeaderEntry(entry.getKey()));
                    List<ItemStack> stacks = entry.getValue().getItems();
                    for (ItemStack stack : stacks) {
                        flattenedEntries.add(new ItemEntry(stack, entry.getKey()));
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
                        flattenedEntries.add(new ItemEntry(ItemStack.EMPTY, entry.getKey()));
                    }
                });
    }

    public ManagerScreen(ManagerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        MousePositionManagerUtil.setLastKnownPosition();
        this.blockEntity = menu.blockEntity;
        this.items = this.blockEntity.getItems();
        this.imageHeight = 243;
        this.imageWidth = 193;
        this.inventoryLabelY = this.imageHeight - 95;
        regenerateFlattenedEntries();

        assert Minecraft.getInstance().player != null;
        Minecraft.getInstance().player.connection.send(new PacketRefreshGroupPoweredState(blockEntity.getBlockPos()));
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
            scrollIndex = 0;
            guiGraphics.blit(GUI_NO_SCROLL_TEXTURE, baseX, baseY, 0, 0, imageWidth, imageHeight, 256, 256);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        //guiGraphics.enableScissor(leftPos + 7, topPos + 17, leftPos + 7 + 162, topPos + 17 + (18 * visibleRows));
        renderItems(guiGraphics, mouseX, mouseY, partialTick);
        //guiGraphics.disableScissor();
        renderTooltip(guiGraphics, mouseX, mouseY);

        int newButtonX = leftPos + imageWidth - 36;
        int newButtonY = topPos + 4;
        guiGraphics.blit(GUI_BUTTONS, newButtonX, newButtonY, 0, 0, 11, 11, 33, 11);
        if (mouseX >= newButtonX && mouseX < newButtonX + 11 && mouseY >= newButtonY && mouseY < newButtonY + 11) {
            guiGraphics.renderTooltip(font, Component.translatable("tooltip.redstonemanager.manager.create_group"), mouseX, mouseY);
        }
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

    private void renderItems(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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
                int maxVisibleWidth = 65;
                int fullTextWidth = font.width(groupName);

                if (fullTextWidth > maxVisibleWidth) {
                    int loopGap = 24;
                    int loopWidth = fullTextWidth + loopGap;

                    long time = System.currentTimeMillis();
                    int pixelOffset = (int) ((time / 20) % loopWidth);

                    int baseX = startX + 3;
                    int baseY = rowY + 5;

                    double scale = Minecraft.getInstance().getWindow().getGuiScale();
                    int scissorX = (int) (baseX * scale);
                    int scissorY = (int) ((this.height - baseY - font.lineHeight) * scale);
                    int scissorW = (int) (maxVisibleWidth * scale);
                    int scissorH = (int) (font.lineHeight * scale);

                    // Enable scissor with correct coords
                    RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);

                    String visible1 = getVisibleSubstring(font, groupName, pixelOffset, maxVisibleWidth);
                    guiGraphics.drawString(font, visible1, baseX, baseY, 0x3f3f3f, false);

                    int secondX = baseX + fullTextWidth + loopGap - pixelOffset;
                    if (secondX < baseX + maxVisibleWidth) {
                        String visible2 = getVisibleSubstring(font, groupName, 0, maxVisibleWidth);
                        guiGraphics.drawString(font, visible2, secondX, baseY, 0x3f3f3f, false);
                    }

                    RenderSystem.disableScissor();
                } else {
                    guiGraphics.drawString(font, groupName, startX + 3, rowY + 5, 0x3f3f3f, false);
                }

                renderToggleButtons(guiGraphics, startX + 3, rowY + 4, groupName, mouseX, mouseY, partialTick);
                renderGroupActionButtons(guiGraphics, startX + 3, rowY + 3, groupName, mouseX, mouseY);

                rowY += 18;
                renderedRows++;
                flattenedIndex++;
                virtualRow++;
            } else if (entry instanceof ItemEntry) {
                int itemCount = 0;
                int rowStartIndex = flattenedIndex;

                while (itemCount < COLUMNS && flattenedIndex < flattenedEntries.size()) {
                    DisplayEntry subEntry = flattenedEntries.get(flattenedIndex);
                    if (!(subEntry instanceof ItemEntry(ItemStack stack, String group))) break;

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
                            if (leverPos != null) {
                                LeverStateCache.requestIfNeeded(leverPos);

                                LeverStateCache.get(leverPos).ifPresentOrElse(cached -> {
                                    Component extraLabel;
                                    if (!cached.found()) {
                                        extraLabel = Component.translatable("errors.redstonemanager.manager.invalid_link");
                                    } else {
                                        extraLabel = cached.powered()
                                                ? Component.translatable("tooltip.redstonemanager.manager.turn_off")
                                                : Component.translatable("tooltip.redstonemanager.manager.turn_on");
                                    }
                                    guiGraphics.renderTooltip(font, extraLabel, mouseX, mouseY - 12);
                                }, () -> {
                                    Component extraLabel = Component.translatable("tooltip.redstonemanager.manager.loading");
                                    guiGraphics.renderTooltip(font, extraLabel, mouseX, mouseY - 12);
                                });
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

    private static String getVisibleSubstring(Font font, String text, int pixelOffset, int maxWidth) {
        if (pixelOffset <= 0) return font.plainSubstrByWidth(text, maxWidth);

        int skipped = 0;
        int start = 0;
        while (start < text.length() && skipped < pixelOffset) {
            skipped += font.width(String.valueOf(text.charAt(start)));
            start++;
        }

        String trimmed = text.substring(start);
        return font.plainSubstrByWidth(trimmed, maxWidth);
    }

    private void renderToggleButtons(@NotNull GuiGraphics guiGraphics, int startX, int startY, String groupName, int mouseX, int mouseY, float partialTick) {
        LinkerGroup linker = items.get(groupName);
        boolean allOn = linker.isPowered();

        if (allOn) {
            guiGraphics.blit(GUI_TOGGLE_BUTTONS, startX + 136, startY - 1, 0, 0, 20, 11, 41, 11);
        } else {
            guiGraphics.blit(GUI_TOGGLE_BUTTONS, startX + 136, startY - 1, 19.5f, 0, 21, 11, 41, 11);
        }

        int toggleX = startX + 139;
        int toggleY = startY + 2;
        if (mouseX >= toggleX && mouseX < toggleX + 20 && mouseY >= toggleY && mouseY < toggleY + 11) {
            Component toggleTooltip;
            if (items.get(groupName).isPowered()) {
                toggleTooltip = Component.translatable("tooltip.redstonemanager.manager.toggle_group_off", groupName);
            } else {
                toggleTooltip = Component.translatable("tooltip.redstonemanager.manager.toggle_group_on", groupName);
            }
            guiGraphics.renderTooltip(font, toggleTooltip, mouseX, mouseY);
        }
    }

    private void renderGroupActionButtons(@NotNull GuiGraphics guiGraphics, int startX, int rowY, String groupName, int mouseX, int mouseY) {
        // Edit button
        guiGraphics.blit(GUI_BUTTONS, startX + 70, rowY, 11, 0, 11, 11, 33, 11);

        if (mouseX >= startX + 70 && mouseX < startX + 81 && mouseY >= rowY && mouseY < rowY + 11) {
            guiGraphics.renderTooltip(font, Component.translatable("tooltip.redstonemanager.manager.rename_group", groupName), startX + 70, rowY + 2);
        }

        // Delete button
        guiGraphics.blit(GUI_BUTTONS, startX + 70 + 11 + 4, rowY, 22, 0, 11, 11, 33, 11);

        if (mouseX >= startX + 70 + 11 + 4 && mouseX < startX + 70 + 11 + 4 + 11 && mouseY >= rowY && mouseY < rowY + 11) {
            guiGraphics.renderTooltip(font, Component.translatable("tooltip.redstonemanager.manager.delete_group", groupName), startX + 70, rowY + 2);
        }
    }

    private void renderLinkerItemBackground(ItemStack linkerItem, GuiGraphics guiGraphics, int slotX, int slotY) {
        BlockPos leverPos = linkerItem.get(ModDataComponents.COORDINATES);
        int slotSize = 16;

        int color;
        if (leverPos != null) {
            LeverStateCache.requestIfNeeded(leverPos); // Triggers request if needed

            Optional<LeverStateCache.CachedLever> cached = LeverStateCache.get(leverPos);
            color = cached.map(cachedLever -> cachedLever.powered() ? 0x7700FF00 : 0x77FF0000).orElse(0x77444444);
        } else {
            color = 0x77000000; // black = not linked
        }

        guiGraphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, color);
    }

    private int getScrollbarHeight() {
        return 18 * 3; // Was 18 * visibleRows, but we have scroll fixed to 3 rows height
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

    private void handleCreateEditGroup(String newName) {
        if (newName.isEmpty()) return;

        /*if (newName.length() > 11) {
            newName = newName.substring(0, 11);
        }*/

        assert Minecraft.getInstance().player != null;
        if (isEditingGroup && !newName.equals(oldGroupName)) {
            Minecraft.getInstance().player.connection.send(new PacketRenameGroup(blockEntity.getBlockPos(), oldGroupName, newName));
        } else {
            Minecraft.getInstance().player.connection.send(new PacketCreateGroup(blockEntity.getBlockPos(), newName));
        }

        isEditingGroup = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MousePositionManagerUtil.getLastKnownPosition();

        int newButtonX = leftPos + imageWidth - 36;
        int newButtonY = topPos + 4;
        if (mouseX >= newButtonX && mouseX < newButtonX + 11 && mouseY >= newButtonY && mouseY < newButtonY + 11) {
            assert Minecraft.getInstance().player != null;
            Minecraft.getInstance().player.connection.send(new PacketPlaySound(blockEntity.getBlockPos(), Holder.direct(SoundEvents.UI_BUTTON_CLICK.value()), 0.3f, 1f));
            Minecraft.getInstance().setScreen(new CreateEditGroupScreen(this::handleCreateEditGroup, "", Minecraft.getInstance().screen));
            return true;
        }

        int startX = leftPos + 9;
        int startY = topPos + 20;
        int slotSize = 18;

        ItemStack carried = menu.getCarried();
        int rowY = startY;
        int flattenedIndex = 0;
        int virtualRow = 0;
        int renderedRows = 0;
        int currentSlotIndex = 0;

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

                // Edit button texture
                if (mouseX >= startX + 70 && mouseX < startX + 81 && mouseY >= rowY + 2 && mouseY < rowY + 13) {
                    isEditingGroup = true;
                    oldGroupName = groupName;
                    assert Minecraft.getInstance().player != null;
                    Minecraft.getInstance().player.connection.send(new PacketPlaySound(blockEntity.getBlockPos(), Holder.direct(SoundEvents.UI_BUTTON_CLICK.value()), 0.3f, 1f));
                    Minecraft.getInstance().setScreen(new CreateEditGroupScreen(this::handleCreateEditGroup, groupName, Minecraft.getInstance().screen));
                    return true;
                }

                // Delete button texture
                if (mouseX >= startX + 70 + 11 + 4 && mouseX < startX + 70 + 11 + 4 + 11 && mouseY >= rowY + 2 && mouseY < rowY + 13) {
                    assert Minecraft.getInstance().player != null;

                    Component text = Component.translatable("confirm.redstonemanager.manager.delete_group", groupName).plainCopy().withStyle(ChatFormatting.RED);
                    Component desc = Component.translatable("confirm.redstonemanager.manager.delete_group_desc").plainCopy().withStyle(ChatFormatting.AQUA);
                    Component action = Component.translatable("label.redstonemanager.manager.confirm").plainCopy().withStyle(ChatFormatting.RED);

                    Minecraft.getInstance().setScreen(new ConfirmActionScreen(
                            text,
                            desc,
                            action,
                            (confirmed) -> {
                                if (confirmed) {
                                    Minecraft.getInstance().player.connection.send(new PacketDeleteGroup(blockEntity.getBlockPos(), groupName));
                                }
                            },
                            Minecraft.getInstance().screen)
                    );
                    return true;
                }


                rowY += 18;
                flattenedIndex++;
                virtualRow++;
                renderedRows++;
            } else if (entry instanceof ItemEntry) {
                int itemCount = 0;
                int rowStartIndex = flattenedIndex;

                while (itemCount < COLUMNS && flattenedIndex < flattenedEntries.size()) {
                    DisplayEntry subEntry = flattenedEntries.get(flattenedIndex);
                    if (!(subEntry instanceof ItemEntry(ItemStack stack, String group))) break;

                    int x = startX + itemCount * slotSize;

                    if (mouseX >= x && mouseX < x + 16 && mouseY >= rowY && mouseY < rowY + 16) {
                        LocalPlayer player = Minecraft.getInstance().player;
                        if (!stack.isEmpty()) {
                            if (button == 1) { // RightClick
                                assert player != null;
                                player.connection.send(new PacketToggleLever(blockEntity.getBlockPos(), stack, group));
                            } else if (button == 0) { // LeftClick
                                assert player != null;
                                // PICKUP = Take item out of a manager / SWAP = Swap existing item with one in hand
                                int clickType = carried.isEmpty() ? ClickAction.PICKUP.ordinal() : ClickAction.SWAP.ordinal();
                                if (isShiftDown()) {
                                    // shift + click out of a manager
                                    clickType = ClickAction.QUICK_MOVE.ordinal();
                                }
                                player.connection.send(new PacketMoveItemBetweenInventories(stack, clickType, 0, blockEntity.getBlockPos(), group));
                            }
                        } else if (!carried.isEmpty()) {
                            assert player != null;
                            // Put item into a manager
                            CompoundTag extraData = new CompoundTag();
                            extraData.putInt("index", currentSlotIndex);
                            extraData.putInt("row", virtualRow);
                            player.connection.send(new PacketMoveItemBetweenInventories(ItemStack.EMPTY, extraData, ClickAction.PUT_INTO_MANAGER.ordinal(), 0, blockEntity.getBlockPos(), group));
                        }
                        return true;
                    }

                    itemCount++;
                    flattenedIndex++;
                    currentSlotIndex++;
                }

                if (flattenedIndex > rowStartIndex) {
                    rowY += 18;
                    renderedRows++;
                    virtualRow++;
                    currentSlotIndex = 0;
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
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void update() {
        this.items = this.blockEntity.getItems();
        regenerateFlattenedEntries();
    }
}
