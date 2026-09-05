package com.tanishisherewith.registry;

import com.tanishisherewith.SoftCurtainsMain;
import com.tanishisherewith.block.CurtainRodBlock;
import com.tanishisherewith.block.RodMaterial;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class CurtainsBlocks {
    public static final Map<RodMaterial, Block> ROD_BLOCKS = new LinkedHashMap<>();

    static {
        for (RodMaterial material : RodMaterial.values()) {
            ROD_BLOCKS.put(material, register(material.getSerializedName() + "_curtain_rod",
                    key -> new CurtainRodBlock(BlockBehaviour.Properties.of()
                            .setId(key)
                            .mapColor(material.getMapColor())
                            .strength(1.0f)
                            .sound(material.getSoundType())
                            .noOcclusion())));
        }
    }

    private static <T extends Block> T register(String name, Function<ResourceKey<Block>, T> blockFactory) {
        Identifier id = Identifier.fromNamespaceAndPath(SoftCurtainsMain.MOD_ID, name);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        T block = blockFactory.apply(key);
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    public static void register() {}
}