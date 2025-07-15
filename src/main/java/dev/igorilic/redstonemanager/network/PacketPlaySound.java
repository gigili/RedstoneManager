package dev.igorilic.redstonemanager.network;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.block.entity.RedstoneManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

public record PacketPlaySound(BlockPos managerPos, Holder<SoundEvent> soundEvent, float volume,
                              float pitch) implements CustomPacketPayload {
    public static final Type<PacketPlaySound> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "play_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketPlaySound> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PacketPlaySound::managerPos,
                    SoundEvent.STREAM_CODEC,
                    PacketPlaySound::soundEvent,
                    ByteBufCodecs.FLOAT,
                    PacketPlaySound::volume,
                    ByteBufCodecs.FLOAT,
                    PacketPlaySound::pitch,
                    PacketPlaySound::new
            );

    @Override
    public @NotNull Type<PacketPlaySound> type() {
        return TYPE;
    }

    public static final IPayloadHandler<PacketPlaySound> HANDLER = (payload, context) -> {
        if (context.player() instanceof ServerPlayer player) {
            if (player.level().getBlockEntity(payload.managerPos()) instanceof RedstoneManagerBlockEntity be) {
                be.playSound(payload.soundEvent.value(), payload.volume, payload.pitch);
                be.setChanged();
                player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
        }
    };
}
