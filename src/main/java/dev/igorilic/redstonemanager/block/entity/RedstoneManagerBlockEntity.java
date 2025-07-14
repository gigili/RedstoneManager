package dev.igorilic.redstonemanager.block.entity;

import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.item.custom.RedstoneLinkerItem;
import dev.igorilic.redstonemanager.screen.custom.ManagerMenu;
import dev.igorilic.redstonemanager.util.IUpdatable;
import dev.igorilic.redstonemanager.util.LinkerGroup;
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
    private final Map<String, LinkerGroup> items = new HashMap<>();

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

    public Map<String, LinkerGroup> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public void createGroup(String groupName) {
        items.computeIfAbsent(groupName, k -> new LinkerGroup(groupName)).addItem(ItemStack.EMPTY);
        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public void addItemToGroup(String groupName, ItemStack item) {
        if (!(item.getItem() instanceof RedstoneLinkerItem)) return;
        this.items.computeIfAbsent(groupName, k -> new LinkerGroup(groupName)).addItem(item);

        updateGroupPoweredState(groupName);

        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    private void updateGroupPoweredState(String groupName) {
        if (level == null) return;
        boolean isOn = false;
        for (ItemStack stack : this.items.get(groupName).getItems()) {
            if (!(stack.getItem() instanceof RedstoneLinkerItem)) continue;

            BlockPos leverPos = stack.get(ModDataComponents.COORDINATES);

            if (leverPos == null) continue;


            BlockState state = level.getBlockState(leverPos);
            if (!state.is(Blocks.LEVER)) continue;

            if (state.getValue(LeverBlock.POWERED)) {
                isOn = true;
                break;
            }
        }

        this.items.get(groupName).setPowered(isOn);
    }

    public void removeItemFromGroup(String groupName, ItemStack item) {
        if (!this.items.containsKey(groupName)) return;
        if (!(item.getItem() instanceof RedstoneLinkerItem)) return;

        items.get(groupName).removeItem(item);

        boolean isEmpty = items.get(groupName).getItems().isEmpty() || items.get(groupName).getItems().stream().allMatch(ItemStack::isEmpty);
        if (isEmpty) {
            this.items.remove(groupName);
        }

        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public int getAllItemSize() {
        return items.values().stream().map(LinkerGroup::getItems).mapToInt(List::size).sum();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag groupList = new ListTag();

        for (Map.Entry<String, LinkerGroup> entry : items.entrySet()) {
            CompoundTag groupTag = new CompoundTag();
            groupTag.putString("Name", entry.getKey());
            groupTag.putBoolean("IsPowered", entry.getValue().isPowered());

            ListTag itemList = new ListTag();
            for (ItemStack stack : entry.getValue().getItems()) {
                if (stack == ItemStack.EMPTY) continue;
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
                boolean isPowered = groupTag.getBoolean("IsPowered");

                List<ItemStack> stacks = new ArrayList<>();
                ListTag itemList = groupTag.getList("Links", Tag.TAG_COMPOUND);

                for (Tag itemTagBase : itemList) {
                    CompoundTag itemTag = (CompoundTag) itemTagBase;
                    ItemStack stack = ItemStack.parseOptional(registries, itemTag);
                    if (stack == ItemStack.EMPTY) continue;
                    stacks.add(stack);
                }


                this.items.put(name, new LinkerGroup(name, isPowered, stacks));
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

        updateGroupPoweredState(findGroupByLever(stack));
    }

    public void toggleAllLinkedLever(String groupName) {
        if (level == null || level.isClientSide) return;
        if (!items.containsKey(groupName)) return;

        boolean allOn = items.get(groupName).isPowered();

        for (ItemStack stack : items.get(groupName).getItems()) {
            if (!(stack.getItem() instanceof RedstoneLinkerItem)) continue;

            BlockPos leverPos = stack.get(ModDataComponents.COORDINATES);

            if (leverPos == null) continue;

            BlockState state = level.getBlockState(leverPos);
            if (!state.is(Blocks.LEVER)) continue;

            level.setBlock(leverPos, state.setValue(LeverBlock.POWERED, !allOn), Block.UPDATE_ALL);
        }

        items.get(groupName).setPowered(!allOn);
        level.playSound(null, getBlockPos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, !allOn ? 0.5F : 0.6F);

        updateGroupPoweredState(groupName);
    }

    public String findGroupByLever(ItemStack item) {
        for (LinkerGroup group : items.values()) {
            int index = group.findLinkerIndex(item);
            if (index == -1) continue;
            return group.getGroupName();
        }
        return null;
    }
}
