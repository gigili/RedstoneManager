package dev.igorilic.redstonemanager.network;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.block.entity.RedstoneManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

public record PacketDeleteGroup(BlockPos managerPos, String groupName) implements CustomPacketPayload {
    public static final Type<PacketDeleteGroup> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "delete_group"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketDeleteGroup> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PacketDeleteGroup::managerPos,
                    ByteBufCodecs.STRING_UTF8,
                    PacketDeleteGroup::groupName,
                    PacketDeleteGroup::new
            );

    @Override
    public @NotNull Type<PacketDeleteGroup> type() {
        return TYPE;
    }

    public static final IPayloadHandler<PacketDeleteGroup> HANDLER = (payload, context) -> {
        if (context.player() instanceof ServerPlayer player) {
            if (player.level().getBlockEntity(payload.managerPos()) instanceof RedstoneManagerBlockEntity be) {
                be.deleteGroup(payload.groupName);
                be.setChanged();
                player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
        }
    };
}
