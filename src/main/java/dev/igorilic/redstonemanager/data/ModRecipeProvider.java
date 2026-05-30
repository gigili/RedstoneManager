package dev.igorilic.redstonemanager.data;

import dev.igorilic.redstonemanager.block.ModBlocks;
import dev.igorilic.redstonemanager.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.REDSTONE, ModBlocks.RM_MANAGER_BLOCK.get())
                .pattern("QCQ")
                .pattern("RPR")
                .pattern("QCQ")
                .define('Q', Items.QUARTZ_BLOCK)
                .define('C', Items.COMPARATOR)
                .define('R', Items.REPEATER)
                .define('P', Items.ENDER_PEARL)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(output);

        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.REDSTONE, ModItems.RM_LINKER.get())
                .pattern("DRD")
                .pattern("CPC")
                .pattern("DRD")
                .define('D', Items.REDSTONE)
                .define('C', Items.COMPARATOR)
                .define('R', Items.REPEATER)
                .define('P', Items.ENDER_PEARL)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(output);

        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.REDSTONE, ModItems.RM_POUCH.get())
                .pattern("LRL")
                .pattern("SCS")
                .pattern("LRL")
                .define('L', Items.LEATHER)
                .define('C', Items.COMPARATOR)
                .define('R', Items.REDSTONE)
                .define('S', Items.STRING)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(output);
    }
}
