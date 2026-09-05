package com.tanishisherewith.client.data.recipe;

import com.tanishisherewith.block.RodMaterial;
import com.tanishisherewith.registry.CurtainsBlocks;
import com.tanishisherewith.registry.CurtainsItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class CurtainRecipeProvider extends FabricRecipeProvider {
    public CurtainRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected @NonNull RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new RecipeProvider(registries, output) {
            @Override
            public void buildRecipes() {
                HolderGetter<Item> itemLookup = registries.lookupOrThrow(Registries.ITEM);
                Item whiteCurtain = CurtainsItems.CURTAINS.get(DyeColor.WHITE);

                for (RodMaterial material : RodMaterial.values()) {
                    Block rodBlock = CurtainsBlocks.ROD_BLOCKS.get(material);
                    Item ingredientItem = material.getIngredientItem();

                    if (material.isWood()) {
                        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.DECORATIONS, rodBlock, 3)
                                .pattern("#S#")
                                .define('#', ingredientItem)
                                .define('S', Items.STICK)
                                .unlockedBy("has_ingredient", has(ingredientItem))
                                .unlockedBy("has_stick", has(Items.STICK))
                                .save(output);
                    } else {
                        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.DECORATIONS, rodBlock, 3)
                                .pattern("###")
                                .define('#', ingredientItem)
                                .unlockedBy("has_ingredient", has(ingredientItem))
                                .save(output);
                    }
                }

                for (DyeColor color : DyeColor.values()) {
                    Item curtainItem = CurtainsItems.CURTAINS.get(color);
                    Item woolItem = Items.WOOL.pick(color);

                    ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.DECORATIONS, curtainItem, 2)
                            .pattern("#")
                            .pattern("#")
                            .define('#', woolItem)
                            .unlockedBy("has_wool", has(woolItem))
                            .save(output);

                    Item dyeItem = Items.DYE.pick(color);
                    ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.DECORATIONS, curtainItem)
                            .requires(whiteCurtain)
                            .requires(dyeItem)
                            .unlockedBy("has_curtain", has(whiteCurtain))
                            .save(output, curtainItem + "_from_dye");
                }
            }
        };
    }

    @Override
    public String getName() {
        return "Curtain Recipe Provider";
    }
}