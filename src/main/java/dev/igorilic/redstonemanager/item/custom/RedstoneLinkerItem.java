package dev.igorilic.redstonemanager.item.custom;

import dev.igorilic.redstonemanager.block.ModBlocks;
import dev.igorilic.redstonemanager.block.entity.RedstoneManagerBlockEntity;
import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.util.LinkerGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class RedstoneLinkerItem extends Item {
    public RedstoneLinkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide && entity instanceof Player && !stack.has(ModDataComponents.ITEM_UUID)) {
            stack.set(ModDataComponents.ITEM_UUID, UUID.randomUUID().toString());
        }
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        if (!stack.has(ModDataComponents.ITEM_UUID)) {
            stack.set(ModDataComponents.ITEM_UUID, UUID.randomUUID().toString());
        }
        return super.getDefaultInstance();
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.FAIL;

        if (state.is(ModBlocks.RM_MANAGER_BLOCK.get())) {
            if (player.isCrouching()) {
                if (!level.isClientSide) {
                    if (level.getBlockEntity(pos) instanceof RedstoneManagerBlockEntity blockEntity) {
                        blockEntity.handleBulkLink(stack, (ServerPlayer) player);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            } else {
                return InteractionResult.PASS;
            }
        }

        if (!level.isClientSide) {
            if (player.isCrouching()) {
                if (LinkerGroup.canLink(state)) {
                    stack.set(ModDataComponents.COORDINATES, pos);
                    stack.set(ModDataComponents.DIMENSION, level.dimension().location());
                    stack.set(ModDataComponents.COORDINATES_START, null);
                    stack.set(ModDataComponents.COORDINATES_END, null);
                    level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
                } else {
                    player.sendSystemMessage(Component.translatable("message.redstonemanager.connection_cleared"));
                    stack.set(ModDataComponents.COORDINATES, null);
                    stack.set(ModDataComponents.DIMENSION, null);
                    stack.set(ModDataComponents.COORDINATES_START, null);
                    stack.set(ModDataComponents.COORDINATES_END, null);
                }
            } else {
                if (stack.get(ModDataComponents.COORDINATES_START) == null) {
                    stack.set(ModDataComponents.COORDINATES_START, pos);
                    stack.set(ModDataComponents.DIMENSION, level.dimension().location());
                    stack.set(ModDataComponents.COORDINATES, null); // Clear direct link
                    player.sendSystemMessage(Component.translatable("message.redstonemanager.area_start_set", pos.toShortString()));
                } else if (stack.get(ModDataComponents.COORDINATES_END) == null) {
                    stack.set(ModDataComponents.COORDINATES_END, pos);
                    player.sendSystemMessage(Component.translatable("message.redstonemanager.area_end_set", pos.toShortString()));
                } else {
                    stack.set(ModDataComponents.COORDINATES_START, null);
                    stack.set(ModDataComponents.COORDINATES_END, null);
                    stack.set(ModDataComponents.DIMENSION, null);
                    player.sendSystemMessage(Component.translatable("message.redstonemanager.area_cleared"));
                }
            }

            player.getInventory().setChanged();
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, @NotNull TooltipContext context, List<Component> tooltipComponent, @NotNull TooltipFlag tooltipFlag) {
        tooltipComponent.add(Component.translatable("tooltip.redstonemanager.rm_linker.right_click"));
        tooltipComponent.add(Component.translatable("tooltip.redstonemanager.rm_linker.shift_right_click"));
        tooltipComponent.add(Component.translatable("tooltip.redstonemanager.rm_linker.area_select"));

        if (itemStack.get(ModDataComponents.COORDINATES) != null) {
            BlockPos blockPos = itemStack.get(ModDataComponents.COORDINATES);
            assert blockPos != null;
            tooltipComponent.add(Component.translatable("tooltip.redstonemanager.rm_linker.linked_to", blockPos.toShortString()));
        }

        if (itemStack.get(ModDataComponents.COORDINATES_START) != null) {
            BlockPos blockPos = itemStack.get(ModDataComponents.COORDINATES_START);
            assert blockPos != null;
            tooltipComponent.add(Component.translatable("tooltip.redstonemanager.rm_linker.area_start", blockPos.toShortString()));
        }

        if (itemStack.get(ModDataComponents.COORDINATES_END) != null) {
            BlockPos blockPos = itemStack.get(ModDataComponents.COORDINATES_END);
            assert blockPos != null;
            tooltipComponent.add(Component.translatable("tooltip.redstonemanager.rm_linker.area_end", blockPos.toShortString()));
        }

        if (itemStack.get(ModDataComponents.DIMENSION) != null) {
            ResourceLocation dimension = itemStack.get(ModDataComponents.DIMENSION);
            assert dimension != null;
            tooltipComponent.add(Component.translatable("tooltip.redstonemanager.rm_linker.linked_in", dimension.toString()));
        }
        super.appendHoverText(itemStack, context, tooltipComponent, tooltipFlag);
    }
}
