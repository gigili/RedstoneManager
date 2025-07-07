package dev.igorilic.redstonemanager.screen.custom;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.util.ItemCount;
import dev.igorilic.redstonemanager.util.MouseUtil;
import dev.igorilic.redstonemanager.util.NumbersUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ManagerScreen extends AbstractContainerScreen<ManagerMenu> {
    private EditBox searchBox;
    private List<ItemStack> allItems;
    private List<ItemStack> filteredItems;
    private int scrollIndex = 0;

    private static final int COLUMNS = 9;
    public static int visibleRows = 3;
    private int itemsPerPage = visibleRows * COLUMNS;

    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SCROLLBAR_X_OFFSET = 174; // adjust to fit GUI width
    private static final int SCROLLBAR_Y_OFFSET = 18;
    private boolean isDraggingScrollbar = false;
    private int dragOffsetY = 0;
    private int lastAllItemsSize = -1;
    private String lastSearchText = "";
    private ItemStack selected = ItemStack.EMPTY;

    private int settingsButtonX = 0;
    private int settingsButtonY = 2;
    private int rowButtonX = 0;
    private int rowButtonY = 20;


    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID,"textures/gui/interface_gui.png");
    private static final ResourceLocation ROW_SPRITE =
            ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID,"textures/gui/interface_row.png");
    static final ResourceLocation SCROLL_SPRITE =
            ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID,"textures/gui/interface_scroll.png");
    static final ResourceLocation SETTINGS_BUTTON =
            ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID,"textures/gui/button_settings.png");
    static final ResourceLocation ROW_BUTTON =
            ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID,"textures/gui/button_row.png");

    public ManagerScreen(ManagerMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.itemsPerPage = visibleRows * COLUMNS;
        this.allItems = getAllItems();
        System.out.println("Constructor allItems size: " + this.allItems.size());  // Debug
        this.filteredItems = new ArrayList<>(allItems);
        this.imageHeight = 175 + (visibleRows - 3) * 18;
        this.imageWidth = 209;
        this.inventoryLabelY = this.imageHeight - 98;
        this.inventoryLabelX = 25;
        this.titleLabelX = 25;
        this.lastSearchText = "";
    }

    public List<ItemStack> getAllItems() {
        return menu.itemStacks;
    }

    private int calculateMaxRows() {
        int windowHeight = this.minecraft.getWindow().getGuiScaledHeight();

        int topSectionHeight = 72;
        int bottomSectionHeight = 101;
        int searchBoxHeight = 20;
        int margin = 10;

        int availableHeight = windowHeight - topSectionHeight - bottomSectionHeight - searchBoxHeight - margin * 2;

        int maxRows = availableHeight / 18;
        return Math.max(3, maxRows);
    }

    private void cycleVisibleRows(int button) {
        int maxRows = calculateMaxRows();

        if (button == 0) { // Left click - cycle forward
            visibleRows++;
            if (visibleRows > maxRows) {
                visibleRows = 3; // min visible rows
            }
        } else if (button == 1) { // Right click - cycle backward
            visibleRows--;
            if (visibleRows < 3) {
                visibleRows = maxRows; // wrap to max
            }
        }

        updateGuiLayout();
    }

    private void updateGuiLayout() {
        itemsPerPage = visibleRows * COLUMNS;
        this.imageHeight = 175 + (visibleRows - 3) * 18;
        this.inventoryLabelY = this.imageHeight - 98;

        assert this.minecraft != null;
        this.topPos = (this.minecraft.getWindow().getGuiScaledHeight() - this.imageHeight) / 2;

        scrollIndex = 0;
        this.init();
    }

    @Override
    protected void init() {
        super.init();

        this.clearWidgets();
        this.menu.slots.clear();

        searchBox = new EditBox(font, leftPos + 86 + 37 , topPos + 3, 80, 14, Component.literal("Search"));
        searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBox);

        assert this.minecraft != null;
        assert this.minecraft.player != null;
        this.menu.addPlayerInventory(this.minecraft.player.getInventory());
        this.menu.addPlayerHotbar(this.minecraft.player.getInventory());

        onSearchChanged(searchBox.getValue());
        lastSearchText = searchBox.getValue();
    }


    private void onSearchChanged(String text) {
        lastSearchText = text;
        Map<Item, Integer> itemCounts = new LinkedHashMap<>();
        for (ItemStack stack : allItems) {
            itemCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        filteredItems = itemCounts.entrySet().stream()
                .filter(entry -> entry.getKey().getDescription().getString().toLowerCase().contains(text.toLowerCase()))
                .map(entry -> {
                    ItemStack stack = new ItemStack(entry.getKey(), entry.getValue());
                    stack.set(ModDataComponents.ITEM_COUNT.get(), new ItemCount(entry.getValue()));
                    return stack;
                })
                .toList();

        scrollIndex = 0;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int baseHeight = 72;
        int tileHeight = 18;
        int extraRows = visibleRows - 3;

        int slotRowWidth = 162;
        int slotRowHeight = 18;
        int startX = leftPos + 24;
        int startY = topPos + 19;

        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, baseHeight);

        int tileSourceX = 0;
        int tileSourceY = 54;
        int tileWidth = 209;

        for (int i = 0; i < extraRows; i++) {
            int drawY = topPos + baseHeight + (i * tileHeight);
            guiGraphics.blit(TEXTURE, leftPos, drawY, tileSourceX, tileSourceY, tileWidth, tileHeight);
        }

        int bottomSourceY = baseHeight + tileHeight - 18;
        int bottomDestY = topPos + baseHeight + (extraRows * tileHeight);
        int bottomHeight = 101;

        guiGraphics.blit(TEXTURE, leftPos, bottomDestY, 0, bottomSourceY, imageWidth, bottomHeight);

        int scrollbarX = leftPos + SCROLLBAR_X_OFFSET + 17;
        int scrollbarY = topPos + SCROLLBAR_Y_OFFSET;
        int handleHeight = 15;
        int maxScroll = Math.max(0, filteredItems.size() - itemsPerPage);
        if (maxScroll > 0) {
            float scrollPercent = scrollIndex / (float) maxScroll;
            int handleY = scrollbarY + (int) ((getScrollbarHeight() - handleHeight) * scrollPercent);
            guiGraphics.blit(SCROLL_SPRITE, scrollbarX, handleY + 2, 168, 0, 12, handleHeight, 12, 15);

        }

        RenderSystem.setShaderTexture(0, ROW_SPRITE); // Make sure to bind row texture
        for (int i = 0; i < visibleRows; i++) {
            int y = startY + (i * slotRowHeight);
            guiGraphics.blit(ROW_SPRITE, startX, y, 0, 0, slotRowWidth, slotRowHeight, slotRowWidth, slotRowHeight);
        }

        //Buttons
        guiGraphics.blit(SETTINGS_BUTTON, leftPos, topPos + 2, 0, 0, 17, 17, 17, 17);
        guiGraphics.blit(ROW_BUTTON, leftPos, topPos + 20, 0, 0, 17, 17, 17, 17);

    }


    private int getScrollbarHeight() {
        return 18 * visibleRows;
    }


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        searchBox.render(guiGraphics, mouseX, mouseY, partialTicks);

        int startX = leftPos + 25;
        int startY = topPos + 20; // Adjusted to fit the GUI layoute

        for (int i = 0; i < itemsPerPage; i++) {
            int index = scrollIndex + i;
            if (index >= filteredItems.size()) break;

            ItemStack stack = filteredItems.get(index);

            int row = i / COLUMNS;
            int col = i % COLUMNS;
            int x = startX + col * 18;
            int y = startY + row * 18;

            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                guiGraphics.renderTooltip(font, stack, mouseX, mouseY);
                selected = stack;
            } else {
                selected = ItemStack.EMPTY;
            }

            guiGraphics.renderItem(stack, x, y);
            renderAmount(guiGraphics, x, y, NumbersUtil.format(stack.getCount()), 0xFFFFFF);
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
        renderButtonTooltips(guiGraphics, mouseX, mouseY, leftPos, topPos);
    }

    public static void renderAmount(final GuiGraphics graphics, final int x, final int y, final String text, final int color) {
        renderAmount(graphics, x, y, text, color, text.length() <= 3);
    }

    public static void renderAmount(final GuiGraphics graphics, final int x, final int y, final String text, final int color, final boolean large) {
        final Font font = Minecraft.getInstance().font;
        final PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        // Large amounts overlap with the slot lines (see Minecraft behavior)
        poseStack.translate(x + (large ? 1D : 0D), y + (large ? 1D : 0D), 300);
        if (!large) {
            poseStack.scale(0.5F, 0.5F, 1);
        }
        graphics.drawString(font, text, (large ? 16 : 30) - font.width(text), large ? 8 : 22, color, true);
        poseStack.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int startX = leftPos + 25;
        int startY = topPos + 20;
        int slotSize = 18;


        if (button == 1 && searchBox != null) {
            if (searchBox.isMouseOver(mouseX, mouseY)) {
                searchBox.setValue("");
                onSearchChanged("");
                return true;
            }
        }

        if (button == 0 && searchBox != null && !searchBox.isMouseOver(mouseX, mouseY)) {
            searchBox.setFocused(false);
        }


        // Handle scrollbar click
        int scrollbarX = leftPos + SCROLLBAR_X_OFFSET + 17;
        int scrollbarY = topPos + SCROLLBAR_Y_OFFSET;
        int handleHeight = 15;
        int maxScroll = Math.max(0, filteredItems.size() - itemsPerPage);
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

        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    protected void containerTick() {

    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar) {
            int scrollbarY = topPos + SCROLLBAR_Y_OFFSET;
            int handleHeight = 15;
            int maxScroll = Math.max(0, filteredItems.size() - itemsPerPage);

            int relativeY = (int) mouseY - scrollbarY - dragOffsetY;
            float percent = Mth.clamp(relativeY / (float)(getScrollbarHeight() - handleHeight), 0.0F, 1.0F);

            int rawIndex = (int)(percent * maxScroll);
            scrollIndex = Mth.clamp((rawIndex / COLUMNS) * COLUMNS, 0, maxScroll);

            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, filteredItems.size() - itemsPerPage);
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

        if (searchBox.isFocused()) {
            if (searchBox.keyPressed(keyCode, scanCode, modifiers) || searchBox.canConsumeInput()) {
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderButtonTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {

        if (MouseUtil.isMouseAboveArea(mouseX, mouseY, x, y, settingsButtonX, settingsButtonY, 17, 17)) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.redstonemanager.settings_menu"), mouseX, mouseY);
        }
        if (MouseUtil.isMouseAboveArea(mouseX, mouseY, x, y, rowButtonX, rowButtonY, 17, 17)) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.redstonemanager.row_menu"), mouseX, mouseY);
        }
    }
}
