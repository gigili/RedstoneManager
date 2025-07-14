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

public record PacketCreateGroup(BlockPos managerPos, String groupName) implements CustomPacketPayload {
    public static final Type<PacketCreateGroup> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "create_group"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketCreateGroup> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PacketCreateGroup::managerPos,
                    ByteBufCodecs.STRING_UTF8,
                    PacketCreateGroup::groupName,
                    PacketCreateGroup::new
            );

    @Override
    public @NotNull Type<PacketCreateGroup> type() {
        return TYPE;
    }

    public static final IPayloadHandler<PacketCreateGroup> HANDLER = (payload, context) -> {
        if (context.player() instanceof ServerPlayer player) {
            if (player.level().getBlockEntity(payload.managerPos()) instanceof RedstoneManagerBlockEntity be) {
                be.createGroup(payload.groupName);
                be.setChanged();
                player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
        }
    };
}
