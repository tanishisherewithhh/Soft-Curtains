package com.tanishisherewith.client.data.generator;

import com.tanishisherewith.SoftCurtainsMain;
import com.tanishisherewith.block.CurtainRodBlock;
import com.tanishisherewith.block.CurtainRodType;
import com.tanishisherewith.block.RodMaterial;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

public class CurtainRodModelGenerator {

    private static final ModelTemplate ROD_BASE = rodTemplate("curtain_rod_base");
    private static final ModelTemplate STOPPER_LEFT = rodTemplate("curtain_rod_stopper_left");
    private static final ModelTemplate STOPPER_RIGHT = rodTemplate("curtain_rod_stopper_right");
    private static final ModelTemplate MIDDLE_SUPPORT = rodTemplate("curtain_rod_middle_support");
    private static final ModelTemplate ROD_INVENTORY = rodTemplate("curtain_rod_inventory");

    private static ModelTemplate rodTemplate(String parent) {
        return new ModelTemplate(
                Optional.of(Identifier.fromNamespaceAndPath(SoftCurtainsMain.MOD_ID, "block/" + parent)),
                Optional.empty(),
                TextureSlot.TEXTURE
        );
    }

    public static void registerCurtainRod(BlockModelGenerators generator, Block rodBlock) {
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(rodBlock);
        String name = blockId.getPath().replace("_curtain_rod", "");

        Identifier textureId = resolveTexture(name);
        TextureMapping textures = new TextureMapping()
                .put(TextureSlot.TEXTURE, new Material(textureId));

        Identifier base = ROD_BASE.createWithSuffix(rodBlock, "_base", textures, generator.modelOutput);
        Identifier left = STOPPER_LEFT.createWithSuffix(rodBlock, "_stopper_left", textures, generator.modelOutput);
        Identifier right = STOPPER_RIGHT.createWithSuffix(rodBlock, "_stopper_right", textures, generator.modelOutput);
        Identifier middle = MIDDLE_SUPPORT.createWithSuffix(rodBlock, "_middle_support", textures, generator.modelOutput);


        ROD_INVENTORY.createWithSuffix(rodBlock, "_inventory", textures, generator.modelOutput);

        MultiPartGenerator multiPart = MultiPartGenerator.multiPart(rodBlock);

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            multiPart.with(
                    new ConditionBuilder().term(CurtainRodBlock.FACING, dir),
                    rotate(base, dir)
            );
        }

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            multiPart.with(
                    new ConditionBuilder()
                            .term(CurtainRodBlock.FACING, dir)
                            .term(CurtainRodBlock.ROD_TYPE, CurtainRodType.END_LEFT),
                    rotate(left, dir)
            );
            multiPart.with(
                    new ConditionBuilder()
                            .term(CurtainRodBlock.FACING, dir)
                            .term(CurtainRodBlock.ROD_TYPE, CurtainRodType.STRAIGHT),
                    rotate(left, dir)
            );
        }

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            multiPart.with(
                    new ConditionBuilder()
                            .term(CurtainRodBlock.FACING, dir)
                            .term(CurtainRodBlock.ROD_TYPE, CurtainRodType.END_RIGHT),
                    rotate(right, dir)
            );
            multiPart.with(
                    new ConditionBuilder()
                            .term(CurtainRodBlock.FACING, dir)
                            .term(CurtainRodBlock.ROD_TYPE, CurtainRodType.STRAIGHT),
                    rotate(right, dir)
            );
        }

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            multiPart.with(
                    new ConditionBuilder()
                            .term(CurtainRodBlock.FACING, dir)
                            .term(CurtainRodBlock.ROD_TYPE, CurtainRodType.MIDDLE_STOPPER),
                    rotate(middle, dir)
            );
        }

        generator.blockStateOutput.accept(multiPart);
    }

    private static Identifier resolveTexture(String materialName) {
        for (RodMaterial mat : RodMaterial.values()) {
            if (mat.getId().equals(materialName)) {
                return Identifier.parse(mat.getTexture());
            }
        }
        return Identifier.withDefaultNamespace("block/" + materialName + "_planks");
    }

    private static MultiVariant rotate(Identifier modelId, Direction direction) {
        MultiVariant variant = BlockModelGenerators.plainVariant(modelId);
        return switch (direction) {
            case SOUTH -> variant.with(BlockModelGenerators.Y_ROT_180);
            case WEST  -> variant.with(BlockModelGenerators.Y_ROT_270);
            case EAST  -> variant.with(BlockModelGenerators.Y_ROT_90);
            default    -> variant;
        };
    }
}