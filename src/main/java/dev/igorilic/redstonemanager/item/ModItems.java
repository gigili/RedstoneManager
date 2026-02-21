package dev.igorilic.redstonemanager.item;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.item.custom.RedstoneLinkerItem;
import dev.igorilic.redstonemanager.item.custom.pouch.PouchItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RedstoneManager.MOD_ID);

    public static final DeferredItem<Item> RM_LINKER = ITEMS.register("rm_linker",
            () -> new RedstoneLinkerItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> RM_POUCH = ITEMS.register("pouch",
            () -> new PouchItem(new Item.Properties().stacksTo(1).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
