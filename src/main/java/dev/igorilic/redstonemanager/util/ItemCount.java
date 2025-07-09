package dev.igorilic.redstonemanager.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.function.Consumer;

public record ItemCount(int amount) implements TooltipProvider {

    public static final StreamCodec<ByteBuf, ItemCount> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ItemCount::amount,
            ItemCount::new
    );

    public static final Codec<ItemCount> CODEC = RecordCodecBuilder.create(i ->
            i.group(
                    Codec.INT.fieldOf("amount").forGetter(ItemCount::amount)
            ).apply(i, ItemCount::new)
    );

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        tooltipAdder.accept(
                Component.literal("Count: " + amount)
        );
    }
}
