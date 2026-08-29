package com.tanishisherewith.block;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.NonNull;

public enum RodMaterial implements StringRepresentable {
    OAK("oak", "minecraft:block/oak_planks", MapColor.WOOD, SoundType.WOOD),
    SPRUCE("spruce", "minecraft:block/spruce_planks", MapColor.WOOD, SoundType.WOOD),
    BIRCH("birch", "minecraft:block/birch_planks", MapColor.SAND, SoundType.WOOD),
    JUNGLE("jungle", "minecraft:block/jungle_planks", MapColor.DIRT, SoundType.WOOD),
    ACACIA("acacia", "minecraft:block/acacia_planks", MapColor.COLOR_ORANGE, SoundType.WOOD),
    DARK_OAK("dark_oak", "minecraft:block/dark_oak_planks", MapColor.COLOR_BROWN, SoundType.WOOD),
    MANGROVE("mangrove", "minecraft:block/mangrove_planks", MapColor.COLOR_RED, SoundType.WOOD),
    CHERRY("cherry", "minecraft:block/cherry_planks", MapColor.TERRACOTTA_WHITE, SoundType.CHERRY_WOOD),
    BAMBOO("bamboo", "minecraft:block/bamboo_planks", MapColor.COLOR_YELLOW, SoundType.BAMBOO_WOOD),
    CRIMSON("crimson", "minecraft:block/crimson_planks", MapColor.CRIMSON_STEM, SoundType.NETHER_WOOD),
    WARPED("warped", "minecraft:block/warped_planks", MapColor.WARPED_STEM, SoundType.NETHER_WOOD),
    IRON("iron", "minecraft:block/iron_block", MapColor.METAL, SoundType.METAL),
    GOLD("gold", "minecraft:block/gold_block", MapColor.GOLD, SoundType.METAL),
    COPPER("copper", "minecraft:block/copper_block", MapColor.COLOR_ORANGE, SoundType.COPPER);

    private final String id;
    private final String texture;
    private final MapColor mapColor;
    private final SoundType soundType;

    RodMaterial(String id, String texture, MapColor mapColor, SoundType soundType) {
        this.id = id;
        this.texture = texture;
        this.mapColor = mapColor;
        this.soundType = soundType;
    }

    public Item getIngredientItem() {
        return switch (this) {
            case OAK -> Items.OAK_PLANKS;
            case SPRUCE -> Items.SPRUCE_PLANKS;
            case BIRCH -> Items.BIRCH_PLANKS;
            case JUNGLE -> Items.JUNGLE_PLANKS;
            case ACACIA -> Items.ACACIA_PLANKS;
            case DARK_OAK -> Items.DARK_OAK_PLANKS;
            case MANGROVE -> Items.MANGROVE_PLANKS;
            case CHERRY -> Items.CHERRY_PLANKS;
            case BAMBOO -> Items.BAMBOO_PLANKS;
            case CRIMSON -> Items.CRIMSON_PLANKS;
            case WARPED -> Items.WARPED_PLANKS;
            case IRON -> Items.IRON_INGOT;
            case GOLD -> Items.GOLD_INGOT;
            case COPPER -> Items.COPPER_INGOT;
        };
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

    public String getTexture() {
        return this.texture;
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.id;
    }
}