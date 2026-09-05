package com.tanishisherewith.block;

import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public record RodMaterial(
        String id,
        Identifier texture,
        Supplier<Item> ingredientSupplier,
        MapColor mapColor,
        SoundType soundType,
        boolean isWood
) implements StringRepresentable {

    private static final Map<String, RodMaterial> REGISTRY = new LinkedHashMap<>();

    public static RodMaterial registerWood(String id, Supplier<Item> planks, MapColor color, SoundType sound) {
        Identifier textureId = Identifier.withDefaultNamespace("block/" + id + "_planks");
        return register(id, textureId, planks, color, sound, true);
    }

    public static RodMaterial registerMetal(String id, Supplier<Item> ingot, MapColor color, SoundType sound) {
        Identifier textureId = Identifier.withDefaultNamespace("block/" + id + "_block");
        return register(id, textureId, ingot, color, sound, false);
    }

    public static RodMaterial register(String id, Identifier texture, Supplier<Item> ingredient, MapColor mapColor, SoundType soundType, boolean isWood) {
        RodMaterial material = new RodMaterial(id, texture, ingredient, mapColor, soundType, isWood);
        REGISTRY.put(id, material);
        return material;
    }

    public static final RodMaterial OAK = registerWood("oak", () -> Items.OAK_PLANKS, MapColor.WOOD, SoundType.WOOD);
    public static final RodMaterial SPRUCE = registerWood("spruce", () -> Items.SPRUCE_PLANKS, MapColor.WOOD, SoundType.WOOD);
    public static final RodMaterial BIRCH = registerWood("birch", () -> Items.BIRCH_PLANKS, MapColor.SAND, SoundType.WOOD);
    public static final RodMaterial JUNGLE = registerWood("jungle", () -> Items.JUNGLE_PLANKS, MapColor.DIRT, SoundType.WOOD);
    public static final RodMaterial ACACIA = registerWood("acacia", () -> Items.ACACIA_PLANKS, MapColor.COLOR_ORANGE, SoundType.WOOD);
    public static final RodMaterial DARK_OAK = registerWood("dark_oak", () -> Items.DARK_OAK_PLANKS, MapColor.COLOR_BROWN, SoundType.WOOD);
    public static final RodMaterial MANGROVE = registerWood("mangrove", () -> Items.MANGROVE_PLANKS, MapColor.COLOR_RED, SoundType.WOOD);
    public static final RodMaterial CHERRY = registerWood("cherry", () -> Items.CHERRY_PLANKS, MapColor.TERRACOTTA_WHITE, SoundType.CHERRY_WOOD);
    public static final RodMaterial BAMBOO = registerWood("bamboo", () -> Items.BAMBOO_PLANKS, MapColor.COLOR_YELLOW, SoundType.BAMBOO_WOOD);
    public static final RodMaterial CRIMSON = registerWood("crimson", () -> Items.CRIMSON_PLANKS, MapColor.CRIMSON_STEM, SoundType.NETHER_WOOD);
    public static final RodMaterial WARPED = registerWood("warped", () -> Items.WARPED_PLANKS, MapColor.WARPED_STEM, SoundType.NETHER_WOOD);
    public static final RodMaterial PALE_OAK = registerWood("pale_oak", () -> Items.PALE_OAK_PLANKS, MapColor.COLOR_LIGHT_GRAY, SoundType.WOOD);

    public static final RodMaterial IRON = registerMetal("iron", () -> Items.IRON_INGOT, MapColor.METAL, SoundType.METAL);
    public static final RodMaterial GOLD = registerMetal("gold", () -> Items.GOLD_INGOT, MapColor.GOLD, SoundType.METAL);
    public static final RodMaterial COPPER = registerMetal("copper", () -> Items.COPPER_INGOT, MapColor.COLOR_ORANGE, SoundType.COPPER);

    public Item getIngredientItem() {
        return this.ingredientSupplier.get();
    }

    public MapColor getMapColor() {
        return this.mapColor;
    }

    public SoundType getSoundType() {
        return this.soundType;
    }

    public String getId() {
        return this.id;
    }

    public Identifier getTexture() {
        return this.texture;
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.id;
    }

    public static Collection<RodMaterial> values() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static RodMaterial byId(String id) {
        return REGISTRY.get(id);
    }
}