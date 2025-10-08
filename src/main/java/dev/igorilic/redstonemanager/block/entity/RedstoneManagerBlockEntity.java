package dev.igorilic.redstonemanager.block.entity;

import dev.igorilic.redstonemanager.Config;
import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.item.custom.RedstoneLinkerItem;
import dev.igorilic.redstonemanager.network.PacketHandler;
import dev.igorilic.redstonemanager.network.PacketLeverStateResponse;
import dev.igorilic.redstonemanager.screen.custom.ManagerMenu;
import dev.igorilic.redstonemanager.util.IUpdatable;
import dev.igorilic.redstonemanager.util.LinkerGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.gameevent.GameEvent;
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

        if (existingItemIndex == -1) return; // Can't locate an existing item in a group

        items.get(groupName).getItems().set(existingItemIndex, inHand.copy());

        this.setChanged();

        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public Map<String, LinkerGroup> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public void createGroup(String groupName, ServerPlayer player) {
        items.computeIfAbsent(groupName, k -> new LinkerGroup(groupName)).addItem(ItemStack.EMPTY);
        updateGroupPoweredState(groupName);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void addItemToGroup(String groupName, ItemStack item) {
        if (!(item.getItem() instanceof RedstoneLinkerItem)) return;
        this.items.computeIfAbsent(groupName, k -> new LinkerGroup(groupName)).addItem(item);

        updateGroupPoweredState(groupName);

        setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void renameGroup(String oldName, String newName) {
        if (!items.containsKey(oldName)) return;
        if (newName.isEmpty()) return;

        LinkerGroup group = items.get(oldName);
        group.setGroupName(newName);

        items.remove(oldName);
        items.put(newName, group);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
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
        setChanged();
    }

    private void updateGroupPoweredState(String groupName, Boolean changed) {
        if (level == null || level.isClientSide) return;
        if (!items.containsKey(groupName)) return;

        boolean isOn = false;
        for (ItemStack stack : items.get(groupName).getItems()) {
            if (!(stack.getItem() instanceof RedstoneLinkerItem)) continue;

            BlockPos leverPos = stack.get(ModDataComponents.COORDINATES);

            if (leverPos == null) continue;

            BlockState state = level.getBlockState(leverPos);
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
    }

    public void removeItemFromGroup(String groupName, ItemStack item) {
        if (!this.items.containsKey(groupName)) return;
        if (!(item.getItem() instanceof RedstoneLinkerItem)) return;

        items.get(groupName).removeItem(item);

        boolean isEmpty = items.get(groupName).getItems().isEmpty() || items.get(groupName).getItems().stream().allMatch(ItemStack::isEmpty);
        if (isEmpty && Config.DELETE_EMPTY_GROUPS.get()) {
            this.items.remove(groupName);
        } else {
            this.items.get(groupName).addItem(ItemStack.EMPTY);
        }

        setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
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
        setChanged();
        if (level != null && level.isClientSide) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.screen instanceof IUpdatable ui) ui.update(items);
            });
        }
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

        setChanged();
        if (level != null && level.isClientSide) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.screen instanceof IUpdatable ui) ui.update(items);
            });
        }
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
    public void onDataPacket(@NotNull Connection connection, @NotNull ClientboundBlockEntityDataPacket clientboundBlockEntityDataPacket, HolderLookup.@NotNull Provider provider) {
        super.onDataPacket(connection, clientboundBlockEntityDataPacket, provider);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        return new ManagerMenu(i, inventory, this);
    }

    public void toggleLinkedLever(ItemStack stack, String group, ServerPlayer player) {
        if (level == null || level.isClientSide) return;

        if (!(level instanceof ServerLevel sl)) return;
        BlockPos leverPos = stack.get(ModDataComponents.COORDINATES);
        if (leverPos == null) return;

        BlockState state = sl.getBlockState(leverPos);
        if (!LinkerGroup.canLink(state)) return;

        boolean isPowered = state.getValue(LeverBlock.POWERED);
        flipLeverVanilla(sl, leverPos);

        updateGroupPoweredState(group);
        PacketHandler.sendToClient(player, new PacketLeverStateResponse(leverPos, true, !isPowered));
        playSound(SoundEvents.LEVER_CLICK, 0.3f, !isPowered ? 0.6F : 0.5F);
        setChanged();
    }

    public void toggleAllLinkedLever(String groupName, ServerPlayer player) {
        if (!(level instanceof ServerLevel sl)) return;
        if (!items.containsKey(groupName)) return;

        boolean target = !items.get(groupName).isPowered();

        for (ItemStack stack : items.get(groupName).getItems()) {
            if (!(stack.getItem() instanceof RedstoneLinkerItem)) continue;
            BlockPos pos = stack.get(ModDataComponents.COORDINATES);
            if (pos == null) continue;

            BlockState st = sl.getBlockState(pos);
            if (!LinkerGroup.canLink(st)) continue;

            if (st.getValue(LeverBlock.POWERED) != target) {
                flipLeverVanilla(sl, pos);
                PacketHandler.sendToClient(player, new PacketLeverStateResponse(pos, true, target));
            }
        }

        items.get(groupName).setPowered(target);
        playSound(SoundEvents.LEVER_CLICK, 0.3f, target ? 0.6F : 0.5F);
        updateGroupPoweredState(groupName);
        setChanged();
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

    private static Direction connectedDir(BlockState s) {
        AttachFace face = s.getValue(LeverBlock.FACE);
        Direction facing = s.getValue(LeverBlock.FACING);
        return switch (face) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> facing;
        };
    }

    private static void flipLeverVanilla(ServerLevel level, BlockPos pos) {
        BlockState old = level.getBlockState(pos);
        if (!(old.getBlock() instanceof LeverBlock)) return;

        BlockState toggled = old.cycle(LeverBlock.POWERED);

        // set + notify clients (vanilla uses flags 3 = UPDATE_CLIENTS | BLOCK_UPDATE)
        level.setBlock(pos, toggled, Block.UPDATE_ALL);

        // neighbor notifications at lever pos
        level.updateNeighborsAt(pos, toggled.getBlock());
        level.updateNeighbourForOutputSignal(pos, toggled.getBlock());

        // neighbor notifications at the block it’s attached to (power goes out that way)
        Direction out = connectedDir(toggled).getOpposite();
        BlockPos attached = pos.relative(out);
        level.updateNeighborsAt(attached, toggled.getBlock());
        level.updateNeighbourForOutputSignal(attached, toggled.getBlock());

        // ensure redstone re-evaluates shapes (some dust layouts need this)
        level.blockUpdated(pos, toggled.getBlock());

        // game event (optional but matches vanilla)
        level.gameEvent(null, toggled.getValue(LeverBlock.POWERED) ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pos);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        loadAdditional(tag, registries);
    }
}
