package dev.igorilic.redstonemanager.data;

import dev.igorilic.redstonemanager.block.ModBlocks;
import dev.igorilic.redstonemanager.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
        super(packOutput, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.RM_MANAGER_BLOCK.get())
                .pattern("QCQ")
                .pattern("RPR")
                .pattern("QCQ")
                .define('Q', Items.QUARTZ_BLOCK)
                .define('C', Items.COMPARATOR)
                .define('R', Items.REPEATER)
                .define('P', Items.ENDER_PEARL)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.RM_LINKER.get())
                .pattern("DRD")
                .pattern("CPC")
                .pattern("DRD")
                .define('D', Items.REDSTONE)
                .define('C', Items.COMPARATOR)
                .define('R', Items.REPEATER)
                .define('P', Items.ENDER_PEARL)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(recipeOutput);
    }
}
