package dev.igorilic.redstonemanager.network;

import dev.igorilic.redstonemanager.RedstoneManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketHandler {
    public static void register(final IEventBus modEventBus) {
        modEventBus.addListener(PacketHandler::onRegisterPayloadHandlers);
    }

    private static void onRegisterPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(RedstoneManager.MOD_ID).versioned("1.0");

        // Register all packets
        registrar.playToServer(
                PacketToggleLever.TYPE,
                PacketToggleLever.STREAM_CODEC,
                PacketToggleLever.HANDLER
        );

        registrar.playToServer(
                PacketCreateGroup.TYPE,
                PacketCreateGroup.STREAM_CODEC,
                PacketCreateGroup.HANDLER
        );

        registrar.playToServer(
                PacketToggleAllLevers.TYPE,
                PacketToggleAllLevers.STREAM_CODEC,
                PacketToggleAllLevers.HANDLER
        );

        registrar.playToServer(
                PacketRenameGroup.TYPE,
                PacketRenameGroup.STREAM_CODEC,
                PacketRenameGroup.HANDLER
        );

        registrar.playToServer(
                PacketDeleteGroup.TYPE,
                PacketDeleteGroup.STREAM_CODEC,
                PacketDeleteGroup.HANDLER
        );

        registrar.playToServer(
                MenuInteractPacketC2S.TYPE,
                MenuInteractPacketC2S.STREAM_CODEC,
                MenuInteractPacketC2S.HANDLER
        );

        registrar.playToServer(
                PacketPlaySound.TYPE,
                PacketPlaySound.STREAM_CODEC,
                PacketPlaySound.HANDLER
        );
    }
}
