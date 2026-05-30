package dev.igorilic.redstonemanager.network;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.util.LinkerGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public record PacketLeverStateRequest(BlockPos pos, Optional<Identifier> dim) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketLeverStateRequest> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(RedstoneManager.MOD_ID, "lever_state_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketLeverStateRequest> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PacketLeverStateRequest::pos,
                    ByteBufCodecs.optional(Identifier.STREAM_CODEC),
                    PacketLeverStateRequest::dim,
                    PacketLeverStateRequest::new
            );

    @Override
    public @NotNull CustomPacketPayload.Type<PacketLeverStateRequest> type() {
        return TYPE;
    }

    public static final IPayloadHandler<PacketLeverStateRequest> HANDLER = (payload, context) -> {
        if (context.player() instanceof ServerPlayer player) {
            ServerLevel current = player.level();

            ServerLevel target = current;
            if (payload.dim.isPresent()) {
                target = Objects.equals(current.dimension().identifier(), payload.dim.get())
                        ? current
                        : resolveLevel(current, payload.dim.get());
            }

            BlockState state = target.getBlockState(payload.pos);
            boolean found = LinkerGroup.canLink(state);
            boolean powered = found && state.getValue(LeverBlock.POWERED);
            PacketHandler.sendToClient(player, new PacketLeverStateResponse(payload.pos, found, powered));
        }
    };

    private static ServerLevel resolveLevel(ServerLevel level, Identifier dimId) {
        if (!(level instanceof ServerLevel sl)) return null;
        MinecraftServer srv = sl.getServer();
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimId);
        return srv.getLevel(key);
    }
}
