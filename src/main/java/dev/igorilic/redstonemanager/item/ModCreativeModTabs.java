package dev.igorilic.redstonemanager.item;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RedstoneManager.MOD_ID);

    public static final Supplier<CreativeModeTab> RM_ITEMS_TAB = CREATIVE_MODE_TAB.register("rm_items_tab", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.RM_LINKER.get()))
            .title(Component.translatable("creativetab.redstonemanager.rm_items_tab"))
            .displayItems(((itemDisplayParameters, output) -> {
                output.accept(ModItems.RM_LINKER);
            }))
            .build());

    public static final Supplier<CreativeModeTab> RM_BLOCK_TAB = CREATIVE_MODE_TAB.register("rm_blocks_tab", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModBlocks.RM_MANAGER_BLOCK.get()))
            .withTabsBefore(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "rm_items_tab"))
            .title(Component.translatable("creativetab.redstonemanager.rm_blocks_tab"))
            .displayItems(((itemDisplayParameters, output) -> {
                output.accept(ModBlocks.RM_MANAGER_BLOCK);
            }))
            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
