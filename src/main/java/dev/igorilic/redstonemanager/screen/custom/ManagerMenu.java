package dev.igorilic.redstonemanager.screen.custom;

import dev.igorilic.redstonemanager.block.ModBlocks;
import dev.igorilic.redstonemanager.block.entity.RedstoneManagerBlockEntity;
import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.item.custom.RedstoneLinkerItem;
import dev.igorilic.redstonemanager.screen.ModMenuTypes;
import dev.igorilic.redstonemanager.util.Interact;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ManagerMenu extends AbstractContainerMenu implements Interact {
    public final RedstoneManagerBlockEntity blockEntity;
    private final Level level;
    private final Inventory playerInventory;

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    // must assign a slot number to each of the slots used by the GUI.
    // For this container, we can see both the tile inventory's slots and the player inventory slots and the hotbar.
    // Each time we add a Slot to the container, it automatically increases the slotIndex, which means
    //  0-8 = hotbar slots (which will map to the InventoryPlayer slot numbers 0-8)
    //  9-35 = player inventory slots (which map to the InventoryPlayer slot numbers 9-35)
    //  36-44 = TileInventory slots, which map to our TileEntity slot numbers 0-8)
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 3;  // must be the number of slots you have!

    public ManagerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ManagerMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.MANAGER_MENU.get(), containerId);
        this.blockEntity = ((RedstoneManagerBlockEntity) blockEntity);
        this.level = inv.player.level();
        this.playerInventory = inv;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        if (!(sourceStack.getItem() instanceof RedstoneLinkerItem)) return ItemStack.EMPTY;

        // From player to manager
        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            blockEntity.addLinker(sourceStack);
            sourceSlot.set(ItemStack.EMPTY);
            return ItemStack.EMPTY;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.RM_MANAGER_BLOCK.get());
    }

    public void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 9 + l * 18, 90 + i * 18));
            }
        }
    }

    public void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 9 + i * 18, 148));
        }
    }

    @Override
    public void clicked(ItemStack itemStack, CompoundTag extraData, ClickType clickType, int button) {
        switch (clickType) {
            case PICKUP -> {
                if (getCarried().isEmpty() && itemStack != null) {
                    final var result = itemStack.copy();
                    if (!result.isEmpty()) {
                        setCarried(result);
                        blockEntity.removeLinker(findLinkerIndex(blockEntity.getLinkers(), itemStack));
                    }
                }
            }
            case SWAP -> {
                if (!getCarried().isEmpty() && itemStack != null) {
                    final var result = getCarried().copy();
                    if (!result.isEmpty()) {
                        setCarried(itemStack);
                        blockEntity.swapLinker(findLinkerIndex(blockEntity.getLinkers(), itemStack), result);
                    }
                }
            }
            case QUICK_MOVE -> {
                int linkerIndex = findLinkerIndex(blockEntity.getLinkers(), itemStack);
                if (moveItemStackTo(itemStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                    blockEntity.removeLinker(linkerIndex);
                    setCarried(ItemStack.EMPTY);
                }
            }
            case null, default -> {
            }
        }
    }

    public int findLinkerIndex(List<ItemStack> linkers, ItemStack target) {
        for (int i = 0; i < linkers.size(); i++) {
            ItemStack linker = linkers.get(i);
            if (linker.has(ModDataComponents.ITEM_UUID) && target.has(ModDataComponents.ITEM_UUID)) {
                if (linker.get(ModDataComponents.ITEM_UUID).equals(target.get(ModDataComponents.ITEM_UUID))) {
                    return i;
                }
            } else if (ItemStack.isSameItemSameComponents(linkers.get(i), target)) {
                return i;
            }
        }
        return -1;
    }
}
