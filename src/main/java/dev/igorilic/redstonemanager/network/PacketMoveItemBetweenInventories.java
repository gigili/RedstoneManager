package dev.igorilic.redstonemanager.network;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.block.entity.RedstoneManagerBlockEntity;
import dev.igorilic.redstonemanager.util.ClickAction;
import dev.igorilic.redstonemanager.util.Interact;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

public record PacketMoveItemBetweenInventories(ItemStack itemStack, CompoundTag extraData, int clickType, int button,
                                               BlockPos managerPos, String groupName) implements CustomPacketPayload {

    public PacketMoveItemBetweenInventories(ItemStack itemStack, int clickType, int button, BlockPos managerPos, String groupName) {
        this(itemStack, new CompoundTag(), clickType, button, managerPos, groupName);
    }

    public static final CustomPacketPayload.Type<PacketMoveItemBetweenInventories> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "interact_menu"));


    public static final IPayloadHandler<PacketMoveItemBetweenInventories> HANDLER = (pkt, ctx) -> {
        final var player = ctx.player();

        if (player.containerMenu instanceof Interact interact) {
            interact.clicked(pkt.itemStack(), pkt.extraData(), ClickAction.values()[pkt.clickType()], pkt.button(), pkt.groupName);
        }

        if (ctx.player() instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.level().getBlockEntity(pkt.managerPos()) instanceof RedstoneManagerBlockEntity be) {
                be.setChanged();
                serverPlayer.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketMoveItemBetweenInventories> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, PacketMoveItemBetweenInventories::itemStack,
            ByteBufCodecs.COMPOUND_TAG, PacketMoveItemBetweenInventories::extraData,
            ByteBufCodecs.INT, PacketMoveItemBetweenInventories::clickType,
            ByteBufCodecs.INT, PacketMoveItemBetweenInventories::button,
            BlockPos.STREAM_CODEC, PacketMoveItemBetweenInventories::managerPos,
            ByteBufCodecs.STRING_UTF8, PacketMoveItemBetweenInventories::groupName,
            PacketMoveItemBetweenInventories::new
    );


    @Override
    public CustomPacketPayload.@NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
