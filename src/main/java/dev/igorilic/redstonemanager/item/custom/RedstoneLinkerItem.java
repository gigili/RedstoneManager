package dev.igorilic.redstonemanager.item.custom;

import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.util.LinkerGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

        if (!level.isClientSide) {
            if (context.getPlayer() != null) {
                if (context.getPlayer().isCrouching()) {
                    if (LinkerGroup.canLink(level.getBlockState(context.getClickedPos()))) {
                        //RMLogger.info("Redstone linker linked to: %s", context.getClickedPos());
                        context.getItemInHand().set(ModDataComponents.COORDINATES, context.getClickedPos());
                        context.getItemInHand().set(ModDataComponents.DIMENSION, level.dimension().location());
                        level.playSound(null, context.getClickedPos(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    context.getPlayer().sendSystemMessage(Component.translatable("message.redstonemanager.connection_cleared"));
                    context.getItemInHand().set(ModDataComponents.COORDINATES, null);
                    context.getItemInHand().set(ModDataComponents.DIMENSION, null);
                }
            }

            context.getPlayer().getInventory().setChanged();
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, @NotNull TooltipContext context, List<Component> tooltipComponent, @NotNull TooltipFlag tooltipFlag) {
        tooltipComponent.add(Component.translatable("tooltip.redstonemanager.rm_linker.right_click"));
        tooltipComponent.add(Component.translatable("tooltip.redstonemanager.rm_linker.shift_right_click"));
        if (itemStack.get(ModDataComponents.COORDINATES) != null) {
            BlockPos blockPos = itemStack.get(ModDataComponents.COORDINATES);
            assert blockPos != null;
            tooltipComponent.add(Component.translatable("tooltip.redstonemanager.rm_linker.linked_to", blockPos.toShortString()));
        }

        if (itemStack.get(ModDataComponents.DIMENSION) != null) {
            ResourceLocation dimension = itemStack.get(ModDataComponents.DIMENSION);
            assert dimension != null;
            tooltipComponent.add(Component.translatable("tooltip.redstonemanager.rm_linker.linked_in", dimension.toString()));
        }
        super.appendHoverText(itemStack, context, tooltipComponent, tooltipFlag);
    }
}
