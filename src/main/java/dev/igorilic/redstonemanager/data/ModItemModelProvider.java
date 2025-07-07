package dev.igorilic.redstonemanager.data;

import dev.igorilic.redstonemanager.RedstoneManager;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, RedstoneManager.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        //basicItem(ModItems.RM_LINKER.get());

        ModelFile linked = withExistingParent("rm_linker_linked", "item/generated")
                .texture("layer0", modLoc("item/rm_linker_linked"));

        // Main model with override
        withExistingParent("rm_linker", "item/generated")
                .texture("layer0", modLoc("item/rm_linker"))
                .override()
                .predicate(ResourceLocation.fromNamespaceAndPath(RedstoneManager.MOD_ID, "linked"), 1f)
                .model(linked);
    }
}
