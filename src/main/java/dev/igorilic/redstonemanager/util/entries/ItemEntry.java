package dev.igorilic.redstonemanager.util.entries;

import net.minecraft.world.item.ItemStack;

public record ItemEntry(ItemStack stack, String group) implements DisplayEntry {
}
