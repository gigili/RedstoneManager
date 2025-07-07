package dev.igorilic.redstonemanager.screen.custom;

import com.google.common.graph.Network;
import dev.igorilic.redstonemanager.block.ModBlocks;
import dev.igorilic.redstonemanager.screen.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.security.Permission;
import java.util.List;
import java.util.Set;

public class ManagerMenu extends AbstractContainerMenu {
    List<ItemStack> itemStacks = List.of();
    private Level level;
    private ContainerData data;
    private Player player;
    private BlockPos blockPos;

    public ManagerMenu(int containerID, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerID, inventory, extraData.readBlockPos(), new SimpleContainerData(1));
    }

    public ManagerMenu(int containerID, Inventory inventory, BlockPos blockPos, ContainerData data) {
        super(ModMenuTypes.MANAGER_MENU.get(), containerID);
        this.player = inventory.player;
        this.blockPos = blockPos;
        this.level = inventory.player.level();
        this.data = data;

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        addDataSlots(data);

    }
/*
    List<ItemStack> getNetworkItems() {
        return networkHolder.getNetwork()
                .getItemDevices()
                .filter(device -> device.isValidDevice() && device.canExtract(DeviceType.ITEM))
                .map(ItemDevice::getItemHandler)
                .filter(Objects::nonNull)
                .map(handler -> {
                    List<ItemStack> stacks = new ArrayList<>();

                    for (int slot = 0; slot < handler.getSlots(); slot++) {
                        stacks.add(handler.getStackInSlot(slot).copy());
                    }

                    return stacks;
                })
                .flatMap(List::stream)
                .filter(item -> !item.isEmpty())
                .toList();
    }
    */

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    // must assign a slot number to each of the slots used by the GUI.
    // For this container, we can see both the tile inventory's slots as well as the player inventory slots and the hotbar.
    // Each time we add a Slot to the container, it automatically increases the slotIndex, which means
    //  0 - 8 = hotbar slots (which will map to the InventoryPlayer slot numbers 0 - 8)
    //  9 - 35 = player inventory slots (which map to the InventoryPlayer slot numbers 9 - 35)
    //  36 - 44 = TileInventory slots, which map to our TileEntity slot numbers 0 - 8)
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 3;  // must be the number of slots you have!

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), blockPos), player, ModBlocks.RM_MANAGER_BLOCK.get());
    }

    public void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18 + 17, 36 + (ManagerScreen.visibleRows * 18) + i * 18));
            }
        }
    }

    public void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18 + 17, 94  + (ManagerScreen.visibleRows * 18) ));
        }
    }

    public record ItemList(List<ItemStack> stacks) {}
}
