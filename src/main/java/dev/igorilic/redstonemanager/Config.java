package dev.igorilic.redstonemanager;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<Boolean> DELETE_EMPTY_GROUPS;

    static {
        BUILDER.comment("Redstone Manager Config");

        // General
        BUILDER.push("General");

        DELETE_EMPTY_GROUPS = BUILDER
                .comment("Whether empty groups should be deleted from the Redstone Manager")
                .define("delete_empty_groups", false);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
