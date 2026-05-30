package dev.igorilic.redstonemanager.item.custom.pouch;

import dev.igorilic.redstonemanager.item.custom.pouch.ui.PouchMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class PouchItem extends Item {
    public PouchItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult use(Level level, Player player, @NotNull InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack itemstack = player.getItemInHand(hand);
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, p) -> new PouchMenu(containerId, playerInventory, itemstack),
                    Component.translatable("gui.redstonemanager.pouch")
            ));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipConsumer, TooltipFlag tooltipFlag) {
        tooltipConsumer.accept(Component.translatable("tooltip.redstonemanager.pouch.store"));
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            int count = 0;
            for (int i = 0; i < contents.getSlots(); i++) {
                if (!contents.getStackInSlot(i).isEmpty()) {
                    count++;
                }
            }
            if (count > 0) {
                tooltipConsumer.accept(Component.translatable("tooltip.redstonemanager.pouch.contains", count));
            }
        }
        super.appendHoverText(stack, context, tooltipDisplay, tooltipConsumer, tooltipFlag);
    }
}
