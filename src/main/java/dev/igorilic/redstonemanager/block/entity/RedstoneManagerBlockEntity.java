package dev.igorilic.redstonemanager.block.entity;

import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.item.custom.RedstoneLinkerItem;
import dev.igorilic.redstonemanager.screen.custom.ManagerMenu;
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
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RedstoneManagerBlockEntity extends BlockEntity implements MenuProvider {
    public final ContainerData data;

    private final List<Channel> channels = new ArrayList<>();
    private int selectedChannel = 0;

    public int getSelectedChannel() {
        return selectedChannel;
    }

    public List<Channel> getChannels() {
        return channels;
    }

    public record Channel(String name, List<ItemStack> linkers) {
        public Channel {
            linkers = List.copyOf(linkers); // Make immutable
        }

        public Channel(String name) {
            this(name, new ArrayList<>());
        }
    }

    // Channel management methods
    public void addChannel(String name) {
        channels.add(new Channel(name));
        setChanged();
    }

    public void selectChannel(int index) {
        if (index >= 0 && index < channels.size()) {
            selectedChannel = index;
            setChanged();
        }
    }

    // Add this method to get linkers from current channel
    public List<ItemStack> getCurrentChannelLinkers() {
        if (channels.isEmpty()) return Collections.emptyList();
        return new ArrayList<>(channels.get(selectedChannel).linkers());
    }

    // Updated method to add linker to current channel
    public void addLinkerToCurrentChannel(ItemStack linker) {
        if (channels.isEmpty()) return;

        Channel current = channels.get(selectedChannel);
        List<ItemStack> newLinkers = new ArrayList<>(current.linkers());
        newLinkers.add(linker.copy());
        channels.set(selectedChannel, new Channel(current.name(), newLinkers));
        setChanged();
    }

    public void toggleAllLeversInChannel(int index){
        toggleCurrentChannelLevers();
    }

    // Method to toggle all levers in current channel
    public void toggleCurrentChannelLevers() {
        if (level == null || level.isClientSide || channels.isEmpty()) return;

        Channel channel = channels.get(selectedChannel);
        for (ItemStack linker : channel.linkers()) {
            BlockPos leverPos = linker.get(ModDataComponents.COORDINATES);
            if (leverPos != null) {
                BlockState state = level.getBlockState(leverPos);
                if (state.is(Blocks.LEVER)) {
                    boolean isPowered = state.getValue(LeverBlock.POWERED);
                    level.setBlock(leverPos, state.setValue(LeverBlock.POWERED, !isPowered), Block.UPDATE_ALL);
                    level.playSound(null, leverPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, isPowered ? 0.5F : 0.6F);
                }
            }
        }
    }

    public RedstoneManagerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MANAGER_BE.get(), pos, blockState);

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return 0;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
    }

    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    public void clearContents() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
    }

    public void drops() {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, inv);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));

        ListTag channelsTag = new ListTag();
        for (Channel channel : channels) {
            CompoundTag channelTag = new CompoundTag();
            channelTag.putString("Name", channel.name());

            ListTag linkersTag = new ListTag();
            for (ItemStack linker : channel.linkers()) {
                linkersTag.add(linker.save(registries));
            }
            channelTag.put("Linkers", linkersTag);

            channelsTag.add(channelTag);
        }
        tag.put("Channels", channelsTag);
        tag.putInt("SelectedChannel", selectedChannel);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));

        channels.clear();
        ListTag channelsTag = tag.getList("Channels", Tag.TAG_COMPOUND);
        for (Tag t : channelsTag) {
            CompoundTag channelTag = (CompoundTag)t;
            String name = channelTag.getString("Name");

            ListTag linkersTag = channelTag.getList("Linkers", Tag.TAG_COMPOUND);
            List<ItemStack> linkers = new ArrayList<>();
            for (Tag linkerTag : linkersTag) {
                linkers.add(ItemStack.parseOptional(registries, (CompoundTag)linkerTag));
            }

            channels.add(new Channel(name, linkers));
        }
        selectedChannel = tag.getInt("SelectedChannel");
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

    public void toggleLinkedLever() {
        if (level == null || level.isClientSide) return;

        ItemStack linkerItem = inventory.getStackInSlot(0);
        if (linkerItem.isEmpty() || !(linkerItem.getItem() instanceof RedstoneLinkerItem)) return;

        BlockPos leverPos = linkerItem.get(ModDataComponents.COORDINATES);
        if (leverPos == null) return;

        BlockState leverState = level.getBlockState(leverPos);
        if (!leverState.is(Blocks.LEVER)) return;

        boolean isLeverOn = leverState.getValue(LeverBlock.POWERED);
        level.setBlock(leverPos, leverState.setValue(LeverBlock.POWERED, !isLeverOn), Block.UPDATE_ALL);
        level.playSound(null, leverPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, isLeverOn ? 0.5F : 0.6F);
    }
}
