package dev.igorilic.redstonemanager.item.custom;

import dev.igorilic.redstonemanager.block.custom.RedstoneManagerBlock;
import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.network.PacketOpenMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RedstoneRemoteItem extends Item {
    public RedstoneRemoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Block clicked = level.getBlockState(context.getClickedPos()).getBlock();

        if (!level.isClientSide) {
            if (context.getPlayer() != null) {
                if (context.getPlayer().isCrouching()) {
                    if (clicked instanceof RedstoneManagerBlock) {
                        context.getItemInHand().set(ModDataComponents.COORDINATES, context.getClickedPos());
                        level.playSound(null, context.getClickedPos(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    context.getPlayer().sendSystemMessage(Component.translatable("message.redstonemanager.connection_cleared"));
                    context.getItemInHand().set(ModDataComponents.COORDINATES, null);
                }
            }

            context.getPlayer().getInventory().setChanged();
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!stack.has(ModDataComponents.COORDINATES)) return InteractionResultHolder.fail(stack);

        BlockPos pos = stack.get(ModDataComponents.COORDINATES);
        if (pos == null) return InteractionResultHolder.fail(stack);

        // client-side only: send open GUI request
        if (level.isClientSide && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.connection.send(new PacketOpenMenu(pos));
        }

        return InteractionResultHolder.success(stack);
    }


    private boolean isChunkLoadedClientSide(BlockPos pos) {
        if (Minecraft.getInstance().level == null) return false;
        ChunkPos chunkPos = new ChunkPos(pos);
        return Minecraft.getInstance().level.hasChunk(chunkPos.x, chunkPos.z);
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
        super.appendHoverText(itemStack, context, tooltipComponent, tooltipFlag);
    }
}
