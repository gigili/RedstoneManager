package dev.igorilic.redstonemanager.item;

import com.mojang.serialization.MapCodec;
import dev.igorilic.redstonemanager.component.ModDataComponents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record ModItemProperties() implements RangeSelectItemModelProperty {
    public static final MapCodec<ModItemProperties> MAP_CODEC = MapCodec.unit(new ModItemProperties());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        if (stack.has(ModDataComponents.COORDINATES) || stack.has(ModDataComponents.COORDINATES_START)) {
            return 1.0F;
        }
        return 0.0F;
    }

    @Override
    public MapCodec<? extends RangeSelectItemModelProperty> type() {
        return MAP_CODEC;
    }
}
