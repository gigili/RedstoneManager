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

public record PacketAddChannel(BlockPos managerPos, String name) implements CustomPacketPayload {
    public static final Type<PacketAddChannel> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "add_channel"));

    public static final StreamCodec<FriendlyByteBuf, PacketAddChannel> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PacketAddChannel::managerPos,
                    ByteBufCodecs.STRING_UTF8,
                    PacketAddChannel::name,
                    PacketAddChannel::new
            );

    @Override
    public Type<PacketAddChannel> type() {
        return TYPE;
    }

    public static void handle(PacketAddChannel payload, IPayloadContext context) {
        if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
            if (player.level().getBlockEntity(payload.managerPos()) instanceof RedstoneManagerBlockEntity be) {
                be.addChannel(payload.name());
                be.setChanged();
                player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
        }
    }
}
