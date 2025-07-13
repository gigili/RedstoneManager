package dev.igorilic.redstonemanager.network;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.block.entity.RedstoneManagerBlockEntity;
import dev.igorilic.redstonemanager.util.IUpdatable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record PacketMoveLinker(BlockPos managerPos, ItemStack linkerItem) implements CustomPacketPayload {
    public static final Type<PacketMoveLinker> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "linker_moved"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketMoveLinker> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PacketMoveLinker::managerPos,
                    ItemStack.OPTIONAL_STREAM_CODEC,
                    PacketMoveLinker::linkerItem,
                    PacketMoveLinker::new
            );

    @Override
    public Type<PacketMoveLinker> type() {
        return TYPE;
    }

    public static final IPayloadHandler<PacketMoveLinker> HANDLER = (payload, context) -> {
        if (context.player() instanceof ServerPlayer player) {
            if (player.level().getBlockEntity(payload.managerPos()) instanceof RedstoneManagerBlockEntity be) {
                ItemStack linkerToToggle = payload.linkerItem();
                if (!linkerToToggle.isEmpty()) {
                    // Sync the block entity to update the client
                    be.setChanged();

                }
                player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);

                Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().screen instanceof IUpdatable ui) {
                        ui.update();
                    }
                });
            }
        }
    };
}
