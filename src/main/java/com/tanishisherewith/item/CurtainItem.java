package com.tanishisherewith.item;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

public class CurtainItem extends Item {
    private final DyeColor color;

    public CurtainItem(Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
    }

    public DyeColor getColor() {
        return this.color;
    }
}