package com.tanishisherewith.registry;

import com.tanishisherewith.SoftCurtainsMain;
import com.tanishisherewith.block.RodMaterial;
import com.tanishisherewith.item.CurtainItem;
import com.tanishisherewith.item.TailoringShearsItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class CurtainsItems {
    public static final Map<RodMaterial, Item> ROD_ITEMS = new LinkedHashMap<>();
    public static final Map<DyeColor, Item> CURTAINS = new EnumMap<>(DyeColor.class);
    public static final Item TAILORING_SHEARS;

    static {
        for (RodMaterial material : RodMaterial.values()) {
            ROD_ITEMS.put(material, register(material.getSerializedName() + "_curtain_rod",
                    key -> new BlockItem(CurtainsBlocks.ROD_BLOCKS.get(material), new Item.Properties().setId(key))));
        }

        for (DyeColor color : DyeColor.values()) {
            Item curtainItem = register(color.getSerializedName() + "_curtain",
                    key -> new CurtainItem(new Item.Properties().setId(key).stacksTo(16), color));
            CURTAINS.put(color, curtainItem);
        }
        TAILORING_SHEARS = register("tailoring_shears",
                key -> new TailoringShearsItem(new Item.Properties().setId(key).durability(238)));
    }

    private static <T extends Item> T register(String name, Function<ResourceKey<Item>, T> itemFactory) {
        Identifier id = Identifier.fromNamespaceAndPath(SoftCurtainsMain.MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        T item = itemFactory.apply(key);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void register() {}
}