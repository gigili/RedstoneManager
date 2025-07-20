package dev.igorilic.redstonemanager.network;

import dev.igorilic.redstonemanager.RedstoneManager;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Objects;

public class PacketHandler {
    private static PayloadRegistrar registrar;

    public static void register(final IEventBus modEventBus) {
        modEventBus.addListener(PacketHandler::onRegisterPayloadHandlers);
    }

    private static void onRegisterPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        registrar = event.registrar(RedstoneManager.MOD_ID).versioned("1.0");

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
                PacketMoveItemBetweenInventories.TYPE,
                PacketMoveItemBetweenInventories.STREAM_CODEC,
                PacketMoveItemBetweenInventories.HANDLER
        );

        registrar.playToServer(
                PacketPlaySound.TYPE,
                PacketPlaySound.STREAM_CODEC,
                PacketPlaySound.HANDLER
        );

        registrar.playToServer(
                PacketLeverStateRequest.TYPE,
                PacketLeverStateRequest.STREAM_CODEC,
                PacketLeverStateRequest.HANDLER
        );

        registrar.playToServer(
                PacketRefreshGroupPoweredState.TYPE,
                PacketRefreshGroupPoweredState.STREAM_CODEC,
                PacketRefreshGroupPoweredState.HANDLER
        );

        registrar.playToClient(
                PacketLeverStateResponse.TYPE,
                PacketLeverStateResponse.STREAM_CODEC,
                PacketLeverStateResponse.HANDLER
        );
    }

    public static void sendToClient(ServerPlayer player, PacketLeverStateResponse packet) {
        if (registrar != null) {
            player.connection.send(packet);
        }
    }

    public static void sendToServer(PacketLeverStateRequest packet) {
        Objects.requireNonNull(Minecraft.getInstance().getConnection()).send(packet);
    }
}
