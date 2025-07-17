package dev.igorilic.redstonemanager.network;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.util.LeverStateCache;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

public record PacketLeverStateResponse(BlockPos pos, boolean found, boolean powered)
        implements CustomPacketPayload {

    public static final Type<PacketLeverStateResponse> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "lever_state_response"));

    public static final StreamCodec<FriendlyByteBuf, PacketLeverStateResponse> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PacketLeverStateResponse::pos,
                    ByteBufCodecs.BOOL,
                    PacketLeverStateResponse::found,
                    ByteBufCodecs.BOOL,
                    PacketLeverStateResponse::powered,
                    PacketLeverStateResponse::new
            );

    @Override
    public @NotNull Type<PacketLeverStateResponse> type() {
        return TYPE;
    }

    public static final IPayloadHandler<PacketLeverStateResponse> HANDLER = (payload, context) -> {
        LeverStateCache.update(payload.pos(), payload.found(), payload.powered());
    };
}
