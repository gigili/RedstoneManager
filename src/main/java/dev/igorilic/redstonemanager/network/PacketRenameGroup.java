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

public record PacketRenameGroup(BlockPos managerPos, String oldGroupName,
                                String newGroupName) implements CustomPacketPayload {
    public static final Type<PacketRenameGroup> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "rename_group"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRenameGroup> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PacketRenameGroup::managerPos,
                    ByteBufCodecs.STRING_UTF8,
                    PacketRenameGroup::oldGroupName,
                    ByteBufCodecs.STRING_UTF8,
                    PacketRenameGroup::newGroupName,
                    PacketRenameGroup::new
            );

    @Override
    public @NotNull Type<PacketRenameGroup> type() {
        return TYPE;
    }

    public static final IPayloadHandler<PacketRenameGroup> HANDLER = (payload, context) -> {
        if (context.player() instanceof ServerPlayer player) {
            if (player.level().getBlockEntity(payload.managerPos()) instanceof RedstoneManagerBlockEntity be) {
                be.renameGroup(payload.oldGroupName, payload.newGroupName);
                be.setChanged();
                player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
        }
    };
}
