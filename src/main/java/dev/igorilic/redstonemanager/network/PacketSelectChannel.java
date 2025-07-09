package dev.igorilic.redstonemanager.network;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.block.entity.RedstoneManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketSelectChannel(BlockPos managerPos, int index) implements CustomPacketPayload {
    public static final Type<PacketSelectChannel> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "select_channel"));

    public static final StreamCodec<FriendlyByteBuf, PacketSelectChannel> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PacketSelectChannel::managerPos,
                    ByteBufCodecs.VAR_INT,
                    PacketSelectChannel::index,
                    PacketSelectChannel::new
            );

    @Override
    public Type<PacketSelectChannel> type() {
        return TYPE;
    }

    public static void handle(PacketSelectChannel payload, IPayloadContext context) {
        if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
            if (player.level().getBlockEntity(payload.managerPos()) instanceof RedstoneManagerBlockEntity be) {
                be.selectChannel(payload.index());
                be.setChanged();
                player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
        }
    }
}
