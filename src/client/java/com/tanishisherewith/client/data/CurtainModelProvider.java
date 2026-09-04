package com.tanishisherewith.client.data;

import com.tanishisherewith.SoftCurtainsMain;
import com.tanishisherewith.block.CurtainRodBlock;
import com.tanishisherewith.block.RodMaterial;
import com.tanishisherewith.entity.CurtainStyle;
import com.tanishisherewith.registry.CurtainsBlocks;
import com.tanishisherewith.registry.CurtainsItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.ClientBootstrap;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

public class CurtainModelProvider extends FabricModelProvider {

    //Need to bootstrap the client so that CurtainStyleProperty is registered in the SelectItemModelProperties registry
    static {
        ClientBootstrap.bootstrap();
        CurtainsBlocks.register();
        CurtainsItems.register();

        try {
            SelectItemModelProperties.ID_MAPPER.put(
                    SoftCurtainsMain.id("curtain_style"),
                    CurtainStyleProperty.TYPE
            );
        } catch (IllegalArgumentException | IllegalStateException ignored) {}
    }

    private static final ModelTemplate FLAT_LAYER0 = new ModelTemplate(
            Optional.of(Identifier.withDefaultNamespace("item/generated")),
            Optional.empty(),
            TextureSlot.LAYER0
    );

    public CurtainModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generator) {
        for (Block block : CurtainsBlocks.ROD_BLOCKS.values()) {
            CurtainRodModelGenerator.registerCurtainRod(generator, block);
        }
    }

    @Override
    public void generateItemModels(ItemModelGenerators generator) {
        generator.generateFlatItem(CurtainsItems.TAILORING_SHEARS, ModelTemplates.FLAT_ITEM);

        Identifier drapesModel = createFlatModel(generator, "curtain_drapes");
        Identifier blindsModel = createFlatModel(generator, "curtain_blinds");
        Identifier shuttersModel = createFlatModel(generator, "curtain_shutters");
        Identifier rollerModel = createFlatModel(generator, "curtain_roller");

        for (DyeColor color : DyeColor.values()) {
            Item curtainItem = CurtainsItems.CURTAINS.get(color);
            if (curtainItem != null) {
                Constant tint = new Constant(color.getTextureDiffuseColor());

                ItemModel.Unbaked drapesTinted = ItemModelUtils.tintedModel(drapesModel, tint);
                ItemModel.Unbaked blindsTinted = ItemModelUtils.tintedModel(blindsModel, tint);
                ItemModel.Unbaked shuttersTinted = ItemModelUtils.tintedModel(shuttersModel, tint);
                ItemModel.Unbaked rollerTinted = ItemModelUtils.tintedModel(rollerModel, tint);

                ItemModel.Unbaked selectModel = ItemModelUtils.select(
                        CurtainStyleProperty.INSTANCE,
                        drapesTinted,
                        ItemModelUtils.when(CurtainStyle.DRAPES, drapesTinted),
                        ItemModelUtils.when(CurtainStyle.BLINDS, blindsTinted),
                        ItemModelUtils.when(CurtainStyle.SHUTTERS, shuttersTinted),
                        ItemModelUtils.when(CurtainStyle.ROLLER, rollerTinted)
                );

                generator.itemModelOutput.accept(curtainItem, selectModel);
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

    private Identifier createFlatModel(ItemModelGenerators generator, String name) {
        TextureMapping mapping = new TextureMapping()
                .put(TextureSlot.LAYER0, new Material(Identifier.fromNamespaceAndPath(SoftCurtainsMain.MOD_ID, "item/" + name)));
        return FLAT_LAYER0.create(
                Identifier.fromNamespaceAndPath(SoftCurtainsMain.MOD_ID, "item/" + name),
                mapping,
                generator.modelOutput
        );
    }
}