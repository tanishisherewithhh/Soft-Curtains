package com.tanishisherewith.item;

import com.tanishisherewith.entity.CurtainStyle;
import com.tanishisherewith.registry.CurtainsComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class CurtainItem extends Item {
    private final DyeColor color;

    public CurtainItem(Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
    }

    public DyeColor getColor() {
        return this.color;
    }

    public static CurtainStyle getStyle(ItemStack stack) {
        CurtainStyle style = stack.get(CurtainsComponents.CURTAIN_STYLE.get());
        return style != null ? style : CurtainStyle.DRAPES;
    }

    @Override
    public @NonNull Component getName(@NonNull ItemStack stack) {
        CurtainStyle style = getStyle(stack);
        if (style == CurtainStyle.DRAPES) {
            return super.getName(stack);
        }
        return Component.translatable(this.getDescriptionId() + "." + style.getSerializedName());
    }
}