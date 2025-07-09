package dev.igorilic.redstonemanager.block.entity;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, RedstoneManager.MOD_ID);

    public static final Supplier<BlockEntityType<RedstoneManagerBlockEntity>> MANAGER_BE = BLOCK_ENTITIES.register(
            "rm_manager_be",
            () -> BlockEntityType.Builder.of(
                    RedstoneManagerBlockEntity::new,
                    ModBlocks.RM_MANAGER_BLOCK.get()
            ).build(null)
    );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
