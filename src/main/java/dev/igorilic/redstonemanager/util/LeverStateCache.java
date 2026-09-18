package dev.igorilic.redstonemanager.util;

import dev.igorilic.redstonemanager.network.PacketHandler;
import dev.igorilic.redstonemanager.network.PacketLeverStateRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LeverStateCache {
    private static final Map<LeverKey, CachedLever> leverStates = new HashMap<>();

    public static void update(BlockPos pos, Optional<Identifier> dimension, boolean found, boolean powered) {
        leverStates.put(new LeverKey(pos, dimension), new CachedLever(found, powered, System.currentTimeMillis()));
    }

    public static Optional<CachedLever> get(BlockPos pos, Optional<Identifier> dimension) {
        if (leverStates.isEmpty()) return Optional.empty();

        CachedLever cached = leverStates.get(new LeverKey(pos, dimension));
        if (cached == null || (System.currentTimeMillis() - cached.timestamp) > 60_000) {
            return Optional.empty(); // stale
        }
        return Optional.of(cached);
    }

    public static void requestIfNeeded(BlockPos pos, Optional<Identifier> dim) {
        if (leverStates.isEmpty() || get(pos, dim).isEmpty()) {
            PacketHandler.sendToServer(new PacketLeverStateRequest(pos, dim));
        }
    }

    public static void refreshAll() {
        leverStates.clear();
    }

    public record CachedLever(boolean found, boolean powered, long timestamp) {
    }

    private record LeverKey(BlockPos pos, Optional<Identifier> dimension) {
    }
}
