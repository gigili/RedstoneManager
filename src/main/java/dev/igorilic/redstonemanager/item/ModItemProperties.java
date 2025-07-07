package dev.igorilic.redstonemanager.item;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.component.ModDataComponents;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

public class ModItemProperties {
    public static void addCustomItemProperties() {
        ItemProperties.register(
                ModItems.RM_LINKER.get(),
                ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "linked"),
                (itemStack, clientLevel, livingEntity, i) -> itemStack.get(ModDataComponents.COORDINATES) != null ? 1f : 0f
        );
    }
}
