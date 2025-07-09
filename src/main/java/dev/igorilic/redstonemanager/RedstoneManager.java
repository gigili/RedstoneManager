package dev.igorilic.redstonemanager;

import dev.igorilic.redstonemanager.block.ModBlocks;
import dev.igorilic.redstonemanager.block.entity.ModBlockEntities;
import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.item.ModCreativeModTabs;
import dev.igorilic.redstonemanager.item.ModItemProperties;
import dev.igorilic.redstonemanager.item.ModItems;
import dev.igorilic.redstonemanager.network.PacketHandler;
import dev.igorilic.redstonemanager.screen.ModMenuTypes;
import dev.igorilic.redstonemanager.screen.custom.ManagerScreen;
import dev.igorilic.redstonemanager.util.RMLogger;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(RedstoneManager.MOD_ID)
public class RedstoneManager {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "redstonemanager";

    public RedstoneManager(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register CREATIVE TAB
        ModCreativeModTabs.register(modEventBus);

        // Register ITEMS
        ModItems.register(modEventBus);

        // Register BLOCKS
        ModBlocks.register(modEventBus);

        // Register DataComponent
        ModDataComponents.register(modEventBus);

        ModBlockEntities.register(modEventBus);

        ModMenuTypes.register(modEventBus);

        PacketHandler.register(modEventBus);


        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (RedstoneManager) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        RMLogger.info("HELLO FROM COMMON SETUP");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        RMLogger.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ModItemProperties.addCustomItemProperties();
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.MANAGER_MENU.get(), ManagerScreen::new);
        }
    }
}
