package com.tanishisherewith.item;

import com.tanishisherewith.entity.CurtainStyle;
import com.tanishisherewith.registry.CurtainsComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class CurtainItem extends Item {
    private final DyeColor color;
    private final @Nullable String customTexture;

    public CurtainItem(Properties properties, DyeColor color) {
        this(properties, color, null);
    }

    public CurtainItem(Properties properties, DyeColor color, @Nullable String customTexture) {
        super(properties);
        this.color = color;
        this.customTexture = customTexture;
    }

    public static CurtainStyle getStyle(ItemStack stack) {
        CurtainStyle style = stack.get(CurtainsComponents.CURTAIN_STYLE.get());
        return style != null ? style : CurtainStyle.DRAPES;
    }

    @Override
    public @NonNull Component getName(@NonNull ItemStack stack) {
        CurtainStyle style = getStyle(stack);
        return Component.translatable(this.getDescriptionId() + "." + style.getSerializedName());
    }

    public DyeColor getColor() {
        return this.color;
    }

    public @Nullable String getCustomTexture() {
        return this.customTexture;
    }
}