package dev.igorilic.redstonemanager;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<Boolean> DELETE_EMPTY_GROUPS;
    public static final ModConfigSpec.ConfigValue<Integer> BULK_SEARCH_RADIUS;

    static {
        BUILDER.comment("Redstone Manager Config");

        // General
        BUILDER.push("General");

        DELETE_EMPTY_GROUPS = BUILDER
                .comment("Whether empty groups should be deleted from the Redstone Manager")
                .define("delete_empty_groups", false);

        BULK_SEARCH_RADIUS = BUILDER
                .comment("The radius to search for similar blocks when bulk linking. NOTE: each block requires a linker in inventory and larger range could cause lag when linking")
                .defineInRange("bulk_search_radius", 32, 1, 256);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
