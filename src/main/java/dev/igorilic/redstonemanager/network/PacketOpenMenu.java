package dev.igorilic.redstonemanager.network;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.block.entity.RedstoneManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

public record PacketOpenMenu(BlockPos pos) implements CustomPacketPayload {
    public static final Type<PacketOpenMenu> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "open_menu"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketOpenMenu> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PacketOpenMenu::pos,
                    PacketOpenMenu::new
            );

    @Override
    public @NotNull CustomPacketPayload.Type<PacketOpenMenu> type() {
        return TYPE;
    }

    public static final IPayloadHandler<PacketOpenMenu> HANDLER = (payload, context) -> {
        if (context.player() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            BlockPos pos = payload.pos;

            if (!level.isLoaded(pos)) return;

            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof RedstoneManagerBlockEntity redstoneManager)) return;

            player.openMenu(new SimpleMenuProvider(redstoneManager, Component.translatable("gui.redstonemanager.manager")), payload.pos);
        }
    };
}
