package dev.igorilic.redstonemanager.item.custom.pouch.ui;

import dev.igorilic.redstonemanager.item.custom.RedstoneLinkerItem;
import dev.igorilic.redstonemanager.screen.ModMenuTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PouchMenu extends AbstractContainerMenu {
    private final ItemStack pouchStack;
    private final IItemHandler pouchHandler;

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    // must assign a slot number to each of the slots used by the GUI.
    // For this container, we can see both the tile inventory's slots as well as the player inventory slots and the hotbar.
    // Each time we add a Slot to the container, it automatically increases the slotIndex, which means
    //  0 - 26 = player inventory slots (which map to the InventoryPlayer slot numbers 9 - 35)
    //  27 - 35 = hotbar slots (which will map to the InventoryPlayer slot numbers 0 - 8)
    //  36 - 89 = Pouch slots, which map to our Pouch inventory slots 0 - 53)
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT = 54;

    public PouchMenu(int containerId, Inventory inv, ItemStack pouchStack) {
        super(ModMenuTypes.POUCH_MENU.get(), containerId);
        this.pouchStack = pouchStack;
        this.pouchHandler = new ItemStackHandler(TE_INVENTORY_SLOT_COUNT) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return stack.getItem() instanceof RedstoneLinkerItem;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            protected int getStackLimit(int slot, @NotNull ItemStack stack) {
                return 64;
            }

            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
            }
        };

        ItemContainerContents contents = pouchStack.get(DataComponents.CONTAINER);
        if (contents != null) {
            for (int i = 0; i < contents.getSlots(); i++) {
                if (i < pouchHandler.getSlots()) {
                    pouchHandler.insertItem(i, contents.getStackInSlot(i), false);
                }
            }
        }

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
        addPouchSlots(pouchHandler);
    }

    private void addPouchSlots(IItemHandler handler) {
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new PouchSlot(handler, col + row * 9, 8 + col * 18, 18 + row * 20));
            }
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack sourceStack = slot.getItem();
            itemstack = sourceStack.copy();

            if (index < TE_INVENTORY_FIRST_SLOT_INDEX) {
                // Player Inv -> Pouch
                if (sourceStack.getItem() instanceof RedstoneLinkerItem) {
                    if (!moveLinkerToPouch(sourceStack)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY; // Don't allow other items
                }
            } else {
                // Pouch -> Player Inv
                // Extract only 1
                ItemStack toExtract = sourceStack.copyWithCount(1);
                if (!this.moveItemStackTo(toExtract, 0, VANILLA_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
                sourceStack.shrink(1);
            }

            if (sourceStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < pouchHandler.getSlots(); i++) {
                stacks.add(pouchHandler.getStackInSlot(i));
            }
            pouchStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem() == pouchStack || player.getOffhandItem() == pouchStack;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 152 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 210));
        }
    }

    private boolean moveLinkerToPouch(ItemStack source) {
        boolean changed = false;

        for (int i = 0; i < pouchHandler.getSlots(); i++) {
            ItemStack inSlot = pouchHandler.getStackInSlot(i);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(source, inSlot)) {
                int space = 64 - inSlot.getCount();
                if (space > 0) {
                    int moveAmount = Math.min(source.getCount(), space);
                    inSlot.grow(moveAmount);
                    source.shrink(moveAmount);
                    changed = true;
                }
            }
            if (source.isEmpty()) break;
        }

        if (!source.isEmpty()) {
            for (int i = 0; i < pouchHandler.getSlots(); i++) {
                if (pouchHandler.getStackInSlot(i).isEmpty()) {
                    pouchHandler.insertItem(i, source.copy(), false);
                    source.setCount(0);
                    changed = true;
                    break;
                }
            }
        }

        return changed;
    }
}
