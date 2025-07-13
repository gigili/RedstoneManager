package dev.igorilic.redstonemanager.block.entity;

import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.item.custom.RedstoneLinkerItem;
import dev.igorilic.redstonemanager.screen.custom.ManagerMenu;
import dev.igorilic.redstonemanager.util.IUpdatable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RedstoneManagerBlockEntity extends BlockEntity implements MenuProvider {
    private final Map<String, List<ItemStack>> items = new HashMap<>();

    public RedstoneManagerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MANAGER_BE.get(), pos, blockState);
    }

    /*public void swapLinker(String groupName, int index, ItemStack newLinker) {
        if (index < 0 || index >= this.linkers.size()) {
            return;
        }

        // Validate the new item is a linker
        if (!(newLinker.getItem() instanceof RedstoneLinkerItem)) {
            return;
        }

        this.linkers.set(index, newLinker.copy());
        this.setChanged();

        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }*/

    public Map<String, List<ItemStack>> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public void createGroup(String groupName) {
        items.computeIfAbsent(groupName, k -> new ArrayList<>()).add(ItemStack.EMPTY);
        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public void addItemToGroup(String groupName, ItemStack item) {
        if (!(item.getItem() instanceof RedstoneLinkerItem)) return;
        this.items.computeIfAbsent(groupName, k -> new ArrayList<>()).add(item);

        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public void removeItemFromGroup(String groupName, ItemStack item) {
        if (!this.items.containsKey(groupName)) return;
        if (!(item.getItem() instanceof RedstoneLinkerItem)) return;

        int index = findLinkerIndex(this.items.get(groupName), item);

        if (index != -1) {
            this.items.get(groupName).remove(index);
            /*if (this.items.get(groupName).isEmpty()) {
                this.items.remove(groupName);
            }*/
        }

        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public int getAllItemSize() {
        return items.values().stream().mapToInt(List::size).sum();
    }

    public int findLinkerIndex(List<ItemStack> linkers, ItemStack target) {
        for (int i = 0; i < linkers.size(); i++) {
            ItemStack linker = linkers.get(i);
            if (linker.has(ModDataComponents.ITEM_UUID) && target.has(ModDataComponents.ITEM_UUID)) {
                if (Objects.equals(linker.get(ModDataComponents.ITEM_UUID), target.get(ModDataComponents.ITEM_UUID))) {
                    return i;
                }
            } else if (ItemStack.isSameItemSameComponents(linkers.get(i), target)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag groupList = new ListTag();

        for (Map.Entry<String, List<ItemStack>> entry : items.entrySet()) {
            CompoundTag groupTag = new CompoundTag();
            groupTag.putString("Name", entry.getKey());

            ListTag itemList = new ListTag();
            for (ItemStack stack : entry.getValue()) {
                itemList.add(stack.save(registries));
            }

            groupTag.put("Links", itemList);
            groupList.add(groupTag);
        }

        tag.put("Groups", groupList);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();

        if (tag.contains("Groups", Tag.TAG_LIST)) {
            ListTag groupList = tag.getList("Groups", Tag.TAG_COMPOUND);

            for (Tag groupTagBase : groupList) {
                CompoundTag groupTag = (CompoundTag) groupTagBase;
                String name = groupTag.getString("Name");

                List<ItemStack> stacks = new ArrayList<>();
                ListTag itemList = groupTag.getList("Links", Tag.TAG_COMPOUND);

                for (Tag itemTagBase : itemList) {
                    CompoundTag itemTag = (CompoundTag) itemTagBase;
                    ItemStack stack = ItemStack.parseOptional(registries, itemTag);
                    stacks.add(stack);
                }

                this.items.put(name, stacks);
            }
        }

        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof IUpdatable ui) {
                ui.update();
            }
        });
    }

    public void drops() {
        /*SimpleContainer inv = new SimpleContainer(linkers.size());
        int index = 0;
        for (Map.Entry<String, List<ItemStack>> entry : items.entrySet()) {
            for (ItemStack stack : entry.getValue()) {
                inv.setItem(index, stack);
                index++;
            }
        }

        if (level != null) {
            Containers.dropContents(level, worldPosition, inv);
        }*/
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("gui.redstonemanager.manager");
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return saveWithFullMetadata(registries);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        return new ManagerMenu(i, inventory, this);
    }

    public void toggleLinkedLever(ItemStack stack) {
        if (level == null || level.isClientSide) return;

        BlockPos leverPos = stack.get(ModDataComponents.COORDINATES);
        if (leverPos == null) return;

        BlockState state = level.getBlockState(leverPos);
        if (!state.is(Blocks.LEVER)) return;

        boolean isLeverPowered = state.getValue(LeverBlock.POWERED);
        level.setBlock(leverPos, state.setValue(LeverBlock.POWERED, !isLeverPowered), Block.UPDATE_ALL);
        level.playSound(null, leverPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, isLeverPowered ? 0.5F : 0.6F);
    }
}
