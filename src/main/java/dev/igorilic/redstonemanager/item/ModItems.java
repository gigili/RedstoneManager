package dev.igorilic.redstonemanager.item;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.item.custom.RedstoneLinkerItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RedstoneManager.MOD_ID);

    public static final DeferredItem<Item> RM_LINKER = ITEMS.register("rm_linker",
            () -> new RedstoneLinkerItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
