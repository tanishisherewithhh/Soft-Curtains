package com.tanishisherewith.client.data;

import com.tanishisherewith.SoftCurtainsMain;
import com.tanishisherewith.block.CurtainRodBlock;
import com.tanishisherewith.block.RodMaterial;
import com.tanishisherewith.registry.CurtainsItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class CurtainModelProvider extends FabricModelProvider {

    public CurtainModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generator) {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof CurtainRodBlock) {
                CurtainRodModelGenerator.registerCurtainRod(generator, block);
            }
        }
    }

    @Override
    public void generateItemModels(ItemModelGenerators generator) {
        for (DyeColor color : DyeColor.values()) {
            Item curtainItem = CurtainsItems.CURTAINS.get(color);
            if (curtainItem != null) {
                TextureMapping textureMapping = new TextureMapping()
                        .put(TextureSlot.LAYER0, new Material(Identifier.withDefaultNamespace("block/" + color.getSerializedName() + "_wool")));
                Identifier model = ModelTemplates.FLAT_ITEM.create(curtainItem, textureMapping, generator.modelOutput);
                generator.itemModelOutput.accept(curtainItem, ItemModelUtils.plainModel(model));
            }
        }

        for (RodMaterial mat : RodMaterial.values()) {
            Item rodItem = CurtainsItems.ROD_ITEMS.get(mat);
            if (rodItem != null) {
                Identifier invBlockModel = Identifier.fromNamespaceAndPath(
                        SoftCurtainsMain.MOD_ID,
                        "block/" + mat.getId() + "_curtain_rod_inventory"
                );
                generator.itemModelOutput.accept(rodItem, ItemModelUtils.plainModel(invBlockModel));
            }
        }
    }
}