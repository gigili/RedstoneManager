package dev.igorilic.redstonemanager.item.custom.pouch.ui;

import dev.igorilic.redstonemanager.item.custom.RedstoneLinkerItem;
import dev.igorilic.redstonemanager.screen.ModMenuTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PouchMenu extends AbstractContainerMenu {
    private final ItemStack pouchStack;
    private final ItemStacksResourceHandler pouchHandler;

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
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

        NonNullList<ItemStack> initialStacks = NonNullList.withSize(TE_INVENTORY_SLOT_COUNT, ItemStack.EMPTY);
        ItemContainerContents contents = pouchStack.get(DataComponents.CONTAINER);
        if (contents != null) {
            for (int i = 0; i < Math.min(contents.getSlots(), TE_INVENTORY_SLOT_COUNT); i++) {
                initialStacks.set(i, contents.getStackInSlot(i));
            }
        }

        this.pouchHandler = new ItemStacksResourceHandler(initialStacks) {
            @Override
            public boolean isValid(int slot, @NotNull ItemResource resource) {
                return resource.isEmpty() || resource.toStack().getItem() instanceof RedstoneLinkerItem;
            }

            @Override
            protected int getCapacity(int slot, ItemResource resource) {
                return 64;
            }

            @Override
            protected void onContentsChanged(int slot, ItemStack previousContents) {
                super.onContentsChanged(slot, previousContents);
            }
        };

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
        addPouchSlots();
    }

    private void addPouchSlots() {
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = col + row * 9;
                this.addSlot(new PouchSlot(pouchHandler, slot, 8 + col * 18, 18 + row * 20));
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
                if (sourceStack.getItem() instanceof RedstoneLinkerItem) {
                    if (!moveLinkerToPouch(sourceStack)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            } else {
                if (sourceStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                ItemStack toExtract = sourceStack.copyWithCount(1);
                if (!this.moveItemStackTo(toExtract, 0, VANILLA_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
                slot.remove(1);
                itemstack = toExtract;
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
        if (!player.level().isClientSide()) {
            List<ItemStack> stacks = new ArrayList<>(pouchHandler.copyToList());
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
        for (int i = 0; i < pouchHandler.size(); i++) {
            ItemStack inSlot = ItemUtil.getStack(pouchHandler, i);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(source, inSlot)) {
                int space = 64 - inSlot.getCount();
                if (space > 0) {
                    int moveAmount = Math.min(source.getCount(), space);
                    pouchHandler.set(i, ItemResource.of(source), inSlot.getCount() + moveAmount);
                    source.shrink(moveAmount);
                }
            }
            if (source.isEmpty()) break;
        }

        if (!source.isEmpty()) {
            for (int i = 0; i < pouchHandler.size(); i++) {
                if (ItemUtil.getStack(pouchHandler, i).isEmpty()) {
                    pouchHandler.set(i, ItemResource.of(source), source.getCount());
                    source.setCount(0);
                    break;
                }
            }
        }

        return source.isEmpty();
    }
}
