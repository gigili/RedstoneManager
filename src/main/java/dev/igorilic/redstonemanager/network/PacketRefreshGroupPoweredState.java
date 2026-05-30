package dev.igorilic.redstonemanager.network;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.block.entity.RedstoneManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record PacketRefreshGroupPoweredState(BlockPos managerPos) implements CustomPacketPayload {
    public static final Type<PacketRefreshGroupPoweredState> TYPE = new Type<>(Identifier.fromNamespaceAndPath(RedstoneManager.MOD_ID, "refresh_group_power_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRefreshGroupPoweredState> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PacketRefreshGroupPoweredState::managerPos,
                    PacketRefreshGroupPoweredState::new
            );

    @Override
    public Type<PacketRefreshGroupPoweredState> type() {
        return TYPE;
    }

    public static final IPayloadHandler<PacketRefreshGroupPoweredState> HANDLER = (payload, context) -> {
        if (context.player() instanceof ServerPlayer player) {
            if (player.level().getBlockEntity(payload.managerPos()) instanceof RedstoneManagerBlockEntity be) {
                be.updateGroupPoweredState();
                player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL);
            }
        }
    };
}
