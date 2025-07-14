package dev.igorilic.redstonemanager.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public interface Interact {
    void clicked(ItemStack itemStack, CompoundTag extraData, ClickType clickType, int button, String groupName);
}
