package dev.igorilic.redstonemanager.network;

import dev.igorilic.redstonemanager.RedstoneManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
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
                new DirectionalPayloadHandler<>(PacketToggleLever::handle, PacketToggleLever::handle)
        );

        registrar.playToServer(
                PacketAddChannel.TYPE,
                PacketAddChannel.STREAM_CODEC,
                new DirectionalPayloadHandler<>(PacketAddChannel::handle, PacketAddChannel::handle)
        );

        registrar.playToServer(
                PacketSelectChannel.TYPE,
                PacketSelectChannel.STREAM_CODEC,
                new DirectionalPayloadHandler<>(PacketSelectChannel::handle, PacketSelectChannel::handle)
        );

        registrar.playToServer(
                PacketAddLinkerToChannel.TYPE,
                PacketAddLinkerToChannel.STREAM_CODEC,
                new DirectionalPayloadHandler<>(PacketAddLinkerToChannel::handle, PacketAddLinkerToChannel::handle)
        );

        registrar.playToServer(
                PacketToggleChannelLevers.TYPE,
                PacketToggleChannelLevers.STREAM_CODEC,
                new DirectionalPayloadHandler<>(PacketToggleChannelLevers::handle, PacketToggleChannelLevers::handle)
        );
    }
}
