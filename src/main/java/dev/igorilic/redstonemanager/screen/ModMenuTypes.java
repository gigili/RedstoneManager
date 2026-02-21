package dev.igorilic.redstonemanager.screen;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.item.custom.pouch.ui.PouchMenu;
import dev.igorilic.redstonemanager.screen.custom.ManagerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, RedstoneManager.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ManagerMenu>> MANAGER_MENU = registerMenuType("manager_menu", ManagerMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<PouchMenu>> POUCH_MENU =
            MENUS.register("pouch_menu", () -> new MenuType<>(
                    (IContainerFactory<PouchMenu>) (windowId, inv, data) ->
                            new PouchMenu(windowId, inv, inv.player.getItemInHand(InteractionHand.MAIN_HAND)),
                    FeatureFlags.DEFAULT_FLAGS
            ));

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

}
