package com.tanishisherewith.registry;

import com.tanishisherewith.SoftCurtainsMain;
import com.tanishisherewith.block.CurtainRodBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public class CurtainBlocks {
    public static final Block CURTAIN_ROD = register("curtain_rod", CurtainRodBlock::new, BlockBehaviour.Properties.of()
            .noOcclusion()
            .strength(1.0F)
            .sound(SoundType.BAMBOO_WOOD));

    public static final Item CURTAIN_ROD_ITEM = registerBlockItem("curtain_rod", CURTAIN_ROD);

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        Identifier location = Identifier.fromNamespaceAndPath(SoftCurtainsMain.MOD_ID, name);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, location);

        Block block = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    private static Item registerBlockItem(String name, Block block) {
        Identifier location = Identifier.fromNamespaceAndPath(SoftCurtainsMain.MOD_ID, name);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, location);

        Item.Properties properties = new Item.Properties().setId(itemKey);

        Item item = new BlockItem(block, properties);
        return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
    }

    public static void register() {
    }
}
