package dev.igorilic.redstonemanager.screen.custom;

import dev.igorilic.redstonemanager.block.ModBlocks;
import dev.igorilic.redstonemanager.block.entity.RedstoneManagerBlockEntity;
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

public class ManagerMenu extends AbstractContainerMenu implements Interact {
    public final RedstoneManagerBlockEntity blockEntity;
    private final Level level;

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

    public ManagerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ManagerMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.MANAGER_MENU.get(), containerId);
        this.blockEntity = ((RedstoneManagerBlockEntity) blockEntity);
        this.level = inv.player.level();

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
            String groupName = "Default";
            if (blockEntity.getItems().entrySet().stream().findFirst().isPresent()) {
                groupName = blockEntity.getItems().entrySet().stream().findFirst().get().getKey();
            }

            blockEntity.addItemToGroup(groupName, sourceStack);
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
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 160 + i * 18));
            }
        }
    }

    public void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 218));
        }
    }

    @Override
    public void clicked(ItemStack itemStack, CompoundTag extraData, ClickType clickType, int button, String groupName) {
        switch (clickType) {
            case PICKUP -> { // Take item out of a manager
                if (getCarried().isEmpty() && itemStack != null) {
                    final var result = itemStack.copy();
                    if (!result.isEmpty()) {
                        setCarried(result);
                        blockEntity.removeItemFromGroup(groupName, itemStack);
                    }
                }
            }
            case PICKUP_ALL -> { // Put item into a manager
                if (!getCarried().isEmpty() && itemStack.isEmpty()) {
                    final var result = getCarried().copy();
                    if (!result.isEmpty()) {
                        setCarried(ItemStack.EMPTY);
                        blockEntity.addItemToGroup(groupName, result);
                    }
                }
            }
            case SWAP -> {
                if (!getCarried().isEmpty() && itemStack != null) {
                    final var result = getCarried().copy();
                    //TODO: Reimplement this
                    /*if (!result.isEmpty()) {
                        setCarried(itemStack);
                        blockEntity.swapLinker(blockEntity.findLinkerIndex(blockEntity.getLinkers(), itemStack), result);
                    }*/
                }
            }
            case QUICK_MOVE -> { // shift + click out of a manager
                ItemStack itemToRemove = itemStack.copy();
                if (moveItemStackTo(itemStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                    blockEntity.removeItemFromGroup(groupName, itemToRemove);
                    setCarried(ItemStack.EMPTY);
                }
            }
            case null, default -> {
            }
        }
    }
}
