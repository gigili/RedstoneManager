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

public record PacketToggleChannelLevers(BlockPos managerPos, int channelIndex) implements CustomPacketPayload {
    public static final Type<PacketToggleChannelLevers> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID,"toggle_channel_levers"));

    public static final StreamCodec<FriendlyByteBuf, PacketToggleChannelLevers> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PacketToggleChannelLevers::managerPos,
                    ByteBufCodecs.VAR_INT,
                    PacketToggleChannelLevers::channelIndex,
                    PacketToggleChannelLevers::new
            );

    @Override
    public Type<PacketToggleChannelLevers> type() {
        return TYPE;
    }

    public static void handle(PacketToggleChannelLevers payload, IPayloadContext context) {
        if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
            if (player.level().getBlockEntity(payload.managerPos()) instanceof RedstoneManagerBlockEntity be) {
                //be.toggleAllLeversInChannel(payload.channelIndex());
                be.setChanged();
                player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
        }
    }
}
