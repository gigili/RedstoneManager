package dev.igorilic.redstonemanager.network;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.util.Interact;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record MenuInteractPacketC2S(ItemStack itemStack, CompoundTag extraData, int clickType,
                                    int button) implements CustomPacketPayload {

    public MenuInteractPacketC2S(ItemStack itemStack, int clickType, int button) {
        this(itemStack, new CompoundTag(), clickType, button);
    }

    public MenuInteractPacketC2S(CompoundTag extraData, int clickType, int button) {
        this(ItemStack.EMPTY, extraData, clickType, button);
    }

    public MenuInteractPacketC2S(int clickType, int button) {
        this(new CompoundTag(), clickType, button);
    }

    public MenuInteractPacketC2S(CompoundTag extraData, int button) {
        this(extraData, 0, button);
    }

    public MenuInteractPacketC2S(int button) {
        this(0, button);
    }

    public static final CustomPacketPayload.Type<MenuInteractPacketC2S> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "interact_menu"));


    public static final IPayloadHandler<MenuInteractPacketC2S> HANDLER = (pkt, ctx) -> {
        final var player = ctx.player();

        if (player.containerMenu instanceof Interact interact)
            interact.clicked(pkt.itemStack(), pkt.extraData(), ClickType.values()[pkt.clickType()], pkt.button());
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, MenuInteractPacketC2S> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, MenuInteractPacketC2S::itemStack,
            ByteBufCodecs.COMPOUND_TAG, MenuInteractPacketC2S::extraData,
            ByteBufCodecs.INT, MenuInteractPacketC2S::clickType,
            ByteBufCodecs.INT, MenuInteractPacketC2S::button,
            MenuInteractPacketC2S::new
    );


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
