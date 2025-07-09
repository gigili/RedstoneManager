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
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketAddLinkerToChannel(BlockPos managerPos, ItemStack linker, int channelIndex) implements CustomPacketPayload {
    public static final Type<PacketAddLinkerToChannel> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "add_linker_to_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketAddLinkerToChannel> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PacketAddLinkerToChannel::managerPos,
                    ItemStack.OPTIONAL_STREAM_CODEC,
                    PacketAddLinkerToChannel::linker,
                    ByteBufCodecs.INT,
                    PacketAddLinkerToChannel::channelIndex,
                    PacketAddLinkerToChannel::new
            );

    @Override
    public Type<PacketAddLinkerToChannel> type() {
        return TYPE;
    }

    public static void handle(PacketAddLinkerToChannel payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.level().getBlockEntity(payload.managerPos()) instanceof RedstoneManagerBlockEntity be) {
                    //be.addLinkerToCurrentChannel(payload.linker());
                    be.setChanged();
                    player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
                }
            }
        });
    }
}
