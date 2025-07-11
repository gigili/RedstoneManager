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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RedstoneManagerBlockEntity extends BlockEntity implements MenuProvider {
    private final List<ItemStack> linkers = new ArrayList<>();

    public RedstoneManagerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MANAGER_BE.get(), pos, blockState);
    }

    public void addLinker(ItemStack linker) {
        if (linker.getItem() instanceof RedstoneLinkerItem) {
            linkers.add(linker);
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    public List<ItemStack> getLinkers() {
        return Collections.unmodifiableList(linkers);
    }

    public void removeLinker(int index) {
        if (index >= 0 && index < linkers.size()) {
            linkers.remove(index);
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    public void swapLinker(int index, ItemStack newLinker) {
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
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag linkersTag = new ListTag();
        for (ItemStack linker : linkers) {
            linkersTag.add(linker.save(registries));
        }
        tag.put("Linkers", linkersTag);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        linkers.clear();
        ListTag linkersTag = tag.getList("Linkers", Tag.TAG_COMPOUND);
        for (Tag t : linkersTag) {
            linkers.add(ItemStack.parseOptional(registries, (CompoundTag) t));
        }
    }

    // Update drops method:
    public void drops() {
        SimpleContainer inv = new SimpleContainer(linkers.size());
        for (int i = 0; i < linkers.size(); i++) {
            inv.setItem(i, linkers.get(i));
        }
        Containers.dropContents(level, worldPosition, inv);
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
        /*
        ItemStack linkerItem = inventory.getStackInSlot(0);
        if (linkerItem.isEmpty() || !(linkerItem.getItem() instanceof RedstoneLinkerItem)) return;

        BlockPos leverPos = linkerItem.get(ModDataComponents.COORDINATES);
        if (leverPos == null) return;

        BlockState leverState = level.getBlockState(leverPos);
        if (!leverState.is(Blocks.LEVER)) return;

        boolean isLeverOn = leverState.getValue(LeverBlock.POWERED);
        level.setBlock(leverPos, leverState.setValue(LeverBlock.POWERED, !isLeverOn), Block.UPDATE_ALL);
        level.playSound(null, leverPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, isLeverOn ? 0.5F : 0.6F);*/
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
