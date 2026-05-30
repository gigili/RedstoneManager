package dev.igorilic.redstonemanager.item.custom.pouch.ui;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.jetbrains.annotations.NotNull;

public class PouchSlot extends ResourceHandlerSlot {
    public PouchSlot(ItemStacksResourceHandler handler, int handlerSlot, int xPosition, int yPosition) {
        super(handler, handler::set, handlerSlot, xPosition, yPosition);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        return 64;
    }

    @Override
    public @NotNull ItemStack remove(int amount) {
        return super.remove(1);
    }
}
