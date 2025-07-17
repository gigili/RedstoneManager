package dev.igorilic.redstonemanager.util;

import dev.igorilic.redstonemanager.component.ModDataComponents;
import dev.igorilic.redstonemanager.item.custom.RedstoneLinkerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

import java.util.function.Consumer;

public class ChunkHandler {

    public static void tempLoadChunk(ServerLevel level, ItemStack stack, Consumer<ServerLevel> callback) {
        if (!(stack.getItem() instanceof RedstoneLinkerItem)) return;

        BlockPos cords = stack.get(ModDataComponents.COORDINATES);
        if (cords == null) return;

        ChunkPos chunkPos = new ChunkPos(cords);

        level.getChunkSource().addRegionTicket(
                TicketType.FORCED,
                chunkPos,
                2,
                chunkPos
        );

        // Delay execution by a few ticks to let chunk load
        level.getServer().execute(() -> {
            // Wait a few ticks before checking
            handler(level, cords, callback, chunkPos);
        });
    }

    public static void tempLoadChunk(ServerLevel level, BlockPos pos, Consumer<ServerLevel> callback) {
        ChunkPos chunkPos = new ChunkPos(pos);

        level.getChunkSource().addRegionTicket(
                TicketType.FORCED,
                chunkPos,
                2,
                chunkPos
        );

        handler(level, pos, callback, chunkPos);
    }

    private static void handler(ServerLevel level, BlockPos pos, Consumer<ServerLevel> callback, ChunkPos chunkPos) {
        level.getServer().tell(new TickTask(level.getServer().getTickCount() + 2, () -> {
            if (level.isLoaded(pos) && level.getChunkSource().hasChunk(chunkPos.x, chunkPos.z)) {
                callback.accept(level);
            }

            level.getChunkSource().removeRegionTicket(
                    TicketType.FORCED,
                    chunkPos,
                    2,
                    chunkPos
            );
        }));
    }
}
