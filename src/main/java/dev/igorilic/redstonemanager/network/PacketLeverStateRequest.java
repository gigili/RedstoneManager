package dev.igorilic.redstonemanager.network;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.util.ChunkHandler;
import dev.igorilic.redstonemanager.util.LinkerGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

public record PacketLeverStateRequest(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketLeverStateRequest> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "lever_state_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketLeverStateRequest> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PacketLeverStateRequest::pos,
                    PacketLeverStateRequest::new
            );

    @Override
    public @NotNull CustomPacketPayload.Type<PacketLeverStateRequest> type() {
        return TYPE;
    }

    public static final IPayloadHandler<PacketLeverStateRequest> HANDLER = (payload, context) -> {
        if (context.player() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            ChunkHandler.tempLoadChunk(level, payload.pos, serverLevel -> {
                BlockState state = serverLevel.getBlockState(payload.pos);
                boolean found = LinkerGroup.canLink(state);
                boolean powered = found && state.getValue(LeverBlock.POWERED);
                PacketHandler.sendToClient(player, new PacketLeverStateResponse(payload.pos, found, powered));
            });
        }
    };
}
