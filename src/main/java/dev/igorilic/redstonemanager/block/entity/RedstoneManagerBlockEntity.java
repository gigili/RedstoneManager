package dev.igorilic.redstonemanager.block.entity;

import dev.igorilic.redstonemanager.Config;
import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.item.custom.RedstoneLinkerItem;
import dev.igorilic.redstonemanager.item.custom.pouch.PouchItem;
import dev.igorilic.redstonemanager.network.PacketHandler;
import dev.igorilic.redstonemanager.network.PacketLeverStateResponse;
import dev.igorilic.redstonemanager.screen.custom.ManagerMenu;
import dev.igorilic.redstonemanager.util.LinkerGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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

    private ServerLevel resolveLevel(Identifier dimId) {
        if (!(level instanceof ServerLevel sl)) return null;
        MinecraftServer srv = sl.getServer();
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimId);
        return srv.getLevel(key);
    }

    private void updateGroupPoweredState(String groupName, Boolean changed) {
        if (!(level instanceof ServerLevel current) || !items.containsKey(groupName)) return;

        boolean isOn = false;
        for (ItemStack stack : items.get(groupName).getItems()) {
            if (!(stack.getItem() instanceof RedstoneLinkerItem)) continue;

            BlockPos leverPos = stack.get(ModDataComponents.COORDINATES);
            Identifier dimension = stack.get(ModDataComponents.DIMENSION);

            ServerLevel target = Objects.equals(current.dimension().identifier(), dimension)
                    ? current
                    : resolveLevel(dimension);

            if (target == null) continue;

            if (leverPos == null) continue;

            BlockState state = target.getBlockState(leverPos);
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
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        var groupsList = output.childrenList("Groups");

        for (Map.Entry<String, LinkerGroup> entry : items.entrySet()) {
            var groupOutput = groupsList.addChild();
            groupOutput.putString("Name", entry.getKey());
            groupOutput.putBoolean("IsPowered", entry.getValue().isPowered());

            var itemsList = groupOutput.list("Links", ItemStack.CODEC);
            for (ItemStack stack : entry.getValue().getItems()) {
                if (stack == ItemStack.EMPTY) continue;
                itemsList.add(stack);
            }
        }
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        this.items.clear();

        var groupsList = input.childrenListOrEmpty("Groups");
        for (ValueInput groupInput : groupsList) {
            String name = groupInput.getStringOr("Name", "");
            boolean isPowered = groupInput.getBooleanOr("IsPowered", false);

            List<ItemStack> stacks = new ArrayList<>();
            var itemStacks = groupInput.listOrEmpty("Links", ItemStack.CODEC);
            for (ItemStack stack : itemStacks) {
                stacks.add(stack);
            }

            this.items.put(name, new LinkerGroup(name, isPowered, stacks));
        }

        setChanged();
    }

    @Override
    public void preRemoveSideEffects(@NotNull BlockPos pos, @NotNull BlockState state) {
        if (level != null && !level.isClientSide()) {
            drops();
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        super.preRemoveSideEffects(pos, state);
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
    public @Nullable AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        return new ManagerMenu(i, inventory, this);
    }

    public void toggleLinkedLever(ItemStack stack, String group, ServerPlayer player) {
        if (!(level instanceof ServerLevel current)) return;

        Identifier dimension = stack.get(ModDataComponents.DIMENSION);
        ServerLevel target = Objects.equals(current.dimension().identifier(), dimension)
                ? current
                : resolveLevel(dimension);

        if (target == null) return;

        BlockPos leverPos = stack.get(ModDataComponents.COORDINATES);
        if (leverPos == null) return;

        BlockState state = target.getBlockState(leverPos);
        if (!LinkerGroup.canLink(state)) return;

        boolean isPowered = state.getValue(LeverBlock.POWERED);
        flipLeverVanilla(target, leverPos);

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

            Identifier dimension = stack.get(ModDataComponents.DIMENSION);
            ServerLevel targetDimension = Objects.equals(level.dimension().identifier(), dimension)
                    ? sl
                    : resolveLevel(dimension);

            if (targetDimension == null) return;

            BlockState st = targetDimension.getBlockState(pos);
            if (!LinkerGroup.canLink(st)) continue;

            if (st.getValue(LeverBlock.POWERED) != target) {
                flipLeverVanilla(targetDimension, pos);
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
        if (level == null || level.isClientSide()) return;
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

        // neighbor notifications at lever levelPosition
        level.updateNeighborsAt(pos, toggled.getBlock());
        level.updateNeighbourForOutputSignal(pos, toggled.getBlock());

        // neighbor notifications at the block it’s attached to (power goes out that way)
        Direction out = connectedDir(toggled).getOpposite();
        BlockPos attached = pos.relative(out);
        level.updateNeighborsAt(attached, toggled.getBlock());
        level.updateNeighbourForOutputSignal(attached, toggled.getBlock());

        // ensure redstone re-evaluates shapes (some dust layouts need this)
        level.updateNeighborsAt(pos, toggled.getBlock());

        // game event (optional but matches vanilla)
        level.gameEvent(null, toggled.getValue(LeverBlock.POWERED) ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pos);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag tag = new CompoundTag();
        var groupsList = new net.minecraft.nbt.ListTag();

        for (Map.Entry<String, LinkerGroup> entry : items.entrySet()) {
            CompoundTag groupTag = new CompoundTag();
            groupTag.putString("Name", entry.getKey());
            groupTag.putBoolean("IsPowered", entry.getValue().isPowered());

            var itemsList = new net.minecraft.nbt.ListTag();
            for (ItemStack stack : entry.getValue().getItems()) {
                if (stack == ItemStack.EMPTY) continue;
                ItemStack.CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack)
                        .ifSuccess(tagElement -> itemsList.add(tagElement));
            }

            groupTag.put("Links", itemsList);
            groupsList.add(groupTag);
        }

        tag.put("Groups", groupsList);
        return tag;
    }

    @Override
    public void handleUpdateTag(@NotNull ValueInput input) {
        loadAdditional(input);
    }

    public boolean handleBulkLink(ItemStack linkerStack, ServerPlayer player) {
        BlockPos start = linkerStack.get(ModDataComponents.COORDINATES_START);
        BlockPos end = linkerStack.get(ModDataComponents.COORDINATES_END);
        Identifier dim = linkerStack.get(ModDataComponents.DIMENSION);

        if (start != null && end != null && dim != null) {
            return bulkAddByRange(start, end, dim, player);
        } else {
            BlockPos linked = linkerStack.get(ModDataComponents.COORDINATES);
            if (linked != null && dim != null) {
                bulkAddBySimilarity(linked, dim, player);
                return false;
            }
        }
        return false;
    }

    private boolean bulkAddByRange(BlockPos start, BlockPos end, Identifier dim, ServerPlayer player) {
        ServerLevel targetLevel = resolveLevel(dim);
        if (targetLevel == null) return false;
        int count = 0;
        String groupName = getFirstOrNewGroup(player);

        Iterable<BlockPos> area = BlockPos.betweenClosed(start, end);
        boolean didRanOut = false;
        boolean shouldConsume = true;

        for (BlockPos pos : area) {
            BlockState state = targetLevel.getBlockState(pos);
            if (!LinkerGroup.canLink(state)) continue;

            if (isAlreadyLinked(pos, dim)) continue;

            ItemStack linker = getOneBlankLinker(player);

            if (linker.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.redstonemanager.out_of_linkers", count));
                didRanOut = true;
                shouldConsume = false;
                break;
            }

            linker.set(ModDataComponents.COORDINATES, pos.immutable());
            linker.set(ModDataComponents.DIMENSION, dim);

            this.items.computeIfAbsent(groupName, k -> new LinkerGroup(groupName)).addItem(linker);
            count++;
        }

        if (!didRanOut) {
            player.sendSystemMessage(Component.translatable("message.redstonemanager.bulk_link_success", count, groupName));
        }
        updateGroupPoweredState(groupName);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
        return shouldConsume;
    }

    private void bulkAddBySimilarity(BlockPos linked, Identifier dim, ServerPlayer player) {
        ServerLevel targetLevel = resolveLevel(dim);
        if (targetLevel == null) return;

        BlockState linkedState = targetLevel.getBlockState(linked);
        int count = 0;
        String groupName = getFirstOrNewGroup(player);

        // Search in a configured radius
        int radius = Config.BULK_SEARCH_RADIUS.get();
        BlockPos min = linked.offset(-radius, -radius, -radius);
        BlockPos max = linked.offset(radius, radius, radius);

        boolean didRanOut = false;

        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            BlockState state = targetLevel.getBlockState(p);
            if (state.getBlock() == linkedState.getBlock() && LinkerGroup.canLink(state)) {
                if (isAlreadyLinked(p, dim)) continue;

                ItemStack linker = getOneBlankLinker(player);

                if (!linker.isEmpty()) {
                    linker.set(ModDataComponents.COORDINATES, p.immutable());
                    linker.set(ModDataComponents.DIMENSION, dim);
                    this.items.computeIfAbsent(groupName, k -> new LinkerGroup(groupName)).addItem(linker);
                    count++;
                } else {
                    player.sendSystemMessage(Component.translatable("message.redstonemanager.out_of_linkers", count));
                    didRanOut = true;
                    break;
                }
            }
        }
        if (!didRanOut) {
            player.sendSystemMessage(Component.translatable("message.redstonemanager.bulk_link_success", count, groupName));
        }
        updateGroupPoweredState(groupName);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    private String getFirstOrNewGroup(ServerPlayer player) {
        if (items.isEmpty()) {
            createGroup("Default", player);
            return "Default";
        }
        return items.keySet().iterator().next();
    }

    private boolean isAlreadyLinked(BlockPos pos, Identifier dim) {
        for (LinkerGroup group : items.values()) {
            for (ItemStack stack : group.getItems()) {
                BlockPos p = stack.get(ModDataComponents.COORDINATES);
                Identifier d = stack.get(ModDataComponents.DIMENSION);
                if (pos.equals(p) && Objects.equals(dim, d)) return true;
            }
        }
        return false;
    }

    private ItemStack getOneBlankLinker(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isItemStackBlankLinker(stack)) {
                ItemStack res = stack.split(1);
                player.getInventory().setChanged();
                return res;
            }
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack pouchStack = player.getInventory().getItem(i);
            if (pouchStack.getItem() instanceof PouchItem) {
                ItemContainerContents contents = pouchStack.get(DataComponents.CONTAINER);
                if (contents != null) {
                    List<ItemStack> stacks = new ArrayList<>();
                    boolean found = false;
                    ItemStack result = ItemStack.EMPTY;

                    for (int j = 0; j < contents.getSlots(); j++) {
                        ItemStack s = contents.getStackInSlot(j).copy();
                        if (!found && isItemStackBlankLinker(s)) {
                            result = s.split(1);
                            found = true;
                        }
                        stacks.add(s);
                    }

                    if (found) {
                        pouchStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
                        player.getInventory().setChanged();
                        return result;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean isItemStackBlankLinker(ItemStack stack) {
        return stack.getItem() instanceof RedstoneLinkerItem &&
                !stack.has(ModDataComponents.COORDINATES) &&
                !stack.has(ModDataComponents.COORDINATES_START) &&
                !stack.has(ModDataComponents.COORDINATES_END) &&
                !stack.has(ModDataComponents.DIMENSION);
    }

}
