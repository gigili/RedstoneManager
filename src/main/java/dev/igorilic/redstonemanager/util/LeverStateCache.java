package dev.igorilic.redstonemanager.util;

import dev.igorilic.redstonemanager.network.PacketHandler;
import dev.igorilic.redstonemanager.network.PacketLeverStateRequest;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LeverStateCache {
    private static final Map<BlockPos, CachedLever> leverStates = new HashMap<>();

    public static void update(BlockPos pos, boolean found, boolean powered) {
        leverStates.put(pos, new CachedLever(found, powered, System.currentTimeMillis()));
    }

    public static Optional<CachedLever> get(BlockPos pos) {
        if (leverStates.isEmpty()) return Optional.empty();

        CachedLever cached = leverStates.get(pos);
        if (cached == null || (System.currentTimeMillis() - cached.timestamp) > 60_000) {
            return Optional.empty(); // stale
        }
        return Optional.of(cached);
    }

    public static void requestIfNeeded(BlockPos pos) {
        if (leverStates.isEmpty() || get(pos).isEmpty()) {
            PacketHandler.sendToServer(new PacketLeverStateRequest(pos));
        }
    }

    public static void refreshAll() {
        leverStates.clear();
    }

    public record CachedLever(boolean found, boolean powered, long timestamp) {
    }
}
