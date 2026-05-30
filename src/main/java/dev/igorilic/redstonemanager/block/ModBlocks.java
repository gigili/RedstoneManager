package dev.igorilic.redstonemanager.block;

import dev.igorilic.redstonemanager.RedstoneManager;
import dev.igorilic.redstonemanager.block.custom.RedstoneManagerBlock;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RedstoneManager.MOD_ID);

    public static final DeferredBlock<RedstoneManagerBlock> RM_MANAGER_BLOCK =
            BLOCKS.registerBlock("rm_manager", RedstoneManagerBlock::new,
                    props -> props.strength(4f).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion());

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
