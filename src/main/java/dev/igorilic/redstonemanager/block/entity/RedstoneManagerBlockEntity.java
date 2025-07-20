package dev.igorilic.redstonemanager.block.entity;

import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.item.custom.RedstoneLinkerItem;
import dev.igorilic.redstonemanager.screen.custom.ManagerMenu;
import dev.igorilic.redstonemanager.util.ChunkHandler;
import dev.igorilic.redstonemanager.util.IUpdatable;
import dev.igorilic.redstonemanager.util.LeverStateCache;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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

    public void swapLinker(String groupName, ItemStack existingItem, ItemStack inHand) {
        if (!items.containsKey(groupName)) return; // Group doesn't exist
        if (existingItem.isEmpty() || inHand.isEmpty()) return; // None of the items can't be empty for swap to work
        if (!(existingItem.getItem() instanceof RedstoneLinkerItem) || !(inHand.getItem() instanceof RedstoneLinkerItem))
            return; // Must be valid item

        LinkerGroup group = items.get(groupName);
        int existingItemIndex = group.findLinkerIndex(existingItem);

        if (existingItemIndex == -1) return; // Can't locate an existing item in group

        items.get(groupName).getItems().set(existingItemIndex, inHand.copy());

        this.setChanged();

        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

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

    public void renameGroup(String oldName, String newName) {
        if (!items.containsKey(oldName)) return;
        if (newName.isEmpty()) return;

        LinkerGroup group = items.get(oldName);
        group.setGroupName(newName);

        items.remove(oldName);
        items.put(newName, group);
    }

    public void deleteGroup(String groupName) {
        if (!items.containsKey(groupName)) return;
        LinkerGroup group = items.get(groupName);
        List<ItemStack> stacks = group.getItems();

        if (!stacks.isEmpty()) {
            SimpleContainer inv = new SimpleContainer(stacks.size());
            int index = 0;
            for (ItemStack stack : stacks) {
                inv.setItem(index, stack);
                index++;
            }

            if (level != null) {
                Containers.dropContents(level, worldPosition, inv);
            }
        }

        items.remove(groupName);
    }

    private void updateGroupPoweredState(String groupName) {
        updateGroupPoweredState(groupName, true);
    }

    public void updateGroupPoweredState() {
        for (LinkerGroup group : items.values()) {
            updateGroupPoweredState(group.getGroupName(), true);
        }
    }

    private void updateGroupPoweredState(String groupName, Boolean changed) {
        if (level == null || level.isClientSide) return;
        if (!items.containsKey(groupName)) return;

        ItemStack item = items.get(groupName).getItems().stream().filter(stack -> !stack.isEmpty() && stack.has(ModDataComponents.COORDINATES)).findFirst().orElse(ItemStack.EMPTY);
        ChunkHandler.tempLoadChunk(((ServerLevel) level), item, (loadedLevel) -> {
            boolean isOn = false;
            for (ItemStack stack : items.get(groupName).getItems()) {
                if (!(stack.getItem() instanceof RedstoneLinkerItem)) continue;

                BlockPos leverPos = stack.get(ModDataComponents.COORDINATES);

                if (leverPos == null) continue;

                BlockState state = loadedLevel.getBlockState(leverPos);
                if (!LinkerGroup.canLink(state)) continue;

                if (state.getValue(LeverBlock.POWERED)) {
                    isOn = true;
                }

                List<String> otherGroups = findAllGroupsForLever(stack);
                for (String otherGroup : otherGroups) {
                    if (!otherGroup.equals(groupName) && changed) {
                        updateGroupPoweredState(otherGroup, false);
                    }
                }
            }

            this.items.get(groupName).setPowered(isOn);
            if (changed) {
                setChanged();
            }
        });
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
        SimpleContainer inv = new SimpleContainer(getAllItemSize());
        int index = 0;
        for (Map.Entry<String, LinkerGroup> entry : items.entrySet()) {
            for (ItemStack stack : entry.getValue().getItems()) {
                inv.setItem(index, stack);
                index++;
            }
        }

        if (level != null) {
            Containers.dropContents(level, worldPosition, inv);
        }
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

    public void toggleLinkedLever(ItemStack stack, String group) {
        if (level == null || level.isClientSide) return;

        ChunkHandler.tempLoadChunk(((ServerLevel) level), stack, (loadedLevel) -> {
            if (loadedLevel == null || loadedLevel.isClientSide) return;

            BlockPos leverPos = stack.get(ModDataComponents.COORDINATES);
            if (leverPos == null) return;

            BlockState state = loadedLevel.getBlockState(leverPos);
            if (!LinkerGroup.canLink(state)) return;

            boolean isLeverPowered = state.getValue(LeverBlock.POWERED);
            loadedLevel.setBlock(leverPos, state.setValue(LeverBlock.POWERED, !isLeverPowered), Block.UPDATE_ALL);
            playSound(SoundEvents.LEVER_CLICK, 0.3f, !isLeverPowered ? 0.5F : 0.6F);

            LeverStateCache.update(leverPos, true, !isLeverPowered);
            updateGroupPoweredState(group);
        });
    }

    public void toggleAllLinkedLever(String groupName) {
        if (level == null || level.isClientSide) return;
        if (!items.containsKey(groupName)) return;
        if (items.get(groupName).getItems().isEmpty()) return;

        boolean allOn = items.get(groupName).isPowered();

        ItemStack item = items.get(groupName).getItems().stream().filter(stack -> !stack.isEmpty() && stack.has(ModDataComponents.COORDINATES)).findFirst().orElse(ItemStack.EMPTY);

        ChunkHandler.tempLoadChunk(((ServerLevel) level), item, (loadedLevel) -> {
            for (ItemStack stack : items.get(groupName).getItems()) {
                if (!(stack.getItem() instanceof RedstoneLinkerItem)) continue;

                BlockPos leverPos = stack.get(ModDataComponents.COORDINATES);

                if (leverPos == null) continue;

                BlockState state = loadedLevel.getBlockState(leverPos);
                if (!LinkerGroup.canLink(state)) continue;

                LeverStateCache.update(leverPos, true, !allOn);
                loadedLevel.setBlock(leverPos, state.setValue(LeverBlock.POWERED, !allOn), Block.UPDATE_ALL);
            }

            items.get(groupName).setPowered(!allOn);
            playSound(SoundEvents.LEVER_CLICK, 0.3f, !allOn ? 0.5F : 0.6F);
            updateGroupPoweredState(groupName);
        });
    }

    public List<String> findAllGroupsForLever(ItemStack item) {
        List<String> groups = new ArrayList<>();
        for (LinkerGroup group : items.values()) {
            int index = group.findLinkerIndex(item);
            if (index == -1) continue;
            groups.add(group.getGroupName());
        }

        return groups;
    }

    public void playSound(SoundEvent soundEvent, float volume, float pitch) {
        if (level == null || level.isClientSide) return;
        level.playSound(null, getBlockPos(), soundEvent, SoundSource.BLOCKS, volume, pitch);
    }
}
