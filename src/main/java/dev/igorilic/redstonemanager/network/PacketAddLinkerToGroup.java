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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

public record PacketAddLinkerToGroup(BlockPos managerPos, String groupName,
                                     ItemStack item) implements CustomPacketPayload {
    public static final Type<PacketAddLinkerToGroup> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "add_linker_to_group"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketAddLinkerToGroup> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PacketAddLinkerToGroup::managerPos,
                    ByteBufCodecs.STRING_UTF8,
                    PacketAddLinkerToGroup::groupName,
                    ItemStack.STREAM_CODEC,
                    PacketAddLinkerToGroup::item,
                    PacketAddLinkerToGroup::new
            );

    @Override
    public @NotNull Type<PacketAddLinkerToGroup> type() {
        return TYPE;
    }

    public static final IPayloadHandler<PacketAddLinkerToGroup> HANDLER = (payload, context) -> {
        if (context.player() instanceof ServerPlayer player) {
            if (player.level().getBlockEntity(payload.managerPos()) instanceof RedstoneManagerBlockEntity be) {
                be.addItemToGroup(payload.groupName, payload.item);
                be.setChanged();
                player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
        }
    };
}
