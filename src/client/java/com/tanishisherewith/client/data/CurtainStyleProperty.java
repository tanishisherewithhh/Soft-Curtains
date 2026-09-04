package com.tanishisherewith.client.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.tanishisherewith.entity.CurtainStyle;
import com.tanishisherewith.item.CurtainItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record CurtainStyleProperty() implements SelectItemModelProperty<CurtainStyle> {
    public static final CurtainStyleProperty INSTANCE = new CurtainStyleProperty();
    public static final MapCodec<CurtainStyleProperty> CODEC = MapCodec.unit(INSTANCE);
    public static final Type<CurtainStyleProperty, CurtainStyle> TYPE = Type.create(CODEC, CurtainStyle.CODEC);

    @Override
    public CurtainStyle get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
        return CurtainItem.getStyle(stack);
    }

    @Override
    public Codec<CurtainStyle> valueCodec() {
        return CurtainStyle.CODEC;
    }

    @Override
    public Type<CurtainStyleProperty, CurtainStyle> type() {
        return TYPE;
    }
}