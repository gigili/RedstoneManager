package dev.igorilic.redstonemanager.network;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.block.entity.RedstoneManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record PacketToggleLever(BlockPos managerPos, ItemStack linkerItem) implements CustomPacketPayload {
    public static final Type<PacketToggleLever> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "toggle_lever"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketToggleLever> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PacketToggleLever::managerPos,
                    ItemStack.OPTIONAL_STREAM_CODEC,
                    PacketToggleLever::linkerItem,
                    PacketToggleLever::new
            );

    @Override
    public Type<PacketToggleLever> type() {
        return TYPE;
    }

    public static final IPayloadHandler<PacketToggleLever> HANDLER = (payload, context) -> {
        if (context.player() instanceof ServerPlayer player) {
            if (player.level().getBlockEntity(payload.managerPos()) instanceof RedstoneManagerBlockEntity be) {
                ItemStack linkerToToggle = payload.linkerItem();
                if (!linkerToToggle.isEmpty()) {
                    be.toggleLinkedLever(linkerToToggle);
                    // Sync the block entity to update the client
                    be.setChanged();
                }
                player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
        }
    };

    public static void handle(PacketToggleLever payload, IPayloadContext context) {

    }
}
